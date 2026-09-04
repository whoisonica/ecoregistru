package ro.ecoregistru.service.export;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import ro.ecoregistru.controller.response.MonthlyEvidenceResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Assembles the Anexa 1 sheets for a year from the two things that already exist: the monthly
 * evidence (chapter 1, with the running stock the engine computes) and the movements themselves
 * (chapters 2, 3 and 4, which need attributes the monthly cache does not carry — the section, the
 * container, the treatment, the operation and the operator).
 *
 * <p>Chapter 1 deliberately comes from the evidence rather than being recomputed here: the stock
 * identity is the engine's contract and there must be exactly one implementation of it.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Anexa1SheetBuilder {

    static final BigDecimal KG_PER_TON = new BigDecimal("1000");

    /**
     * @param evidence the monthly lines for the year, already filtered to the wanted work points
     * @param movements every live movement of that year for the same tenant
     */
    /**
     * @param sectionsByWorkPoint what each work point has defined under "Secţia", used when a
     *                            month's movements name none — see {@link #section}
     */
    public List<Anexa1Sheet> build(Company company,
                                   int year,
                                   List<MonthlyEvidenceResponse> evidence,
                                   List<WasteMovement> movements,
                                   Map<UUID, List<String>> sectionsByWorkPoint) {
        // The form is per (work point, waste code); the evidence lines already come in that shape.
        Map<Key, List<MonthlyEvidenceResponse>> byPair = evidence.stream()
                .collect(Collectors.groupingBy(e -> new Key(e.workPointId(), e.wasteCodeId()),
                        java.util.LinkedHashMap::new, Collectors.toList()));

        // Only Anexa 1 movements: goods taken over from third parties belong to the art. 48
        // register and never appear on this form (HG 856/2002 art. 2 alin. (1)).
        Map<Key, List<WasteMovement>> movementsByPair = movements.stream()
                .filter(m -> m.getRegister() == WasteRegister.ANEXA_1)
                .collect(Collectors.groupingBy(
                        m -> new Key(m.getWorkPoint().getId(), m.getWasteCode().getId()),
                        java.util.LinkedHashMap::new, Collectors.toList()));

        List<Anexa1Sheet> sheets = new ArrayList<>();
        for (Map.Entry<Key, List<MonthlyEvidenceResponse>> entry : byPair.entrySet()) {
            List<MonthlyEvidenceResponse> lines = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(MonthlyEvidenceResponse::month))
                    .toList();
            if (lines.isEmpty()) {
                continue;
            }
            Map<Integer, List<WasteMovement>> byMonth =
                    movementsByPair.getOrDefault(entry.getKey(), List.of()).stream()
                            .collect(Collectors.groupingBy(m -> m.getDate().getMonthValue()));

            sheets.add(sheet(company, year, lines, byMonth,
                    sectionsByWorkPoint.getOrDefault(entry.getKey().workPointId(), List.of())));
        }
        sheets.sort(Comparator.comparing(Anexa1Sheet::workPointName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Anexa1Sheet::wasteCode));
        return sheets;
    }

    private Anexa1Sheet sheet(Company company, int year,
                              List<MonthlyEvidenceResponse> lines,
                              Map<Integer, List<WasteMovement>> byMonth,
                              List<String> sections) {
        MonthlyEvidenceResponse first = lines.get(0);

        // The header's "Stoc/kg" is what the year opened with: January's closing stock, wound back
        // through January's own movements.
        BigDecimal opening = first.closingStock()
                .subtract(first.totalGenerated())
                .add(first.totalRecovered())
                .add(first.totalDisposed())
                .add(first.totalUnclassifiedOut());

        List<Anexa1Sheet.Anexa1MonthRow> rows = new ArrayList<>();
        for (MonthlyEvidenceResponse line : lines) {
            List<WasteMovement> monthly = byMonth.getOrDefault(line.month(), List.of());
            rows.add(row(line, monthly, sections));
        }

        return new Anexa1Sheet(
                company.getName(), year, first.workPointName(),
                first.wasteCodeName(), first.wasteCode(), first.hazardous(),
                physicalState(byMonth), opening, rows);
    }

    private Anexa1Sheet.Anexa1MonthRow row(MonthlyEvidenceResponse line,
                                           List<WasteMovement> monthly,
                                           List<String> sections) {
        List<WasteMovement> recoveries = monthly.stream()
                .filter(m -> m.getOperation() == WasteOperation.RECOVERED).toList();
        List<WasteMovement> disposals = monthly.stream()
                .filter(m -> m.getOperation() == WasteOperation.DISPOSED).toList();

        // Cap. 2 is about what happened ON the site, and its two "Cant." columns answer two
        // different questions.
        //
        // "Stocare: Cant." is what the month produced — confirmed on 336 filled months, where it
        // equals the generated quantity and never the remaining stock, even on a sheet carrying
        // 50 tonnes of it. It used to come out 0 for a client who records only handovers, but that
        // was the generation bug, not this column: with generation implied from the exits it now
        // carries the real figure.
        //
        // "Tratare: Cant." is only what this company did itself. A recovery performed by a partner
        // is treated at their place and belongs in cap. 3, so a client who just hands the cardboard
        // over treats nothing and the column reads 0 — answer U, 24.08.2026, and confirmed again
        // on 25.08 when she saw a figure there and asked why. Printing the quantity would claim an
        // operation that never happened.
        //
        // And "Modul" and "Scopul" describe that same treatment, so they are read from the same
        // movements as the quantity — audit point 14, 02.09.2026. Taken from the whole month, as
        // they were until then, a handover on which the client had also ticked a treatment method
        // printed "Modul: TM" next to "Cant.: 0.000": a treatment declared with no quantity, a
        // rubric contradicting itself on a filed form. The corpus settles the form of the rubric
        // (regula de lucru 3): Panemar, a bakery that only hands over, writes 0.000 with Modul "-",
        // while Hamburger, which really does bale, writes both. Silence on both is their practice.
        BigDecimal storedQuantity = line.totalGenerated();
        List<WasteMovement> treatedOnSite = monthly.stream()
                .filter(m -> m.getOperation().isExit() && m.getPartner() == null)
                .toList();
        BigDecimal treatedHere = treatedOnSite.stream()
                .map(this::kg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Anexa1Sheet.Anexa1MonthRow(
                line.month(),
                line.totalGenerated(), line.totalRecovered(), line.totalDisposed(),
                line.closingStock(),
                section(monthly, sections),
                storedQuantity,
                distinct(monthly, m -> name(m.getStorageType())),
                treatedHere,
                distinct(treatedOnSite, m -> name(m.getTreatmentMethod())),
                distinct(treatedOnSite, m -> m.getOperationCode() == null
                        ? null : name(m.getOperationCode().treatmentPurpose())),
                distinct(monthly, m -> name(m.getTransportMeans())),
                distinct(monthly, m -> name(m.getWasteDestination())),
                handovers(recoveries),
                handovers(disposals));
    }

    /**
     * The lines chapter 3 or chapter 4 gets for one month: one per distinct (operation, operator)
     * pair, in the order the movements happened.
     *
     * <p>Answer B, 24.08.2026: "trebuie un rând nou pentru fiecare chestie nouă pentru luna
     * respectivă". Two handovers of the same waste in the same month, under different R codes or
     * to different operators, are two lines — not two values crammed into one cell, which is what
     * this printed before. The quantities still add up to the month's figure in chapter 1, because
     * they are summed here exactly as {@code EvidenceCalculator} sums them: kilograms, and a
     * movement still waiting for the recipient's weighbridge contributes nothing.
     *
     * <p>A group where <em>no</em> movement has been weighed yet keeps a {@code null} quantity, so
     * the form prints an empty cell instead of a zero it cannot stand behind. The operator's name
     * still appears — the handover happened, only the weight is missing.
     */
    private List<Anexa1Sheet.Handover> handovers(List<WasteMovement> movements) {
        Map<String, BigDecimal> quantities = new java.util.LinkedHashMap<>();
        Map<String, Anexa1Sheet.Handover> rubrics = new java.util.LinkedHashMap<>();

        for (WasteMovement m : movements.stream()
                .sorted(Comparator.comparing(WasteMovement::getDate)).toList()) {
            String operation = name(m.getOperationCode());
            String operator = m.getPartner() == null ? null : m.getPartner().getName();
            String key = operation + "\0" + operator;

            rubrics.putIfAbsent(key, new Anexa1Sheet.Handover(null, operation, operator));
            if (m.getQuantity() != null) {
                quantities.merge(key, kg(m), BigDecimal::add);
            }
        }
        return rubrics.entrySet().stream()
                .map(e -> new Anexa1Sheet.Handover(quantities.get(e.getKey()),
                        e.getValue().operation(), e.getValue().operator()))
                .toList();
    }

    /**
     * The "Secţia" column of cap. 2: where the waste came from inside the work point.
     *
     * <p>What the movements say, when they say anything. When they do not — the ordinary case for a
     * client who records a handover and nothing else — the work point's own sections are printed
     * instead: "Birouri, Producţie". Asked for on 25.08.2026 on her own sheet, where the column
     * came out empty because no movement named a section.
     *
     * <p>It is the provenance of the site's waste rather than a claim about one load, which is why
     * it prints all of them and not a guess at one. A work point with no sections defined still
     * prints an empty column — nothing is invented out of nowhere.
     */
    private String section(List<WasteMovement> monthly, List<String> sections) {
        String recorded = distinct(monthly, m -> m.getInternalGenerator() == null
                ? null : m.getInternalGenerator().getName());
        if (!recorded.isBlank()) {
            return recorded;
        }
        return monthly.isEmpty() ? "" : String.join(", ", sections);
    }

    /** The physical state as recorded; blank when the movements of the year disagree or say nothing. */
    private String physicalState(Map<Integer, List<WasteMovement>> byMonth) {
        Set<String> states = byMonth.values().stream()
                .flatMap(List::stream)
                .map(m -> name(m.getPhysicalState()))
                .filter(v -> v != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return states.size() == 1 ? states.iterator().next() : "";
    }

    /**
     * The distinct values of one rubric within a month, joined.
     *
     * <p>This is chapter 2 only, and it stays joined on purpose. Chapters 3 and 4 split into one
     * line per handover (see {@link #handovers}), but chapter 2's line carries the month's stored
     * quantity — "Stocare: Cant." is what the month generated, on all 336 filled months — and that
     * figure belongs to the month, not to any one section or means of transport. Splitting the row
     * would mean splitting that quantity on a rule nobody has given us.
     */
    private String distinct(List<WasteMovement> movements, Function<WasteMovement, String> of) {
        return movements.stream()
                .map(of)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private BigDecimal kg(WasteMovement m) {
        if (m.getQuantity() == null) {
            return BigDecimal.ZERO;
        }
        return m.getUnit() == Unit.TONS ? m.getQuantity().multiply(KG_PER_TON) : m.getQuantity();
    }

    /** A sheet is one work point and one waste code. */
    private record Key(UUID workPointId, UUID wasteCodeId) {}
}
