package ro.ecoregistru.service.export;

import ro.ecoregistru.enums.PackagingCategory;
import ro.ecoregistru.enums.PackagingMaterial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Anexa 1 Ambalaje (Ordinul 794/2012), assembled and ready to print: the identification header,
 * tabelul 1 (what was put on the national market) and tabelul 2 (what was handed over).
 *
 * <p>Reference: {@code documente oficiale/RAPORTARE AMBALAJE 2021_anexa 1_ HRR.xlsx}, the filled
 * copy, read cell by cell, and the blank template beside it. Two sheets, {@code Tabelul nr. 1} and
 * {@code Tabelul nr. 2}, both in <b>kilograms</b>, as the act prints at the head of every one of
 * its five annexes.
 *
 * <p><b>Both tables now come from the movements.</b> Until 25.08.2026 tabelul 1 was a grid the
 * client typed, on the reasoning that packaging put on the market is not waste and cannot be
 * derived. What that missed is the flow: the kilograms pass through a movement on a
 * {@code 15 01 xx} code anyway — that is how they leave the stock and reach the waste record — so
 * the only thing missing was the packaging's material and kind, which the movement now carries.
 *
 * @param unclassified movements that could not be placed on a row of tabelul 1 because nobody said
 *                     what the packaging was. They are carried here, not hidden and not guessed, so
 *                     the screen can ask and the printed form can say how many kilograms are still
 *                     out of the table.
 */
public record PackagingDeclaration(
        String companyName,
        String county,
        String address,
        String contact,
        String caenCode,
        String cui,
        int year,
        List<MarketRow> marketRows,
        List<HandoverRow> handoverRows,
        List<UnclassifiedRow> unclassified,
        String preparedBy,
        String preparedByRole
) {

    /**
     * One material row of tabelul 1, in kilograms. Nulls print as empty cells, never as zero: on a
     * form filed with an authority, "none" and "not answered" are different statements.
     *
     * @param overridden true when the client replaced the computed figures with their own for this
     *                   material — the market figure is legally about goods sold, so a company that
     *                   knows it differs from what the movements show may say so, and the form then
     *                   prints what they said
     */
    public record MarketRow(
            PackagingMaterial material,
            BigDecimal salesPackaging,
            BigDecimal primaryTotal,
            BigDecimal primaryReusable,
            BigDecimal secondaryTotal,
            BigDecimal secondaryReusable,
            BigDecimal hazardousContent,
            boolean overridden
    ) {

        /** Col. 2 — "Total (col. 3+5)", a sum and never a stored figure. */
        public BigDecimal packagedGoodsTotal() {
            if (primaryTotal == null && secondaryTotal == null) {
                return null;
            }
            return nz(primaryTotal).add(nz(secondaryTotal));
        }

        /** True when the client has said nothing and the movements say nothing either. */
        public boolean isEmpty() {
            return salesPackaging == null && primaryTotal == null && primaryReusable == null
                    && secondaryTotal == null && secondaryReusable == null
                    && hazardousContent == null;
        }

        private static BigDecimal nz(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    /**
     * One line of tabelul 2: a material, the quantity handed over, the operator who took it, and
     * the R/D operation they perform on it.
     *
     * <p>One line per operator, which the annex asks for in writing — nota 1: "Se completează câte
     * o rubrică distinctă pentru fiecare dintre operatorii care au preluat deşeurile de ambalaje
     * din materialul respectiv." The same rule the specialist gave for the waste-management record
     * on the same day (answer B).
     */
    public record HandoverRow(
            PackagingMaterial material,
            BigDecimal quantity,
            String operatorName,
            String operatorAddress,
            String operatorCui,
            String operation
    ) {}

    /**
     * A packaging movement the tables could not use, and why.
     *
     * @param missingMaterial the waste code does not settle the material and nobody chose one —
     *                        {@code 15 01 04} is aluminium and steel at once
     * @param missingCategory nobody said whether it was sales, primary, or secondary and transport
     *                        packaging, so there is no column for the quantity
     */
    public record UnclassifiedRow(
            java.util.UUID movementId,
            LocalDate date,
            String wasteCode,
            BigDecimal quantity,
            PackagingMaterial material,
            PackagingCategory category,
            boolean missingMaterial,
            boolean missingCategory
    ) {}
}
