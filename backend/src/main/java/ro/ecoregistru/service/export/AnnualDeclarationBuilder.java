package ro.ecoregistru.service.export;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import ro.ecoregistru.controller.response.MonthlyEvidenceResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Assembles the annual declaration — one {@link AnnualDeclaration} per work point, one row per
 * waste code — from the same two sources as {@link Anexa1SheetBuilder}: the cached monthly evidence
 * (the quantities and the stock) and the movements themselves (who recovered or disposed of what,
 * under which code).
 *
 * <p>Chapter 1 of the fişa and this sheet must agree to the kilogram, so neither recomputes the
 * other: both read the evidence the engine wrote. What this adds is only the year-level fold —
 * twelve monthly lines into one — and the "prin cine" column, which the monthly cache does not
 * carry.
 */
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnnualDeclarationBuilder {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * @param evidence the monthly lines of the year, already filtered to the wanted work points
     * @param movements every live movement of that year for the same tenant
     */
    public List<AnnualDeclaration> build(Company company,
                                         int year,
                                         List<MonthlyEvidenceResponse> evidence,
                                         List<WasteMovement> movements) {
        // Every filled model in the corpus is one work point per sheet — "punct de lucru: CLUJ",
        // "Punct de lucru Bragadiru" — and the same company files one per site.
        Map<UUID, List<MonthlyEvidenceResponse>> byWorkPoint = evidence.stream()
                .collect(Collectors.groupingBy(MonthlyEvidenceResponse::workPointId,
                        LinkedHashMap::new, Collectors.toList()));

        // Only Anexa 1 movements: goods taken over from third parties are the art. 48 register's
        // business and never reach this sheet (HG 856/2002 art. 2 alin. (1)).
        Map<Key, List<WasteMovement>> movementsByPair = movements.stream()
                .filter(m -> m.getRegister() == WasteRegister.ANEXA_1)
                .collect(Collectors.groupingBy(
                        m -> new Key(m.getWorkPoint().getId(), m.getWasteCode().getId()),
                        LinkedHashMap::new, Collectors.toList()));

        List<AnnualDeclaration> declarations = new ArrayList<>();
        for (Map.Entry<UUID, List<MonthlyEvidenceResponse>> entry : byWorkPoint.entrySet()) {
            List<AnnualDeclaration.Row> rows = rows(entry.getValue(), movementsByPair);
            if (rows.isEmpty()) {
                continue;
            }
            declarations.add(declaration(company, year, entry.getValue().get(0).workPointName(), rows));
        }
        declarations.sort(Comparator.comparing(AnnualDeclaration::workPointName,
                String.CASE_INSENSITIVE_ORDER));
        return declarations;
    }

    private List<AnnualDeclaration.Row> rows(List<MonthlyEvidenceResponse> lines,
                                             Map<Key, List<WasteMovement>> movementsByPair) {
        Map<UUID, List<MonthlyEvidenceResponse>> byCode = lines.stream()
                .collect(Collectors.groupingBy(MonthlyEvidenceResponse::wasteCodeId,
                        LinkedHashMap::new, Collectors.toList()));

        List<AnnualDeclaration.Row> rows = new ArrayList<>();
        for (Map.Entry<UUID, List<MonthlyEvidenceResponse>> entry : byCode.entrySet()) {
            List<MonthlyEvidenceResponse> months = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(MonthlyEvidenceResponse::month))
                    .toList();
            MonthlyEvidenceResponse first = months.get(0);
            MonthlyEvidenceResponse last = months.get(months.size() - 1);

            BigDecimal generated = sum(months, MonthlyEvidenceResponse::totalGenerated);
            BigDecimal recovered = sum(months, MonthlyEvidenceResponse::totalRecovered);
            BigDecimal disposed = sum(months, MonthlyEvidenceResponse::totalDisposed);
            BigDecimal unclassified = sum(months, MonthlyEvidenceResponse::totalUnclassifiedOut);

            List<WasteMovement> yearly = movementsByPair
                    .getOrDefault(new Key(first.workPointId(), first.wasteCodeId()), List.of());

            rows.add(new AnnualDeclaration.Row(
                    first.wasteCode(),
                    first.wasteCodeName(),
                    first.hazardous(),
                    openingStock(first),
                    generated, recovered, disposed,
                    last.closingStock(),
                    through(yearly, WasteOperation.RECOVERED),
                    through(yearly, WasteOperation.DISPOSED),
                    unclassified));
        }
        rows.sort(Comparator.comparing(AnnualDeclaration.Row::wasteCode));
        return rows;
    }

    /**
     * What the year opened with: January's closing stock wound back through January's own
     * movements. The same arithmetic as the fişa's header, and deliberately so — the two documents
     * are read side by side and a client who finds different opening stocks on them stops trusting
     * both.
     */
    private BigDecimal openingStock(MonthlyEvidenceResponse january) {
        return january.closingStock()
                .subtract(january.totalGenerated())
                .add(january.totalRecovered())
                .add(january.totalDisposed())
                .add(january.totalUnclassifiedOut());
    }

    /**
     * The "valorificat prin:" / "eliminat prin:" column — the code and the operator, as the models
     * write it ("R3 - Hamburger Recycling Group GMBH"). A year with two recyclers prints both, for
     * the same reason the fişa lists distinct values in a month: the sheet has one row per code and
     * dropping one of the two would hide a handover that happened.
     *
     * <p>An operation carried out on site has no partner, so only the code prints.
     */
    private String through(List<WasteMovement> movements, WasteOperation operation) {
        return movements.stream()
                .filter(m -> m.getOperation() == operation)
                .map(m -> {
                    String code = m.getOperationCode() == null ? null : m.getOperationCode().name();
                    String partner = m.getPartner() == null ? null : m.getPartner().getName();
                    if (code == null && partner == null) {
                        return null;
                    }
                    if (code == null) {
                        return partner;
                    }
                    return partner == null ? code : code + " - " + partner;
                })
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .collect(Collectors.joining("; "));
    }

    private AnnualDeclaration declaration(Company company, int year, String workPointName,
                                          List<AnnualDeclaration.Row> rows) {
        return new AnnualDeclaration(
                company.getName(),
                blank(company.getAddress()),
                blank(company.getCui()),
                contactLine(company),
                environmentalAuth(company),
                blank(company.getCaenCode()),
                year,
                workPointName,
                blank(company.getContactName()),
                blank(company.getContactRole()),
                blank(company.getContactPhone()),
                blank(company.getContactEmail()),
                rows);
    }

    /** "Tel/fax/e-mail:" on the models — one line, whatever of the three we hold. */
    private String contactLine(Company company) {
        List<String> parts = new ArrayList<>();
        if (notBlank(company.getContactPhone())) {
            parts.add("Telefon " + company.getContactPhone().trim());
        }
        if (notBlank(company.getContactEmail())) {
            parts.add("E-mail " + company.getContactEmail().trim());
        }
        return String.join("  ·  ", parts);
    }

    /**
     * "Autorizatie de mediu/nr inregistrare/data/valabilitate". We hold the number and the expiry
     * date, which is the part that matters at a control; the models fill the rest with the
     * revision history, which we do not track.
     */
    private String environmentalAuth(Company company) {
        if (!notBlank(company.getEnvironmentalAuthNumber())) {
            return "";
        }
        String value = company.getEnvironmentalAuthNumber().trim();
        if (company.getEnvironmentalAuthExpiry() != null) {
            value += ", valabilă până la " + company.getEnvironmentalAuthExpiry().format(DATE);
        }
        return value;
    }

    private BigDecimal sum(List<MonthlyEvidenceResponse> months,
                           Function<MonthlyEvidenceResponse, BigDecimal> of) {
        return months.stream()
                .map(of)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String blank(String value) {
        return value == null ? "" : value.trim();
    }

    /** A row is one work point and one waste code. */
    private record Key(UUID workPointId, UUID wasteCodeId) {}
}
