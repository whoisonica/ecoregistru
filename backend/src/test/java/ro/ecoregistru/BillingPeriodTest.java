package ro.ecoregistru;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.service.BillingCalculator;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** F2 — which period a day falls in; the daily run invoices that one if it has no invoice yet. */
class BillingPeriodTest {

    @Test
    void aPeriodTurnsOnTheSameDateOfTheNextMonth() {
        LocalDate start = LocalDate.of(2026, 10, 17);
        assertThat(BillingCalculator.periodOn(start, LocalDate.of(2026, 10, 17))).isZero();
        assertThat(BillingCalculator.periodOn(start, LocalDate.of(2026, 11, 16))).isZero();
        assertThat(BillingCalculator.periodOn(start, LocalDate.of(2026, 11, 17))).isEqualTo(1);
        assertThat(BillingCalculator.periodOn(start, LocalDate.of(2027, 10, 17))).isEqualTo(12);
    }

    /** Aceleași perioade ca în BillingCalculatorTest: 31.01–27.02, 28.02–30.03, apoi 31.03. */
    @Test
    void theEndOfAMonthFollowsTheStartDate() {
        LocalDate start = LocalDate.of(2027, 1, 31);
        assertThat(BillingCalculator.periodOn(start, LocalDate.of(2027, 2, 27))).isZero();
        assertThat(BillingCalculator.periodOn(start, LocalDate.of(2027, 2, 28))).isEqualTo(1);
        assertThat(BillingCalculator.periodOn(start, LocalDate.of(2027, 3, 30))).isEqualTo(1);
        assertThat(BillingCalculator.periodOn(start, LocalDate.of(2027, 3, 31))).isEqualTo(2);
    }

    @Test
    void beforeTheStartThereIsOnlyTheFirstPeriod() {
        assertThat(BillingCalculator.periodOn(LocalDate.of(2026, 10, 17), LocalDate.of(2026, 9, 1))).isZero();
    }
}
