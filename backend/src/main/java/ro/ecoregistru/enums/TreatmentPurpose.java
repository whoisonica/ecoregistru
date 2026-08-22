package ro.ecoregistru.enums;

import lombok.Getter;

/**
 * "Scopul" — the closed two-value nomenclator printed on HG 856/2002 anexa nr. 1, cap. 2, nota 3:
 *
 * <pre>
 * 3) Scopul tratării:
 *    V - pentru valorificare           E - în vederea eliminării
 * </pre>
 *
 * The letters are the abbreviations the form itself uses, so they are the enum names.
 *
 * <p>Cap. 1 of the same annex has no "handed over" column — waste handed to an authorised operator
 * is reported under "valorificată" or "eliminată final", with the operator named in cap. 3 / cap. 4
 * (docs/surse-oficiale.md §1.2). This is the field that decides which of the two columns it feeds.
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
