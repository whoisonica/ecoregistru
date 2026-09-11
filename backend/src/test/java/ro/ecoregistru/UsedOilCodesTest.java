package ro.ecoregistru;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.util.UsedOilCodes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The list that decides who is a "producător sau deţinător de uleiuri uzate" — and, through that,
 * who is told about art. 31 alin. (3) in the dossier and who gets the 30 April deadline.
 *
 * <p>The act names no codes, so the boundary is ours (see {@link UsedOilCodes} for the reading).
 * These tests pin the two edges that are a judgement rather than a lookup: <b>13 07</b> is liquid
 * fuel, not a used oil, and of <b>13 05</b> only the oil itself counts, not what the separator
 * retains around it. Both are the kind of line that a later reader would otherwise widen by
 * accident, turning a statement to the client into a guess.
 */
class UsedOilCodesTest {

    @Test
    void wholeGroupsOfChapter13AreUsedOils() {
        assertThat(UsedOilCodes.isUsedOil("13 01 10")).isTrue();  // uleiuri hidraulice
        assertThat(UsedOilCodes.isUsedOil("13 02 08")).isTrue();  // clientul-tip: ulei de motor
        assertThat(UsedOilCodes.isUsedOil("13 03 07")).isTrue();  // uleiuri izolante
        assertThat(UsedOilCodes.isUsedOil("13 04 03")).isTrue();  // uleiuri de santină
        assertThat(UsedOilCodes.isUsedOil("13 08 02")).isTrue();  // alte emulsii
    }

    @Test
    void machiningOilsOf1201AreIn_butTheRestOfThatGroupIsNot() {
        assertThat(UsedOilCodes.isUsedOil("12 01 06")).isTrue();
        assertThat(UsedOilCodes.isUsedOil("12 01 10")).isTrue();
        // 12 01 is metal machining as a whole: filings, dust and sludges are not oils.
        assertThat(UsedOilCodes.isUsedOil("12 01 01")).isFalse();
        assertThat(UsedOilCodes.isUsedOil("12 01 05")).isFalse();
    }

    /** Petrol, diesel and fuel oil are fuels that were never in service as oils. */
    @Test
    void liquidFuelsOf1307AreNotUsedOils() {
        assertThat(UsedOilCodes.isUsedOil("13 07 01")).isFalse();
        assertThat(UsedOilCodes.isUsedOil("13 07 02")).isFalse();
        assertThat(UsedOilCodes.isUsedOil("13 07 03")).isFalse();
    }

    /** Of the oil/water separator, only 13 05 06 is the oil; the rest is what it retained. */
    @Test
    void onlyTheOilOfTheSeparatorCounts() {
        assertThat(UsedOilCodes.isUsedOil("13 05 06")).isTrue();
        assertThat(UsedOilCodes.isUsedOil("13 05 01")).isFalse();  // solide din paturile de nisip
        assertThat(UsedOilCodes.isUsedOil("13 05 02")).isFalse();  // nămoluri
        assertThat(UsedOilCodes.isUsedOil("13 05 07")).isFalse();  // ape uleioase
    }

    @Test
    void unrelatedCodesAndMalformedInputAreNotUsedOils() {
        assertThat(UsedOilCodes.isUsedOil("20 01 01")).isFalse();
        assertThat(UsedOilCodes.isUsedOil("16 06 01")).isFalse();
        assertThat(UsedOilCodes.isUsedOil(null)).isFalse();
        assertThat(UsedOilCodes.isUsedOil("")).isFalse();
        assertThat(UsedOilCodes.isUsedOil("13")).isFalse();
    }

    /** The dossier names what it found, so the filter keeps order and drops repeats. */
    @Test
    void amongKeepsOrderAndDropsRepeats() {
        assertThat(UsedOilCodes.among(List.of(
                "20 01 01", "13 02 08", "13 07 02", "13 02 08", "12 01 07")))
                .containsExactly("13 02 08", "12 01 07");
        assertThat(UsedOilCodes.among(List.of("20 01 01"))).isEmpty();
        assertThat(UsedOilCodes.among(null)).isEmpty();
    }
}
