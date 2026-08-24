package ro.ecoregistru.service.export;

import java.math.BigDecimal;
import java.util.List;

/**
 * One sheet of the waste-management record (HG 856/2002, anexa 1) — a single waste code, at a
 * single work point, for one year. This is the shape the filled workbooks received from the
 * specialist have: an identification header, then four chapters of twelve months plus a TOTAL AN
 * line.
 *
 * <p>Reference: {@code documente oficiale/deseuri generate_Cluj_2025_Iuhos Lorena.pdf} and the ten
 * filled workbooks alongside it.
 *
 * @param openingStock the stock the year starts with — the header's "Stoc/kg"
 */
public record Anexa1Sheet(
        String companyName,
        int year,
        String workPointName,
        String wasteCodeName,
        String wasteCode,
        String physicalState,
        BigDecimal openingStock,
        List<Anexa1MonthRow> rows
) {

    /**
     * One month, across all four chapters at once — which is how the form reads across a row even
     * though it prints as four separate tables.
     *
     * <p>Chapters 1 and 2 have exactly one line per month: chapter 1 is the stock ledger, whose
     * running balance has to be readable month by month, and chapter 2's "Stocare: Cant." is the
     * quantity the month generated. Chapters 3 and 4 do not: see {@link Handover}.
     */
    public record Anexa1MonthRow(
            int month,
            // cap. 1 — generarea
            BigDecimal generated,
            BigDecimal recovered,
            BigDecimal disposed,
            BigDecimal closingStock,
            // cap. 2 — stocarea provizorie, tratarea şi transportul
            String section,
            BigDecimal storedQuantity,
            String storageType,
            BigDecimal treatedQuantity,
            String treatmentMethod,
            String purpose,
            String transportMeans,
            String destination,
            // cap. 3 — valorificarea (one entry per operation + operator)
            List<Handover> recoveries,
            // cap. 4 — eliminarea (idem)
            List<Handover> disposals
    ) {}

    /**
     * One line of chapter 3 or chapter 4: a quantity, the R/D operation it went out under, and the
     * economic operator who performs that operation.
     *
     * <p><b>Why this is a list and not three joined strings.</b> Until 24.08.2026 a month with two
     * different handovers printed its distinct values inside one cell ("R3, R12", both operator
     * names) because the models have twelve rows and we would not drop a figure nobody could then
     * account for. Asked directly (question B), the specialist answered: <em>"trebuie un rând nou
     * pentru fiecare chestie nouă pentru luna respectivă"</em> — a new row per distinct handover.
     * The corpus had no precedent for either form: across 345 filled cells no month ever carries
     * two values, so the case simply never came up at that client.
     *
     * <p>The same rule appears in writing on the other annex: Ordinul 794/2012, anexa 1, tabelul 2,
     * nota 1 asks for "câte o rubrică distinctă pentru fiecare dintre operatorii care au preluat".
     *
     * @param quantity in kg, or {@code null} when every movement behind this line is still waiting
     *                 for the recipient's weighbridge — the cell then prints empty rather than
     *                 claiming a zero
     */
    public record Handover(BigDecimal quantity, String operation, String operator) {}
}
