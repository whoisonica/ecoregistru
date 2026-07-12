package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.response.EvidenceRegenerationResponse;
import ro.ecoregistru.controller.response.MonthlyEvidenceResponse;
import ro.ecoregistru.entity.MonthlyEvidence;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.MonthlyEvidenceRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.security.TenantContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The evidence engine (FAZA EVID / E1). Movements are the source of truth; this recomputes the
 * cached {@link MonthlyEvidence} lines for a whole year in one pass so cumulative stock is
 * well-defined. Stock model (from MonthlyEvidence's contract):
 * closingStock = previous closingStock + generated + collected − handedOver − recovered − disposed.
 * Quantities are normalised to KG. Everything is tenant-scoped via {@link TenantContext}.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EvidenceCalculator {

    private static final BigDecimal KG_PER_TON = BigDecimal.valueOf(1000);

    MonthlyEvidenceRepository evidenceRepository;
    WasteMovementRepository movementRepository;
    CompanyRepository companyRepository;

    /** Groups movements by the (work point, waste code) an evidence line is scoped to. */
    private record GroupKey(UUID workPointId, UUID wasteCodeId) {}

    /**
     * Recomputes every evidence line for the tenant and the given year from its movements.
     * Opening stock for January carries over from the previous December's closing stock
     * (if that year was already generated), otherwise starts at zero.
     */
    @Transactional
    public EvidenceRegenerationResponse regenerateYear(int year) {
        UUID tenantId = TenantContext.require();

        List<WasteMovement> movements = movementRepository
                .findAllByCompany_IdAndDeletedFalseAndDateBetween(
                        tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));

        // Opening balance per group = previous year's December closing stock.
        Map<GroupKey, BigDecimal> openingStock = new HashMap<>();
        for (MonthlyEvidence prev : evidenceRepository.findByCompany_IdAndYear(tenantId, year - 1)) {
            if (prev.getMonth() == 12) {
                openingStock.put(
                        new GroupKey(prev.getWorkPoint().getId(), prev.getWasteCode().getId()),
                        prev.getClosingStock());
            }
        }

        // Replace the year's lines wholesale; flush the deletes before inserting to avoid
        // colliding with the (company, work point, year, month, code) unique constraint.
        evidenceRepository.deleteByCompany_IdAndYear(tenantId, year);
        evidenceRepository.flush();

        Map<GroupKey, List<WasteMovement>> byGroup = movements.stream()
                .collect(Collectors.groupingBy(
                        m -> new GroupKey(m.getWorkPoint().getId(), m.getWasteCode().getId())));

        Instant now = Instant.now();
        var company = companyRepository.getReferenceById(tenantId);
        List<MonthlyEvidence> lines = new ArrayList<>();

        for (List<WasteMovement> group : byGroup.values()) {
            WorkPoint workPoint = group.get(0).getWorkPoint();
            WasteCode wasteCode = group.get(0).getWasteCode();
            GroupKey key = new GroupKey(workPoint.getId(), wasteCode.getId());

            Map<Integer, List<WasteMovement>> byMonth = group.stream()
                    .collect(Collectors.groupingBy(m -> m.getDate().getMonthValue()));

            BigDecimal running = openingStock.getOrDefault(key, BigDecimal.ZERO);
            for (int month = 1; month <= 12; month++) {
                List<WasteMovement> monthMovements = byMonth.get(month);
                if (monthMovements == null) {
                    continue; // no activity: stock carries forward untouched
                }
                Totals totals = sum(monthMovements);
                running = running
                        .add(totals.generated).add(totals.collected)
                        .subtract(totals.handedOver).subtract(totals.recovered).subtract(totals.disposed);

                lines.add(MonthlyEvidence.builder()
                        .company(company)
                        .workPoint(workPoint)
                        .year(year)
                        .month(month)
                        .wasteCode(wasteCode)
                        .totalGenerated(totals.generated)
                        .totalCollected(totals.collected)
                        .totalHandedOver(totals.handedOver)
                        .totalRecovered(totals.recovered)
                        .totalDisposed(totals.disposed)
                        .closingStock(running)
                        .generatedAt(now)
                        .build());
            }
        }

        evidenceRepository.saveAll(lines);
        return new EvidenceRegenerationResponse(year, lines.size());
    }

    @Transactional(readOnly = true)
    public List<MonthlyEvidenceResponse> list(int year, Integer month, UUID workPointId) {
        UUID tenantId = TenantContext.require();
        return evidenceRepository.findByCompany_IdAndYear(tenantId, year).stream()
                .filter(e -> month == null || e.getMonth() == month)
                .filter(e -> workPointId == null || e.getWorkPoint().getId().equals(workPointId))
                .sorted(Comparator
                        .comparing((MonthlyEvidence e) -> e.getWorkPoint().getName())
                        .thenComparing(MonthlyEvidence::getMonth)
                        .thenComparing(e -> e.getWasteCode().getCode()))
                .map(this::toResponse)
                .toList();
    }

    private Totals sum(List<WasteMovement> movements) {
        Totals t = new Totals();
        for (WasteMovement m : movements) {
            BigDecimal kg = toKg(m.getQuantity(), m.getUnit());
            switch (m.getOperation()) {
                case GENERATED -> t.generated = t.generated.add(kg);
                case COLLECTED -> t.collected = t.collected.add(kg);
                case HANDED_OVER -> t.handedOver = t.handedOver.add(kg);
                case RECOVERED -> t.recovered = t.recovered.add(kg);
                case DISPOSED -> t.disposed = t.disposed.add(kg);
            }
        }
        return t;
    }

    private static BigDecimal toKg(BigDecimal quantity, Unit unit) {
        return unit == Unit.TONS ? quantity.multiply(KG_PER_TON) : quantity;
    }

    private MonthlyEvidenceResponse toResponse(MonthlyEvidence e) {
        return new MonthlyEvidenceResponse(
                e.getId(),
                e.getWorkPoint().getId(),
                e.getWorkPoint().getName(),
                e.getYear(),
                e.getMonth(),
                e.getWasteCode().getId(),
                e.getWasteCode().getCode(),
                e.getWasteCode().getName(),
                e.getWasteCode().isHazardous(),
                e.getTotalGenerated(),
                e.getTotalCollected(),
                e.getTotalHandedOver(),
                e.getTotalRecovered(),
                e.getTotalDisposed(),
                e.getClosingStock(),
                e.getGeneratedAt());
    }

    /** Mutable accumulator for one month's operation totals (KG). */
    private static final class Totals {
        BigDecimal generated = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal handedOver = BigDecimal.ZERO;
        BigDecimal recovered = BigDecimal.ZERO;
        BigDecimal disposed = BigDecimal.ZERO;
    }
}
