package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.response.EvidenceRegenerationResponse;
import ro.ecoregistru.controller.response.MonthlyEvidenceResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.MonthlyEvidence;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.MonthlyEvidenceRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.export.Anexa1Sheet;
import ro.ecoregistru.service.export.AnnualDeclaration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The evidence engine (FAZA EVID): rebuilds the cached {@link MonthlyEvidence} lines of Anexa 1
 * from the movements, which are the source of truth.
 *
 * <p><b>What it aggregates.</b> Only movements of the {@code ANEXA_1} register. HG 856/2002
 * art. 2 alin. (1) keeps the fişa to <em>"deşeurile generate în cadrul activităţilor proprii"</em>,
 * so goods taken over from third parties never reach it — they belong to the art. 48 register.
 *
 * <p><b>The stock identity.</b> Anexa 1 cap. 1 has the columns
 * <em>Generate | din care: valorificată | eliminată final | rămasă în stoc</em> and no "handed
 * over" one, so
 * <pre>stock = previous stock + generated − recovered − disposed − unclassified out</pre>
 * A handover is reported under "valorificată" or "eliminată final" following the R/D family of the
 * operation its recipient performs; it is a description of the same physical exit, never a second
 * one. Handovers left over from before the code was mandatory have no family: they leave the stock
 * but enter no official column, and the line is marked incomplete rather than closed over a guess.
 *
 * <p><b>Shape.</b> A (work point, code) that is live in a year gets all 12 months, activity or
 * not — the form is a 12-row table and a month with no movements still has to show its stock. A
 * pair whose stock carried over from December of the previous year is live even with no movements
 * at all, so it cannot silently vanish from the report.
 *
 * <p>Quantities are normalised to KG. Everything is tenant-scoped via {@link TenantContext}.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EvidenceCalculator {

    private static final BigDecimal KG_PER_TON = BigDecimal.valueOf(1000);

    MonthlyEvidenceRepository evidenceRepository;
    WasteMovementRepository movementRepository;
    CompanyRepository companyRepository;
    ro.ecoregistru.service.export.Anexa1SheetBuilder anexa1SheetBuilder;
    ro.ecoregistru.repository.InternalGeneratorRepository internalGeneratorRepository;
    ro.ecoregistru.service.export.AnnualDeclarationBuilder annualDeclarationBuilder;

    /** Groups movements by the (work point, waste code) an evidence line is scoped to. */
    private record GroupKey(UUID workPointId, UUID wasteCodeId) {}

    /** A pair that carried stock into the year, with the entities needed to write its lines. */
    private record CarriedOver(WorkPoint workPoint, WasteCode wasteCode, BigDecimal openingStock) {}

    /**
     * Recomputes the tenant's evidence for the given year from its movements, then for every later
     * year up to the last one cached. Stock is cumulative across years, so regenerating 2025 after
     * a correction leaves 2026 wrong unless it is rebuilt too — the cascade is what makes a
     * back-dated fix land everywhere it is visible.
     */
    @Transactional
    public EvidenceRegenerationResponse regenerateYear(int year) {
        UUID tenantId = TenantContext.require();

        int lines = regenerateSingleYear(tenantId, year);

        Integer lastCachedYear = evidenceRepository.findMaxYear(tenantId);
        List<Integer> cascaded = new ArrayList<>();
        if (lastCachedYear != null) {
            // Walk the whole range, gaps included: a year with no cached lines still has to be
            // rebuilt, otherwise it stays the hole that stops the stock from reaching the years
            // after it.
            for (int later = year + 1; later <= lastCachedYear; later++) {
                lines += regenerateSingleYear(tenantId, later);
                cascaded.add(later);
            }
        }
        return new EvidenceRegenerationResponse(year, lines, cascaded);
    }

    /** Rebuilds one year in isolation; returns how many lines it wrote. */
    private int regenerateSingleYear(UUID tenantId, int year) {
        List<WasteMovement> movements = movementRepository
                .findAllByCompany_IdAndDeletedFalseAndDateBetween(
                        tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));

        // Opening balance per group = previous year's December closing stock. A pair carrying
        // stock is live this year even with no movements, so it also seeds the group set below.
        Map<GroupKey, CarriedOver> carried = new LinkedHashMap<>();
        for (MonthlyEvidence prev : evidenceRepository.findByCompany_IdAndYear(tenantId, year - 1)) {
            if (prev.getMonth() == 12 && prev.getClosingStock().signum() != 0) {
                carried.put(
                        new GroupKey(prev.getWorkPoint().getId(), prev.getWasteCode().getId()),
                        new CarriedOver(prev.getWorkPoint(), prev.getWasteCode(), prev.getClosingStock()));
            }
        }

        // Replace the year's lines wholesale; flush the deletes before inserting to avoid
        // colliding with the (company, work point, year, month, code) unique constraint.
        evidenceRepository.deleteByCompany_IdAndYear(tenantId, year);
        evidenceRepository.flush();

        Map<GroupKey, List<WasteMovement>> byGroup = movements.stream()
                .filter(m -> m.getRegister() == WasteRegister.ANEXA_1)
                .collect(Collectors.groupingBy(
                        m -> new GroupKey(m.getWorkPoint().getId(), m.getWasteCode().getId())));

        Set<GroupKey> groups = new LinkedHashSet<>(byGroup.keySet());
        groups.addAll(carried.keySet());

        Instant now = Instant.now();
        Company company = companyRepository.getReferenceById(tenantId);
        List<MonthlyEvidence> lines = new ArrayList<>();

        for (GroupKey key : groups) {
            List<WasteMovement> group = byGroup.getOrDefault(key, List.of());
            CarriedOver from = carried.get(key);
            WorkPoint workPoint = group.isEmpty() ? from.workPoint() : group.get(0).getWorkPoint();
            WasteCode wasteCode = group.isEmpty() ? from.wasteCode() : group.get(0).getWasteCode();

            Map<Integer, List<WasteMovement>> byMonth = group.stream()
                    .collect(Collectors.groupingBy(m -> m.getDate().getMonthValue()));

            BigDecimal running = from == null ? BigDecimal.ZERO : from.openingStock();
            for (int month = 1; month <= 12; month++) {
                Totals totals = sum(byMonth.getOrDefault(month, List.of()));

                // What left the site this month was produced somewhere, and on this form that
                // somewhere is here: cap. 1 reads "Generate — din care: valorificată | eliminată
                // final | rămasă în stoc", so the three are parts of the first and cannot exceed
                // it. When the recorded generation plus the stock carried in does not cover the
                // exits, the difference is generation that nobody wrote down.
                //
                // Not an invented figure: it is the quantity already recorded on the way out,
                // acknowledged in the column it must have come from. When the client does record
                // generation, or the carried stock covers the exit, this is zero and nothing
                // changes. Specialist, 25.08.2026: "cum poţi să valorifici ceva ce nu este generat?"
                BigDecimal exits = totals.recovered
                        .add(totals.disposed)
                        .add(totals.unclassifiedOut);
                BigDecimal covered = running.add(totals.generated);
                BigDecimal implied = exits.subtract(covered).max(BigDecimal.ZERO);
                BigDecimal generated = totals.generated.add(implied);

                running = running.add(generated).subtract(exits);

                lines.add(MonthlyEvidence.builder()
                        .company(company)
                        .workPoint(workPoint)
                        .year(year)
                        .month(month)
                        .wasteCode(wasteCode)
                        .totalGenerated(generated)
                        .impliedGenerated(implied)
                        .totalRecovered(totals.recovered)
                        .totalDisposed(totals.disposed)
                        .totalHandedOver(totals.handedOver)
                        .totalUnclassifiedOut(totals.unclassifiedOut)
                        .awaitingWeighing(totals.awaitingWeighing)
                        .closingStock(running)
                        .generatedAt(now)
                        .build());
            }
        }

        evidenceRepository.saveAll(lines);
        return lines.size();
    }

    /**
     * The Anexa 1 sheets for a year: one per (work point, waste code), in the shape the form
     * prints. Chapter 1 comes from the cached monthly lines — the stock identity has one
     * implementation and it is the one above — while chapters 2 to 4 need attributes the cache
     * does not carry, so they are read from the movements themselves.
     */
    @Transactional(readOnly = true)
    public List<Anexa1Sheet> anexa1(int year, UUID workPointId) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.getReferenceById(tenantId);
        List<MonthlyEvidenceResponse> lines = list(year, null, workPointId);
        List<WasteMovement> movements = movementRepository
                .findAllByCompany_IdAndDeletedFalseAndDateBetween(
                        tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        return anexa1SheetBuilder.build(company, year, lines, movements,
                sectionsByWorkPoint(tenantId));
    }

    /**
     * What each work point has defined under "Secţia", in name order. Cap. 2 falls back to these
     * when a month's movements name no section of their own.
     */
    private Map<UUID, List<String>> sectionsByWorkPoint(UUID tenantId) {
        Map<UUID, List<String>> byWorkPoint = new LinkedHashMap<>();
        for (var section : internalGeneratorRepository.findAllByCompany_IdOrderByNameAsc(tenantId)) {
            if (section.isActive()) {
                byWorkPoint.computeIfAbsent(section.getWorkPoint().getId(),
                        k -> new ArrayList<>()).add(section.getName());
            }
        }
        return byWorkPoint;
    }

    /**
     * The annual declaration for a year: one sheet per work point, one row per waste code. It
     * summarises exactly what {@link #anexa1(int, UUID)} details month by month, and reads the same
     * cached lines so the two documents cannot disagree.
     */
    @Transactional(readOnly = true)
    public List<AnnualDeclaration> annualDeclaration(int year, UUID workPointId) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.getReferenceById(tenantId);
        List<MonthlyEvidenceResponse> lines = list(year, null, workPointId);
        List<WasteMovement> movements = movementRepository
                .findAllByCompany_IdAndDeletedFalseAndDateBetween(
                        tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        return annualDeclarationBuilder.build(company, year, lines, movements);
    }

    @Transactional(readOnly = true)
    public List<MonthlyEvidenceResponse> list(int year, Integer month, UUID workPointId) {
        UUID tenantId = TenantContext.require();
        return evidenceRepository.findByCompany_IdAndYear(tenantId, year).stream()
                .filter(e -> month == null || e.getMonth() == month)
                .filter(e -> workPointId == null || e.getWorkPoint().getId().equals(workPointId))
                .sorted(Comparator
                        .comparing((MonthlyEvidence e) -> e.getWorkPoint().getName())
                        .thenComparing(e -> e.getWasteCode().getCode())
                        .thenComparing(MonthlyEvidence::getMonth))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Adds up one month of Anexa 1 movements.
     *
     * <p>An exit lands in the column its R/D code implies — "valorificata" or "eliminata final" —
     * whoever performed it. Handing waste to a recycler is a RECOVERED with a partner named, so it
     * is counted once, in the column the form has, and only remembered separately as the memo
     * "din care predat". UNCLASSIFIED_OUT is the legacy state: the quantity left the site, so it
     * leaves the stock, but it enters no official column and marks the line incomplete.
     */
    private Totals sum(List<WasteMovement> movements) {
        Totals t = new Totals();
        for (WasteMovement m : movements) {
            // Waiting for the recipient's weighbridge: it left, but with how much is not known
            // yet. Nothing is added to any column — a guess here would land on an official form —
            // and the line is marked provisional instead.
            if (m.getQuantity() == null) {
                t.awaitingWeighing = true;
                continue;
            }
            BigDecimal kg = toKg(m.getQuantity(), m.getUnit());
            switch (m.getOperation()) {
                case GENERATED -> t.generated = t.generated.add(kg);
                case RECOVERED -> {
                    t.recovered = t.recovered.add(kg);
                    if (m.getPartner() != null) {
                        t.handedOver = t.handedOver.add(kg); // memo: "din care predat"
                    }
                }
                case DISPOSED -> {
                    t.disposed = t.disposed.add(kg);
                    if (m.getPartner() != null) {
                        t.handedOver = t.handedOver.add(kg); // memo: "din care predat"
                    }
                }
                case UNCLASSIFIED_OUT -> t.unclassifiedOut = t.unclassifiedOut.add(kg);
                // Takeovers are art. 48 and are filtered out before this point (HG 856 art. 2(1)).
                case COLLECTED -> { }
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
                e.getImpliedGenerated(),
                e.getTotalRecovered(),
                e.getTotalDisposed(),
                e.getTotalHandedOver(),
                e.getTotalUnclassifiedOut(),
                e.getTotalUnclassifiedOut().signum() > 0 || e.isAwaitingWeighing(),
                e.isAwaitingWeighing(),
                e.getClosingStock(),
                e.getGeneratedAt());
    }

    /** Mutable accumulator for one month's Anexa 1 totals (KG). */
    private static final class Totals {
        BigDecimal generated = BigDecimal.ZERO;
        BigDecimal recovered = BigDecimal.ZERO;
        BigDecimal disposed = BigDecimal.ZERO;
        /** Subset of recovered + disposed: how much left as a handover. Not a stock term. */
        BigDecimal handedOver = BigDecimal.ZERO;
        BigDecimal unclassifiedOut = BigDecimal.ZERO;
        /** At least one exit is still waiting for the recipient to weigh it. */
        boolean awaitingWeighing = false;
    }
}
