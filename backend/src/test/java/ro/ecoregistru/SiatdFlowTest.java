package ro.ecoregistru;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.util.SiatdFlow;

import static org.assertj.core.api.Assertions.assertThat;
import static ro.ecoregistru.enums.SiatdModule.BATTERY;
import static ro.ecoregistru.enums.SiatdModule.MUNICIPAL;
import static ro.ecoregistru.enums.SiatdModule.PACKAGING;
import static ro.ecoregistru.enums.SiatdModule.TYRE;
import static ro.ecoregistru.enums.SiatdModule.WEEE;

/**
 * Which SIATD module a reception line falls under, by its LER code. The module decides the confirmation
 * term (3, 5 or 15 days), so a code sorted into the wrong one is a deadline shown wrong — and a code
 * sorted into none when it belongs to one is a fine nobody saw coming.
 */
class SiatdFlowTest {

    @Test
    void packagingIsTheWholeOf150101() {
        assertThat(SiatdFlow.of("15 01 07")).contains(PACKAGING);
        assertThat(SiatdFlow.of("15 01 10*")).contains(PACKAGING);
    }

    @Test
    void endOfLifeTyres() {
        assertThat(SiatdFlow.of("16 01 03")).contains(TYRE);
    }

    @Test
    void weeeFromChapter16AndFromMunicipal() {
        assertThat(SiatdFlow.of("16 02 14")).contains(WEEE);
        assertThat(SiatdFlow.of("20 01 35*")).contains(WEEE);
        assertThat(SiatdFlow.of("20 01 21*")).contains(WEEE);
        assertThat(SiatdFlow.of("20 01 36")).contains(WEEE);
    }

    @Test
    void portableBatteriesButNotCarBatteries() {
        assertThat(SiatdFlow.of("16 06 04")).contains(BATTERY);
        assertThat(SiatdFlow.of("20 01 33*")).contains(BATTERY);
        assertThat(SiatdFlow.of("16 06 01*")).isEmpty();   // acumulatori auto, nu portabili
    }

    @Test
    void theRestOfChapter20IsMunicipal() {
        assertThat(SiatdFlow.of("20 01 01")).contains(MUNICIPAL);
        assertThat(SiatdFlow.of("20 03 01")).contains(MUNICIPAL);
        assertThat(SiatdFlow.of(" 20 01 40 ")).contains(MUNICIPAL);
    }

    @Test
    void codesOutsideSiatdHaveNoModule() {
        assertThat(SiatdFlow.of("17 04 05")).isEmpty();
        assertThat(SiatdFlow.of("19 12 01")).isEmpty();
        assertThat(SiatdFlow.of(null)).isEmpty();
    }
}
