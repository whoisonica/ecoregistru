package ro.ecoregistru.service.export;

import java.math.BigDecimal;
import java.util.List;

/**
 * The annual declaration — the "centralizator" sheet, one page per work point and per year, with a
 * single line for each waste code: what the year opened with, what was generated, recovered and
 * disposed of, what is left, and through whom.
 *
 * <p>It is the summary that sits in front of the twelve-row Anexa 1 sheets ({@link Anexa1Sheet}),
 * not a substitute for them: the fişa proves the month-by-month movement, this page is what an
 * inspector or the APM reads first.
 *
 * <p><b>Reference:</b> the {@code raportare deseuri generate} sheet, present in nine of the filled
 * workbooks in {@code documente oficiale/} plus the blank template. The corpus carries two layouts
 * of it — a full identification header (Cluj and Timişoara, six files, and the blank template) and
 * a short one headed "CENTRALIZATOR" (Bragadiru, three files). We print the full one: it is the
 * layout of the blank template the specialist sent as the model to fill, and it is the only one
 * that identifies the company well enough to stand on its own once detached from the workbook.
 *
 * <p>Quantities are in kilograms, as the header of every model declares ("Unitatea de masura: kg").
 *
 * @param environmentalAuth the authorization line as it should print, already assembled; blank when
 *                          the account has no authorization recorded
 * @param caenCode          may be blank — see {@code V15__annual_declaration_header.sql}: the rubric
 *                          prints empty rather than guessed
 */
public record AnnualDeclaration(
        String companyName,
        String companyAddress,
        String cui,
        String contactLine,
        String environmentalAuth,
        String caenCode,
        int year,
        String workPointName,
        String preparedBy,
        String preparedByRole,
        String preparedByPhone,
        String preparedByEmail,
        List<Row> rows
) {

    /**
     * One waste code over a whole year. The identity the sheet asserts across the row is the
     * engine's own: {@code stoc final = stoc iniţial + generat − valorificat − eliminat}.
     *
     * @param unclassifiedOut the quantity that left the site in the year without an R/D code. The
     *                        models have no column for it — nobody who fills one by hand has such
     *                        a quantity, because they write the code as they write the line. Ours
     *                        can, on rows recorded before the code became mandatory, and then the
     *                        row above does not balance. It is carried here so the sheet can say
     *                        so in a footnote instead of printing a line that silently doesn't add
     *                        up. Zero on every well-formed row.
     * @param recoveredThrough distinct "R3 - Operator" values for the year, joined; blank when
     *                         nothing was recovered
     * @param hazardous        whether the nomenclator marks the code as hazardous, so the code
     *                         column can print the trailing asterisk HG 856/2002 art. 4 alin. (3)
     *                         requires. Not baked into {@code wasteCode}: the rows are sorted by it
     *                         and the stored code has no asterisk.
     */
    public record Row(
            String wasteCode,
            String wasteCodeName,
            boolean hazardous,
            BigDecimal openingStock,
            BigDecimal generated,
            BigDecimal recovered,
            BigDecimal disposed,
            BigDecimal closingStock,
            String recoveredThrough,
            String disposedThrough,
            BigDecimal unclassifiedOut
    ) {
        /** Whether this row carries a quantity that left the site with no operation code. */
        public boolean hasUnclassifiedOut() {
            return unclassifiedOut != null && unclassifiedOut.signum() > 0;
        }
    }

    /** Whether any row needs the "ieşiri fără cod R/D" footnote. */
    public boolean hasUnclassifiedOut() {
        return rows.stream().anyMatch(Row::hasUnclassifiedOut);
    }
}
