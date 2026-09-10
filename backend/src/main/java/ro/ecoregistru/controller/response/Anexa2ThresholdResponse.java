package ro.ecoregistru.controller.response;

import java.math.BigDecimal;

/**
 * What the "&lt; 1t/an" tick of Anexa 2 la HG 1061/2008 is proposed from, with the figures that
 * produced the proposal shown next to it.
 *
 * <p>The screen needs all of it, and that is the point of the record. A tick that appeared on its
 * own would be a decision taken silently on a word the act never defines; a tick with
 * <em>"0,84 t generate în 2026 pe 08 01 17*"</em> beside it is information the person signing the
 * form can act on.
 *
 * @param year         the calendar year the totals cover — our choice, not the act's, and named
 *                     for that reason (see {@code Anexa2ThresholdCalculator})
 * @param wasteCode    the code as an official form spells it, asterisk included
 * @param generatedTons what the evidence says was generated on this code in that year
 * @param groupCode    the four-digit group the code belongs to, e.g. {@code "08 01"}
 * @param groupTons    the same total across every code in that group — the wider reading of
 *                     "categorie", shown because it is the one that could make the proposal wrong
 * @param belowOneTon  what the per-code total proposes
 * @param chosen       what the client answered on this movement; {@code null} = not answered
 * @param effectiveBelowOneTon what the form will actually print: the answer, or the proposal
 * @param groupWarning true when the per-code total is under a tonne but the group total is not —
 *                     the only case where the undefined word can change the outcome
 */
public record Anexa2ThresholdResponse(
        int year,
        String wasteCode,
        BigDecimal generatedTons,
        String groupCode,
        BigDecimal groupTons,
        boolean belowOneTon,
        Boolean chosen,
        boolean effectiveBelowOneTon,
        boolean groupWarning
) {}
