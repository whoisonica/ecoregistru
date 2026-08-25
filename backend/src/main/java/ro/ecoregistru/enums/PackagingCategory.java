package ro.ecoregistru.enums;

import lombok.Getter;

/**
 * The kind of packaging a quantity was, for tabelul 1 of Anexa 1 Ambalaje (Ordinul 794/2012).
 *
 * <p>The three members are the three quantity groups the form's header splits into — read off
 * {@code documente oficiale/RAPORTARE AMBALAJE _anexa 1.xlsx}, row 16 to row 19, where the columns
 * are numbered 1 to 7:
 *
 * <ul>
 *   <li><b>col. 1</b> "Ambalaje de desfacere fabricate/importate" — {@link #SALES};</li>
 *   <li><b>col. 3</b> "Ambalaje primare", of "ambalaje folosite la ambalarea produselor introduse
 *       pe piaţa naţională" — {@link #PRIMARY};</li>
 *   <li><b>col. 5</b> "Ambalaje secundare şi de transport", same group — {@link #SECONDARY}.</li>
 * </ul>
 *
 * <p>Col. 2 is "Total (col. 3+5)" and is a sum, so it has no member. Cols. 4 and 6 ("din care:
 * ambalaj reutilizabil") and col. 7 ("cu conţinut periculos") are flags on the movement, not
 * categories: the same kilogram is both a primary packaging and a reusable one.
 *
 * <p><b>Why it lives on the movement.</b> The kilograms already travel through a movement on a
 * {@code 15 01 xx} code — that is how they leave the stock and reach the waste record. The only
 * thing the movement could not say was which of these three the packaging was, so that is what was
 * added, rather than a second register that would hold the same quantity twice.
 */
@Getter
public enum PackagingCategory {

    /** Col. 1 — sales packaging manufactured or imported, destined for the national market. */
    SALES("Ambalaje de desfacere fabricate/importate"),

    /** Col. 3 — primary packaging, the one in direct contact with the product. */
    PRIMARY("Ambalaje primare"),

    /**
     * Col. 5 — secondary and transport packaging: the box, the pallet, the stretch film. The one
     * filled row of the model we hold (Oţel, 5192 kg) sits here, which is why the form proposes it.
     */
    SECONDARY("Ambalaje secundare şi de transport");

    private final String officialLabel;

    PackagingCategory(String officialLabel) {
        this.officialLabel = officialLabel;
    }
}
