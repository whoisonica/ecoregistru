package ro.ecoregistru.service.export;

import ro.ecoregistru.enums.PackagingMaterial;

import java.math.BigDecimal;
import java.util.List;

/**
 * Anexa 1 Ambalaje (Ordinul 794/2012), assembled and ready to print: the identification header,
 * tabelul 1 (what was put on the market — answered by the client) and tabelul 2 (what was handed
 * over — computed from the movements).
 *
 * <p>Reference: {@code documente oficiale/RAPORTARE AMBALAJE 2021_anexa 1_ HRR.xlsx}, the filled
 * copy, read rubric by rubric on 24.08.2026, and the blank template beside it. Both are in
 * <b>kilograms</b>, as the act prints at the head of every one of its five annexes.
 *
 * @param ambiguousCodes packaging codes whose material the European List does not settle
 *                       (15 01 04 metal, composites, mixed), so their quantity sits in "Altele"
 *                       and the form says which codes did that instead of hiding it
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
        List<String> ambiguousCodes,
        String preparedBy,
        String preparedByRole
) {

    /** One material row of tabelul 1. Nulls print as empty cells, never as zero. */
    public record MarketRow(
            PackagingMaterial material,
            BigDecimal salesPackaging,
            BigDecimal primaryTotal,
            BigDecimal primaryReusable,
            BigDecimal secondaryTotal,
            BigDecimal secondaryReusable,
            BigDecimal hazardousContent
    ) {

        /** Col. 2 — "Total (col. 3+5)", a sum and never a stored figure. */
        public BigDecimal packagedGoodsTotal() {
            if (primaryTotal == null && secondaryTotal == null) {
                return null;
            }
            return nz(primaryTotal).add(nz(secondaryTotal));
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
}
