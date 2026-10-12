package ro.ecoregistru.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.MonthDay;

import static org.assertj.core.api.Assertions.assertThat;

/** The visa request day: 60 days before the anniversary of the initial authorization (Ordinul 1150/2020 art. 5 (4)). */
class VisaDueTest {

    @Test
    void anAuthorizationIssuedInJanuaryHasItsVisaDueInNovember() {
        // 15 January − 60 days = 16 November of the year before.
        assertThat(DeadlineService.computedVisaDue(LocalDate.of(2019, 1, 15))).contains(MonthDay.of(11, 16));
    }

    @Test
    void oneIssuedOn29FebruaryCountsFrom28February() {
        // 28 February − 60 days = 30 December.
        assertThat(DeadlineService.computedVisaDue(LocalDate.of(2016, 2, 29))).contains(MonthDay.of(12, 30));
    }

    @Test
    void withoutADateThereIsNoVisaDay() {
        assertThat(DeadlineService.computedVisaDue(null)).isEmpty();
    }
}
