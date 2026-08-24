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
    public List<Anexa1Sheet> build(Company company,
                                   int year,
                                   List<MonthlyEvidenceResponse> evidence,
                                   List<WasteMovement> movements) {
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

            sheets.add(sheet(company, year, lines, byMonth));
        }
        sheets.sort(Comparator.comparing(Anexa1Sheet::workPointName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Anexa1Sheet::wasteCode));
        return sheets;
    }

    private Anexa1Sheet sheet(Company company, int year,
                              List<MonthlyEvidenceResponse> lines,
                              Map<Integer, List<WasteMovement>> byMonth) {
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
            rows.add(row(line, monthly));
        }

        return new Anexa1Sheet(
                company.getName(), year, first.workPointName(),
                first.wasteCodeName(), first.wasteCode(),
                physicalState(byMonth), opening, rows);
    }

    private Anexa1Sheet.Anexa1MonthRow row(MonthlyEvidenceResponse line, List<WasteMovement> monthly) {
        List<WasteMovement> recoveries = monthly.stream()
                .filter(m -> m.getOperation() == WasteOperation.RECOVERED).toList();
        List<WasteMovement> disposals = monthly.stream()
                .filter(m -> m.getOperation() == WasteOperation.DISPOSED).toList();

        // Cap. 2 is about what happened ON the site. What was stored is what the month produced —
        // confirmed on 336 filled months, where "Stocare: Cant." equals the month's generated
        // quantity and never the remaining stock, even on a sheet carrying 50 tonnes of it.
        //
        // What was "treated" is only the part this company handled itself. ⚠️ Careful: the comment
        // here used to claim the filled models show 0 in that column when a partner does the work.
        // They do not — checked on 24.08.2026, all 336 months read "Tratare: Cant." = the month's
        // quantity, including where chapter 3 names an outside recycler. The corpus is a recycling
        // company that really does sort and bale on site, so its sheets may be describing its own
        // treatment rather than a rule; a client who only hands cardboard over treats nothing.
        // Left as it is until the specialist answers (question U) — printing the quantity would
        // claim a treatment that never happened.
        BigDecimal treatedHere = monthly.stream()
                .filter(m -> m.getOperation().isExit() && m.getPartner() == null)
                .map(this::kg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Anexa1Sheet.Anexa1MonthRow(
                line.month(),
                line.totalGenerated(), line.totalRecovered(), line.totalDisposed(),
                line.closingStock(),
                distinct(monthly, m -> m.getInternalGenerator() == null
                        ? null : m.getInternalGenerator().getName()),
                line.totalGenerated(),
                distinct(monthly, m -> name(m.getStorageType())),
                treatedHere,
                distinct(monthly, m -> name(m.getTreatmentMethod())),
                distinct(monthly, m -> m.getOperationCode() == null
                        ? null : name(m.getOperationCode().treatmentPurpose())),
                distinct(monthly, m -> name(m.getTransportMeans())),
                distinct(monthly, m -> name(m.getWasteDestination())),
                distinct(recoveries, m -> name(m.getOperationCode())),
                distinct(recoveries, m -> m.getPartner() == null ? null : m.getPartner().getName()),
                distinct(disposals, m -> name(m.getOperationCode())),
                distinct(disposals, m -> m.getPartner() == null ? null : m.getPartner().getName()));
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
     * The distinct values of one rubric within a month, joined. The form has twelve rows and no
     * room for a thirteenth, so a month with two different operators prints both rather than
     * silently losing one.
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
