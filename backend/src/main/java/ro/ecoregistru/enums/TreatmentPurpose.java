package ro.ecoregistru.enums;

import lombok.Getter;

/**
 * "Scopul" — the letter printed in HG 856/2002 anexa nr. 1, cap. 2, nota 3.
 *
 * <p>The note itself offers two letters:
 *
 * <pre>
 * 3) Scopul tratării:
 *    V - pentru valorificare           E - în vederea eliminării
 * </pre>
 *
 * <p><b>Both letters are written</b> (proprietarul, 16.09.2026): V next to a recovery, E next to a
 * disposal, read from what the movement says happened to the waste. Until then only V was printed,
 * because the specialist's filled workbooks left the cell blank on disposal sheets; the owner asked
 * for the note's own two letters instead, so a filed sheet never shows an empty "Scopul".
 *
 * <p>One of the five closed nomenclators of cap. 2; the other four arrive with Etapa 3.
 */
@Getter
public enum TreatmentPurpose {

    V("pentru valorificare"),
    E("în vederea eliminării");

    /** The wording of nota 3, verbatim. */
    private final String officialLabel;

    TreatmentPurpose(String officialLabel) {
        this.officialLabel = officialLabel;
    }
}
