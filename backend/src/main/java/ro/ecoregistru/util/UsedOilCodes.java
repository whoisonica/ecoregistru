package ro.ecoregistru.util;

import java.util.List;
import java.util.Set;

/**
 * Which waste codes make a company a "producător sau deţinător de uleiuri uzate" under
 * OUG 92/2021 art. 31 — the one signal two different obligations both read.
 *
 * <p><b>Why this is a class and not a literal in two places.</b> Art. 31 alin. (3) (hand the
 * <em>entire</em> quantity only to authorised operators) and art. 49 alin. (9) (report to APM by
 * 30 April) are addressed to the same person, and the application decides who that is from the
 * movements it already holds rather than by asking. Two copies of the list would drift, and a
 * drift here is not cosmetic: one of the two obligations carries 40.000–60.000 lei
 * (art. 62 alin. (1) lit. a) and the other 5.000–10.000 (lit. e).
 *
 * <p><b>The act gives no list of codes, so this is our reading, written down.</b> OUG 92/2021 does
 * not enumerate the codes of an "ulei uzat"; HG 235/2007, which once governed them, was repealed
 * by art. 71 alin. (1) lit. b) and everything now sits in art. 31–32, which speak of oils, not of
 * codes. What the nomenclator gives is chapter 13 — "deşeuri uleioase şi deşeuri de combustibili
 * lichizi" — plus the used machining oils that Decizia 2014/955/UE files under 12 01.
 *
 * <p><b>What is in, and what is deliberately out.</b> In: the whole of 13 01 (hydraulic),
 * 13 02 (engine, gear and lubricating — the client-typical {@code 13 02 08*}), 13 03 (insulating
 * and heat-transmission), 13 04 (bilge), 13 08 (other oily wastes), the single code
 * {@code 13 05 06} ("ulei de la separatoarele ulei/apă"), and 12 01 06–12 01 10 (used lubricating
 * oils, emulsions and solutions from metal machining). Out: <b>13 07</b>, which is liquid
 * <em>fuel</em> — petrol, diesel, fuel oil are not oils that have become unfit for their use — and
 * the rest of <b>13 05</b>, which is what a separator retains rather than the oil itself: sands,
 * sludges, oily water, mixtures.
 *
 * <p><b>Which way the uncertainty is allowed to fall.</b> Both readers of this list assert
 * something to the client, and the rule from {@code ReportType} holds: an alert is a statement,
 * made on a positive signal and not on silence. So the list stays at the codes that are oils by
 * their own denomination. A separator sludge on {@code 13 05 02} does not, on its own, make a
 * company an oil holder — and if it also holds oil, it will have an oil code too.
 *
 * <p>Written 11.09.2026, from {@code docs/surse-oficiale.md} §2.5. When it changes, it changes
 * here, and both obligations follow.
 */
public final class UsedOilCodes {

    /** Four-digit groups whose every code is a waste oil. */
    private static final Set<String> GROUPS = Set.of("13 01", "13 02", "13 03", "13 04", "13 08");

    /**
     * Six-digit codes taken one by one, because their group holds other things too: 13 05 is the
     * oil/water separator, of which only 13 05 06 is the oil; 12 01 is metal machining, of which
     * 06–10 are the used oils and emulsions.
     */
    private static final Set<String> CODES = Set.of(
            "13 05 06",
            "12 01 06", "12 01 07", "12 01 08", "12 01 09", "12 01 10");

    private UsedOilCodes() {
    }

    /** Whether a single six-digit code, as stored (no asterisk), is a used oil. */
    public static boolean isUsedOil(String code) {
        if (code == null || code.length() < 5) {
            return false;
        }
        return GROUPS.contains(code.substring(0, 5)) || CODES.contains(code);
    }

    /**
     * The used-oil codes among those given, in the order received and without repetition — so a
     * note can name what it found instead of asserting a category the client never sees.
     */
    public static List<String> among(List<String> codes) {
        return codes == null ? List.of() : codes.stream()
                .filter(UsedOilCodes::isUsedOil)
                .distinct()
                .toList();
    }
}
