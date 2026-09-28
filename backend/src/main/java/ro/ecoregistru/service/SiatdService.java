package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.response.SiatdReceptionRow;
import ro.ecoregistru.controller.response.SiatdSummary;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.SiatdModule;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.SiatdDeadlines.SiatdDeadline;
import ro.ecoregistru.service.SiatdDeadlines.SiatdDeadline.State;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.SIATD_CODE_SINGLE;
import static ro.ecoregistru.exception.ErrorMessageEnum.SIATD_CODE_TOO_LONG;
import static ro.ecoregistru.exception.ErrorMessageEnum.SIATD_CONFIRM_SELECTION;
import static ro.ecoregistru.exception.ErrorMessageEnum.SIATD_NO_DEADLINE;

/**
 * F6a — recepțiile cu termen de confirmare în SIATD: lista din Cântar → SIATD, cifrele benzii și confirmarea. Termenul nu
 * se stochează ({@link SiatdDeadlines}); se stochează doar confirmarea, cu omul, data și codul opțional al tranzacției.
 *
 * <p>Confirmarea nu e dată de registru: merge și după finalizare, și după lacătul Anexei 3. O fac doar cei care aprobă,
 * mai multe deodată (un colector de la PF are zeci de recepții pe zi), dar codul SIATD doar pe una: e unic pe tranzacție.
 * {@code WeighingOperation} e auditată, deci confirmarea și anularea ei intră singure în jurnal.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SiatdService {

    static final int MAX_PER_REQUEST = 100;
    static final Duration CONFIRMED_WINDOW = Duration.ofDays(30);
    private static final Set<Role> APPROVERS = EnumSet.of(Role.PLATFORM_ADMIN, Role.ADMIN, Role.CONSULTANT);

    WeighingOperationRepository operationRepository;
    WasteMovementRepository movementRepository;
    CompanyRepository companyRepository;
    AppUserRepository userRepository;
    WeighingOperationService operationService;
    DepotAccess depotAccess;

    /** Rândurile unui subtab, pe depozitele pe care le vede omul: de confirmat și ratate după termen, confirmate după dată. */
    @Transactional(readOnly = true)
    public List<SiatdReceptionRow> receptions(State state) {
        UUID tenantId = TenantContext.require();
        Company company = company(tenantId);
        LocalDate today = DeadlineService.today();
        if (state == State.CONFIRMED) {
            List<WeighingOperation> confirmed = depotAccess.filter(
                    operationRepository.findSiatdConfirmedSince(tenantId, Instant.now().minus(CONFIRMED_WINDOW)),
                    o -> o.getWorkPoint().getId());
            return rows(confirmed, company);
        }
        return rows(depotAccess.filter(candidates(company), o -> o.getWorkPoint().getId()),
                company).stream()
                .filter(r -> stateOf(r, today) == state)
                .sorted(java.util.Comparator.comparing(SiatdReceptionRow::due).thenComparing(SiatdReceptionRow::number))
                .toList();
    }

    @Transactional(readOnly = true)
    public SiatdSummary summary() {
        UUID tenantId = TenantContext.require();
        Company company = company(tenantId);
        if (company.siatdEnrolment().isEmpty()) {
            return new SiatdSummary(false, 0, 0, 0, 0);
        }
        LocalDate today = DeadlineService.today();
        int pending = 0;
        int dueToday = 0;
        int dueTomorrow = 0;
        int missed = 0;
        for (SiatdReceptionRow r : rows(depotAccess.filter(candidates(company),
                o -> o.getWorkPoint().getId()), company)) {
            if (stateOf(r, today) == State.MISSED) {
                missed++;
                continue;
            }
            pending++;
            if (r.due().equals(today)) {
                dueToday++;
            } else if (r.due().equals(today.plusDays(1))) {
                dueTomorrow++;
            }
        }
        return new SiatdSummary(true, pending, dueToday, dueTomorrow, missed);
    }

    /** Pentru mementoul de pe mail: toată firma, fără utilizator pe fir, doar ce nu e încă ratat. */
    @Transactional(readOnly = true)
    public List<SiatdReceptionRow> pendingFor(UUID companyId, LocalDate today) {
        Company company = company(companyId);
        return rows(candidates(company), company).stream()
                .filter(r -> stateOf(r, today) == State.PENDING)
                .toList();
    }

    @Transactional
    public List<SiatdReceptionRow> confirm(List<UUID> operationIds, String code) {
        requireApprover();
        if (operationIds == null || operationIds.isEmpty() || operationIds.size() > MAX_PER_REQUEST) {
            throw new BusinessException(SIATD_CONFIRM_SELECTION);
        }
        String trimmed = code == null || code.isBlank() ? null : code.trim();
        if (trimmed != null && new LinkedHashSet<>(operationIds).size() > 1) {
            throw new BusinessException(SIATD_CODE_SINGLE);
        }
        if (trimmed != null && trimmed.length() > 60) {
            throw new BusinessException(SIATD_CODE_TOO_LONG);
        }
        UUID tenantId = TenantContext.require();
        Company company = company(tenantId);
        List<WeighingOperation> operations = new LinkedHashSet<>(operationIds).stream()
                .map(id -> operationService.requireOperation(id, tenantId))
                .toList();
        Map<UUID, List<WasteMovement>> lines = linesOf(operations);
        UUID userId = SecurityUtils.currentUser().getId();
        Instant now = Instant.now();
        for (WeighingOperation o : operations) {
            if (deadline(o, lines.getOrDefault(o.getId(), List.of()), company) == null) {
                throw new BusinessException(SIATD_NO_DEADLINE);
            }
            if (o.getSiatdConfirmedAt() == null) {
                o.setSiatdConfirmedAt(now);
                o.setSiatdConfirmedBy(userId);
                o.setSiatdCode(trimmed);
            }
        }
        return rows(operations, company, lines);
    }

    /** O confirmare dată din greșeală: recepția se întoarce la „de confirmat” sau la „ratate”, fără cod. */
    @Transactional
    public void unconfirm(UUID operationId) {
        requireApprover();
        WeighingOperation o = operationService.requireOperation(operationId, TenantContext.require());
        o.setSiatdConfirmedAt(null);
        o.setSiatdConfirmedBy(null);
        o.setSiatdCode(null);
    }

    // --- rândurile ---

    private List<SiatdReceptionRow> rows(Collection<WeighingOperation> operations, Company company) {
        return rows(operations, company, linesOf(operations));
    }

    private List<SiatdReceptionRow> rows(Collection<WeighingOperation> operations, Company company,
                                         Map<UUID, List<WasteMovement>> lines) {
        Map<UUID, String> names = new HashMap<>();
        userRepository.findAllById(operations.stream().map(WeighingOperation::getSiatdConfirmedBy)
                        .filter(Objects::nonNull).distinct().toList())
                .forEach(u -> names.put(u.getId(), WeighingOperationService.fullName(u)));
        List<SiatdReceptionRow> rows = new ArrayList<>();
        for (WeighingOperation o : operations) {
            List<WasteMovement> own = lines.getOrDefault(o.getId(), List.of());
            SiatdDeadline d = deadline(o, own, company);
            if (d == null) {
                continue;
            }
            // Doar liniile pe modulele bifate: fierul de pe aceeași recepție nu intră în tranzacția SIATD.
            Map<SiatdModule, LocalDate> enrolment = company.siatdEnrolment();
            BigDecimal kg = own.stream()
                    .filter(m -> ro.ecoregistru.util.SiatdFlow.of(m.getWasteCode().getCode())
                            .filter(enrolment::containsKey).isPresent())
                    .map(WasteMovement::getQuantity).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean person = o.getNaturalPerson() != null;
            String partner = person ? o.getNaturalPerson().getName()
                    : o.getPartner() == null ? null : o.getPartner().getName();
            rows.add(new SiatdReceptionRow(o.getId(), o.getNumber(), o.getDate(), o.getWorkPoint().getId(),
                    o.getWorkPoint().getName(), partner, person, d.modules(), kg, d.due(), d.reminder(),
                    o.getSiatdConfirmedAt(), names.get(o.getSiatdConfirmedBy()), o.getSiatdCode()));
        }
        return rows;
    }

    private static State stateOf(SiatdReceptionRow r, LocalDate today) {
        if (r.confirmedAt() != null) {
            return State.CONFIRMED;
        }
        return today.isAfter(r.due()) ? State.MISSED : State.PENDING;
    }

    private static SiatdDeadline deadline(WeighingOperation o, List<WasteMovement> lines, Company company) {
        Map<SiatdModule, LocalDate> enrolment = company.siatdEnrolment();
        return SiatdDeadlines.of(o.getType(), o.getStatus(), o.getDate(),
                lines.stream().map(m -> m.getWasteCode().getCode()).toList(), enrolment);
    }

    private Map<UUID, List<WasteMovement>> linesOf(Collection<WeighingOperation> operations) {
        if (operations.isEmpty()) {
            return Map.of();
        }
        return movementRepository.findAllByWeighingOperation_IdInOrderByLineNoAsc(
                        operations.stream().map(WeighingOperation::getId).toList()).stream()
                .collect(Collectors.groupingBy(m -> m.getWeighingOperation().getId()));
    }

    private List<WeighingOperation> candidates(Company company) {
        Map<SiatdModule, LocalDate> enrolment = company.siatdEnrolment();
        if (enrolment.isEmpty()) {
            return List.of();
        }
        return operationRepository.findSiatdCandidates(company.getId(),
                enrolment.containsKey(SiatdModule.MUNICIPAL), enrolment.containsKey(SiatdModule.PACKAGING),
                enrolment.containsKey(SiatdModule.WEEE), enrolment.containsKey(SiatdModule.BATTERY),
                enrolment.containsKey(SiatdModule.TYRE));
    }

    private Company company(UUID companyId) {
        return companyRepository.findById(companyId).orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
    }

    /** Controllerul are aceeași regulă; aici e jumătatea care ține și fără HTTP. */
    private static void requireApprover() {
        AppUser user = SecurityUtils.currentUser();
        if (!APPROVERS.contains(user.getRole())) {
            throw new AccessDeniedException("Doar administratorul sau consultantul confirmă recepțiile în SIATD.");
        }
    }
}
