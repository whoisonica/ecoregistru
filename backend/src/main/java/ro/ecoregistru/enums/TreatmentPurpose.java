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
 * <p><b>Only V is written.</b> Practice has dropped the E, and the ten filled Anexa 1 workbooks
 * received from the specialist say so almost unanimously: across Cluj, Timişoara, Bragadiru and
 * Oradea, every recovery sheet carries "V" on all twelve rows, while every disposal sheet
 * (20 03 01, 19 12 12) carries a dash. "E" appears exactly once in the whole corpus — Cluj 2022,
 * code 19 12 12 — and the same client's 2023 and 2024 sheets replaced it with a dash. What
 * identifies a disposal is its D code, in cap. 4, next to the operator who performs it; the letter
 * adds nothing the D code does not already say.
 *
 * <p>So this nomenclator has one member, and {@link WasteOperationCode#treatmentPurpose()} returns
 * null for the D family: the cell stays empty, exactly as it does on the filled forms.
 *
 * <p>One of the five closed nomenclators of cap. 2; the other four arrive with Etapa 3.
 */
@Getter
public enum TreatmentPurpose {

    V("pentru valorificare");

    /** The wording of nota 3, verbatim. */
    private final String officialLabel;

    TreatmentPurpose(String officialLabel) {
        this.officialLabel = officialLabel;
    }
}
