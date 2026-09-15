package ro.ecoregistru.service;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.entity.Subscription;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * §9.3 — oprirea cu preaviz de o lună: se facturează perioada în care cade ziua de peste o lună, și nimic după
 * ea. Perioadele pornite pe 17: 17.10–16.11, 17.11–16.12.
 */
class SubscriptionEndsOnTest {

    static final Subscription S = Subscription.builder().startedAt(LocalDate.of(2026, 10, 17)).build();

    @Test
    void theNoticeMonthDecidesTheLastPeriod() {
        assertThat(SubscriptionService.endsOnAfterNotice(S, LocalDate.of(2026, 10, 20))).isEqualTo(LocalDate.of(2026, 12, 16));
        assertThat(SubscriptionService.endsOnAfterNotice(S, LocalDate.of(2026, 10, 17))).isEqualTo(LocalDate.of(2026, 12, 16));
        assertThat(SubscriptionService.endsOnAfterNotice(S, LocalDate.of(2026, 10, 16))).isEqualTo(LocalDate.of(2026, 11, 16));
    }

    /** Oprit înainte de start: se plătește numai prima perioadă (un abonament greșit se șterge, nu se oprește). */
    @Test
    void stoppedBeforeTheStartOnlyTheFirstPeriodIsBilled() {
        assertThat(SubscriptionService.endsOnAfterNotice(S, LocalDate.of(2026, 9, 1))).isEqualTo(LocalDate.of(2026, 11, 16));
    }
}
