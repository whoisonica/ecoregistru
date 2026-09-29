package ro.ecoregistru.service;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.enums.DeadlineRecurrence;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DeadlineRecurrenceTest {

    @Test
    void theLastDayOfTheMonthStaysTheLastDay() {
        LocalDate jan31 = LocalDate.of(2027, 1, 31);
        LocalDate feb = DeadlineService.nextOccurrence(jan31, DeadlineRecurrence.MONTHLY);
        assertThat(feb).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(DeadlineService.nextOccurrence(feb, DeadlineRecurrence.MONTHLY)).isEqualTo(LocalDate.of(2027, 3, 31));
    }

    @Test
    void anOrdinaryDayKeepsItsDay() {
        LocalDate may15 = LocalDate.of(2027, 5, 15);
        assertThat(DeadlineService.nextOccurrence(may15, DeadlineRecurrence.QUARTERLY)).isEqualTo(LocalDate.of(2027, 8, 15));
        assertThat(DeadlineService.nextOccurrence(may15, DeadlineRecurrence.SEMIANNUAL)).isEqualTo(LocalDate.of(2027, 11, 15));
        assertThat(DeadlineService.nextOccurrence(may15, DeadlineRecurrence.ANNUAL)).isEqualTo(LocalDate.of(2028, 5, 15));
    }
}
