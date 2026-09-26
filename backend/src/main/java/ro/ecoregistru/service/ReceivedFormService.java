package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.ReceivedFormRequest;
import ro.ecoregistru.controller.response.ReceivedFormResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.ReceivedForm;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ReceivedFormRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * D2.6 — registrul formularelor de transport primite, pe depozit (HG 1061/2008 art. 20 alin. (1), (5)).
 *
 * <p>Append-only: un rând scris nu se mai schimbă (baza refuză orice UPDATE, V72). O greșeală se îndreaptă cu un rând
 * nou care îl numește pe cel greșit și spune de ce; rândul greșit rămâne, cu numărul lui. Numerele de ordine cresc
 * fără goluri pe fiecare depozit, sub un lacăt consultativ, ca la operațiunile de cântar.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReceivedFormService {

    static final Set<String> KINDS = Set.of("ANEXA_3", "ANEXA_2");

    ReceivedFormRepository repository;
    WorkPointRepository workPointRepository;
    CompanyRepository companyRepository;
    WeighingOperationRepository operationRepository;
    DepotAccess depotAccess;
    ro.ecoregistru.service.export.ReceivedFormsRegisterGenerator generator;

    @Transactional(readOnly = true)
    public List<ReceivedFormResponse> list(UUID workPointId, int year) {
        UUID tenantId = TenantContext.require();
        WorkPoint depot = requireDepot(workPointId, tenantId);
        return responses(repository.findForRegister(tenantId, depot.getId(),
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)));
    }

    @Transactional
    public ReceivedFormResponse record(ReceivedFormRequest request) {
        UUID tenantId = TenantContext.require();
        WorkPoint depot = requireDepot(request.workPointId(), tenantId);
        return single(save(depot, request, null, null, tenantId));
    }

    /** Corectura: un rând nou, în registrul rândului îndreptat, care îl numește; rândul greșit rămâne. */
    @Transactional
    public ReceivedFormResponse correct(UUID id, ReceivedFormRequest request) {
        UUID tenantId = TenantContext.require();
        ReceivedForm wrong = repository.findByIdAndCompanyId(id, tenantId)
                .filter(f -> depotAccess.allows(f.getWorkPoint().getId()))
                .orElseThrow(() -> new NotFoundException(RECEIVED_FORM_NOT_FOUND));
        String reason = blankToNull(request.correctionReason());
        if (reason == null) {
            throw new BusinessException(RECEIVED_FORM_REASON_REQUIRED);
        }
        operationRepository.lockNumbering("received-forms:" + wrong.getWorkPoint().getId());
        if (repository.existsByCorrectsId(id)) {
            throw new BusinessException(RECEIVED_FORM_ALREADY_CORRECTED);
        }
        return single(save(wrong.getWorkPoint(), request, wrong.getId(), reason, tenantId));
    }

    /**
     * D2.5 → D2.6: la recepția unui transfer, B trece în registrul lui Anexa 3 a lui A — dacă A a tipărit-o (altfel
     * n-are număr, deci nimic de trecut). Cantitatea e cea de pe formular: a expeditorului.
     */
    @Transactional
    public void recordTransferReceipt(WeighingOperation operation, BigDecimal sentKg, String wasteDescription) {
        if (operation.getAnexa3Number() == null) {
            return;
        }
        Company company = operation.getCompany();
        ReceivedFormRequest request = new ReceivedFormRequest(operation.getTargetWorkPoint().getId(),
                operation.getReceivedOn(), "ANEXA_3", operation.getAnexa3Series(),
                String.valueOf(operation.getAnexa3Number()), operation.getDate(),
                company.getName() + " — " + operation.getWorkPoint().getName(), company.getCui(),
                wasteDescription, sentKg, operation.getId(), null);
        save(operation.getTargetWorkPoint(), request, null, null, company.getId());
    }

    /** PDF-ul registrului unui depozit pe un an: seria, numerele și „Pagina X din Y”. */
    @Transactional(readOnly = true)
    public byte[] render(UUID workPointId, int year) {
        UUID tenantId = TenantContext.require();
        WorkPoint depot = requireDepot(workPointId, tenantId);
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        String name = company.getCui() == null ? company.getName() : company.getName() + " · CUI " + company.getCui();
        return generator.pdf(name, depot.getName(), depot.getReceivedFormsSeries(), "Anul " + year,
                repository.findForRegister(tenantId, depot.getId(), LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)));
    }

    private ReceivedForm save(WorkPoint depot, ReceivedFormRequest request, UUID corrects, String reason, UUID tenantId) {
        if (request.receivedOn() == null) {
            throw new BusinessException(RECEIVED_FORM_DATE_REQUIRED);
        }
        String number = blankToNull(request.formNumber());
        if (number == null) {
            throw new BusinessException(RECEIVED_FORM_NUMBER_REQUIRED);
        }
        String sender = blankToNull(request.senderName());
        if (sender == null) {
            throw new BusinessException(RECEIVED_FORM_SENDER_REQUIRED);
        }
        String kind = request.formKind() == null ? "ANEXA_3" : request.formKind();
        if (!KINDS.contains(kind)) {
            throw new BusinessException(RECEIVED_FORM_KIND_UNKNOWN);
        }
        if (request.quantityKg() != null && request.quantityKg().signum() <= 0) {
            throw new BusinessException(RECEIVED_FORM_QUANTITY_NOT_POSITIVE);
        }
        UUID operationId = request.weighingOperationId() == null ? null
                : operationRepository.findByIdAndCompany_Id(request.weighingOperationId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(WEIGHING_OPERATION_NOT_FOUND)).getId();
        operationRepository.lockNumbering("received-forms:" + depot.getId());
        Integer max = repository.findMaxEntryNo(depot.getId());
        return repository.saveAndFlush(ReceivedForm.builder()
                .companyId(tenantId)
                .workPoint(depot)
                .entryNo(max == null ? 1 : max + 1)
                .receivedOn(request.receivedOn())
                .formKind(kind)
                .formSeries(blankToNull(request.formSeries()))
                .formNumber(number)
                .formDate(request.formDate())
                .senderName(sender)
                .senderCui(blankToNull(request.senderCui()))
                .wasteDescription(blankToNull(request.wasteDescription()))
                .quantityKg(request.quantityKg())
                .weighingOperationId(operationId)
                .correctsId(corrects)
                .correctionReason(reason)
                .createdBy(SecurityUtils.currentUser().getId())
                .createdAt(Instant.now())
                .build());
    }

    private WorkPoint requireDepot(UUID workPointId, UUID tenantId) {
        if (workPointId == null) {
            throw new BusinessException(RECEIVED_FORM_DEPOT_REQUIRED);
        }
        return depotAccess.require(workPointRepository.findByIdAndCompany_Id(workPointId, tenantId)
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND)));
    }

    private ReceivedFormResponse single(ReceivedForm f) {
        return responses(List.of(f)).get(0);
    }

    /** Numerele rândurilor legate (cel îndreptat, cel care îndreaptă) se citesc din aceeași listă sau din bază. */
    private List<ReceivedFormResponse> responses(List<ReceivedForm> rows) {
        Map<UUID, Integer> numbers = new HashMap<>();
        Map<UUID, Integer> correctedBy = new HashMap<>();
        rows.forEach(f -> numbers.put(f.getId(), f.getEntryNo()));
        for (ReceivedForm f : rows) {
            if (f.getCorrectsId() != null) {
                correctedBy.put(f.getCorrectsId(), f.getEntryNo());
                numbers.computeIfAbsent(f.getCorrectsId(),
                        id -> repository.findById(id).map(ReceivedForm::getEntryNo).orElse(null));
            }
        }
        return rows.stream().map(f -> new ReceivedFormResponse(f.getId(), f.getWorkPoint().getId(),
                f.getWorkPoint().getName(), f.getEntryNo(), f.getReceivedOn(), f.getFormKind(), f.getFormSeries(),
                f.getFormNumber(), f.getFormDate(), f.getSenderName(), f.getSenderCui(), f.getWasteDescription(),
                f.getQuantityKg(), f.getWeighingOperationId(),
                f.getCorrectsId() == null ? null : numbers.get(f.getCorrectsId()), f.getCorrectionReason(),
                correctedBy.get(f.getId()), f.getCreatedAt())).toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
