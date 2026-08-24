package ro.ecoregistru;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.service.export.Anexa3FormGenerator;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which unit an Anexa 3 prints, and what happens to the figure when it is not the recorded one.
 *
 * <p>The sources disagree: HG 1061/2008 anexa 3 carries "tone" and "mc", two of the three filled
 * models agree with it — including the stamped one from a collector, where 76 kilograms are written
 * 0,076 — and the third prints KG. Question A3.4 is with the specialist; until it comes back the
 * company chooses, and an account that never chose keeps printing what it always did.
 *
 * <p>The one thing that must never happen is a figure that disagrees with the unit beside it on a
 * form leaving the site, so the conversion is pinned here rather than left to a PDF eyeball.
 */
class Anexa3UnitTest {

    @Test
    void withoutAChoiceTheMovementsOwnUnitIsPrinted() {
        assertThat(Anexa3FormGenerator.printedUnit(movement(Unit.KG, null))).isEqualTo(Unit.KG);
        assertThat(Anexa3FormGenerator.printedUnit(movement(Unit.TONS, null))).isEqualTo(Unit.TONS);
    }

    @Test
    void theCompanysChoiceWins() {
        assertThat(Anexa3FormGenerator.printedUnit(movement(Unit.KG, Unit.TONS))).isEqualTo(Unit.TONS);
        assertThat(Anexa3FormGenerator.printedUnit(movement(Unit.TONS, Unit.KG))).isEqualTo(Unit.KG);
    }

    /** 76 kg is 0,076 tone — exactly how the Hamburger form is filled in. */
    @Test
    void kilogramsBecomeTonnesExactly() {
        BigDecimal printed = Anexa3FormGenerator.converted(new BigDecimal("76"), Unit.KG, Unit.TONS);
        assertThat(printed).isEqualByComparingTo(new BigDecimal("0.076"));
    }

    @Test
    void tonnesBecomeKilogramsExactly() {
        BigDecimal printed = Anexa3FormGenerator.converted(new BigDecimal("1.02"), Unit.TONS, Unit.KG);
        assertThat(printed).isEqualByComparingTo(new BigDecimal("1020"));
    }

    /** No rounding, at any scale: the figure on paper is the figure that was recorded. */
    @Test
    void conversionNeverRounds() {
        BigDecimal printed = Anexa3FormGenerator.converted(new BigDecimal("1234.567"), Unit.KG, Unit.TONS);
        assertThat(printed).isEqualByComparingTo(new BigDecimal("1.234567"));
    }

    @Test
    void sameUnitIsLeftAlone() {
        BigDecimal quantity = new BigDecimal("450.000");
        assertThat(Anexa3FormGenerator.converted(quantity, Unit.KG, Unit.KG)).isSameAs(quantity);
    }

    /** A load the recipient weighs has no quantity yet; the rubric prints blank, converted or not. */
    @Test
    void aMissingQuantityStaysMissing() {
        assertThat(Anexa3FormGenerator.converted(null, Unit.KG, Unit.TONS)).isNull();
    }

    private WasteMovement movement(Unit recorded, Unit companyChoice) {
        return WasteMovement.builder()
                .unit(recorded)
                .company(Company.builder().anexa3Unit(companyChoice).build())
                .build();
    }
}
