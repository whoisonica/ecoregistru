package ro.ecoregistru;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static ro.ecoregistru.util.SiatdCalendar.due;
import static ro.ecoregistru.util.SiatdCalendar.isNonWorking;
import static ro.ecoregistru.util.SiatdCalendar.orthodoxEaster;
import static ro.ecoregistru.util.SiatdCalendar.reminder;

/**
 * The SIATD confirmation term counted the Civil Code way (art. 2.553–2.554): neither the first nor the last day
 * counts, and a term ending on a non-working day moves to the next working one. The examples are the ones written
 * in `surse-oficiale.md` §6.1; the holidays are Codul muncii art. 139, read on 28.09.2026.
 */
class SiatdCalendarTest {

    private static LocalDate d(int y, int m, int day) {
        return LocalDate.of(y, m, day);
    }

    @Test
    void theExamplesFromTheSources() {
        assertThat(due(d(2026, 10, 2), 5)).isEqualTo(d(2026, 10, 8));
        assertThat(due(d(2026, 10, 2), 3)).isEqualTo(d(2026, 10, 6));
        assertThat(due(d(2026, 10, 2), 15)).isEqualTo(d(2026, 10, 19));   // 18.10 e duminică
        assertThat(due(d(2026, 10, 4), 5)).isEqualTo(d(2026, 10, 12));    // 10.10 e sâmbătă
    }

    @Test
    void theReminderComesADayEarlier() {
        assertThat(reminder(d(2026, 10, 2), 5)).isEqualTo(d(2026, 10, 7));
    }

    @Test
    void orthodoxEasterAgainstKnownDates() {
        assertThat(orthodoxEaster(2026)).isEqualTo(d(2026, 4, 12));
        assertThat(orthodoxEaster(2027)).isEqualTo(d(2027, 5, 2));
        assertThat(orthodoxEaster(2028)).isEqualTo(d(2028, 4, 16));
        assertThat(orthodoxEaster(2029)).isEqualTo(d(2029, 4, 8));
        assertThat(orthodoxEaster(2030)).isEqualTo(d(2030, 4, 28));
    }

    @Test
    void movableHolidays() {
        assertThat(isNonWorking(d(2027, 4, 30))).isTrue();   // Vinerea Mare 2027
        assertThat(isNonWorking(d(2027, 5, 3))).isTrue();    // a doua zi de Paște
        assertThat(isNonWorking(d(2026, 6, 1))).isTrue();    // a doua zi de Rusalii (31.05) și 1 iunie
        assertThat(isNonWorking(d(2028, 6, 5))).isTrue();    // a doua zi de Rusalii 2028 (Paște 16.04 + 50)
        assertThat(isNonWorking(d(2026, 4, 9))).isFalse();   // joia dinaintea Vinerii Mari
    }

    @Test
    void everyFixedHoliday() {
        int[][] fixed = {{1, 1}, {1, 2}, {1, 6}, {1, 7}, {1, 24}, {5, 1}, {6, 1}, {8, 15}, {11, 30}, {12, 1},
                {12, 25}, {12, 26}};
        for (int[] md : fixed) {
            // three consecutive years, so every date lands on a weekday at least once — a weekend can't hide a gap
            for (int year = 2029; year <= 2031; year++) {
                assertThat(isNonWorking(d(year, md[0], md[1]))).as("%d.%d.%d", md[1], md[0], year).isTrue();
            }
        }
        assertThat(isNonWorking(d(2026, 10, 6))).isFalse();
        assertThat(isNonWorking(d(2029, 1, 3))).isFalse();
    }

    @Test
    void weekends() {
        assertThat(isNonWorking(d(2026, 10, 10))).isTrue();
        assertThat(isNonWorking(d(2026, 10, 11))).isTrue();
    }

    @Test
    void aTermLandingOnGoodFriday2027MovesToTheTuesdayAfterEaster() {
        // 24.04 + 6 = vineri 30.04 (Vinerea Mare), sâmbătă 1 Mai, Paște 02.05, luni 03.05 → marți 04.05
        assertThat(due(d(2027, 4, 24), 5)).isEqualTo(d(2027, 5, 4));
    }
}
