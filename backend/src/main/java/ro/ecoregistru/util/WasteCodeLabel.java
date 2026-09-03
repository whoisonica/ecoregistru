package ro.ecoregistru.util;

/**
 * How a waste code is written on an official form: six digits, and a trailing asterisk when the
 * code is a hazardous one.
 *
 * <p><b>HG 856/2002, art. 4 alin. (3):</b> <em>"Deşeurile periculoase prevăzute în anexa nr. 2 sunt
 * marcate cu un asterisc (*)."</em> Art. 5 alin. (1) adds that a type of waste is defined by a
 * complete six-digit code. The header of the fişa points at exactly that codification — "Tipul de
 * deşeu … cod … (conform codificării din anexa nr. 2)" — so on a form filed with the authority the
 * asterisk is part of how the code is spelled, not decoration. 408 of the 842 codes carry one.
 *
 * <p><b>Why the stored code has no asterisk.</b> {@code waste_codes.csv} keeps the six digits and
 * the hazardous flag in separate fields, which is right for storage: it keeps the code sortable and
 * comparable, and stops two spellings of the same code from existing. The asterisk is put back at
 * the last possible moment — here — so there is one implementation of the rule and the generators
 * cannot drift apart.
 *
 * <p><b>Where it does NOT go: the denominations.</b> Anexa 2 writes cross-references inside names
 * clean. {@code 19 12 12} reads "alte deşeuri […] altele decât cele specificate la <b>19 12 11</b>"
 * even though {@code 19 12 11*} is hazardous, and {@code 20 01 36} points at "20 01 21, 20 01 23 şi
 * 20 01 35" the same way. The names in the seed come verbatim from EUR-Lex and already match the
 * act, so nothing rewrites them. (The filled sheets in the corpus do add the star there — four of
 * them write "la 19 12 11*" — but that is their embellishment, not the legal text.)
 *
 * <p>Added by the conformance audit of 02.09.2026, which found the asterisk missing from every
 * printed form. See {@code docs/surse-oficiale.md} §1.1.
 */
public final class WasteCodeLabel {

    /** The character the act uses; a plain ASCII asterisk, as in the annex. */
    private static final String HAZARDOUS_MARK = "*";

    private WasteCodeLabel() {
    }

    /**
     * The code as an official form spells it.
     *
     * @param code      the six digits as stored, e.g. {@code "13 02 08"}; {@code null} or blank
     *                  comes back untouched, because a missing code is a missing code and gains
     *                  nothing from a lone asterisk
     * @param hazardous whether the nomenclator marks this code as hazardous
     * @return {@code "13 02 08*"} when hazardous, {@code "13 02 08"} otherwise
     */
    public static String official(String code, boolean hazardous) {
        if (code == null || code.isBlank()) {
            return code;
        }
        // Idempotent on purpose: a code that already carries the mark is left alone, so calling
        // this twice — or on data that arrived pre-formatted — cannot produce "13 02 08**".
        if (!hazardous || code.endsWith(HAZARDOUS_MARK)) {
            return code;
        }
        return code + HAZARDOUS_MARK;
    }
}
