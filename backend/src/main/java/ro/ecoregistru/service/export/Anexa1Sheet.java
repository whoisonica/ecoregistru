package ro.ecoregistru.service.export;

import java.math.BigDecimal;
import java.util.List;

/**
 * One sheet of the Anexa 1 form (HG 856/2002) — a single waste code, at a single work point, for
 * one year. This is the shape the filled workbooks received from the specialist have: an
 * identification header, then four chapters of exactly twelve rows plus a TOTAL AN line.
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
     * <p>Where several movements in the same month disagree — two handovers under different R
     * codes, or to different operators — the distinct values are listed together ("R3, R13").
     * The form has exactly twelve rows, so they have to share one; picking one and dropping the
     * rest would put a figure on an official document that nobody recorded.
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
            // cap. 3 — valorificarea
            String recoveryOperations,
            String recoveryOperators,
            // cap. 4 — eliminarea
            String disposalOperations,
            String disposalOperators
    ) {}
}
