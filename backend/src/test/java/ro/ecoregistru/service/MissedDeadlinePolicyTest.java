package ro.ecoregistru.service;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.DeadlineStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Regula implicită, pe calendar fix: 17.09.2026 desparte resturile vechi de termenele ratate cu adevărat. */
class MissedDeadlinePolicyTest {

    private final MissedDeadlinePolicy policy = new MissedDeadlinePolicy("2026-09-17");
    private final LocalDate today = LocalDate.of(2027, 3, 20);

    private static ReportingDeadline d(LocalDate due, DeadlineStatus status) {
        return ReportingDeadline.builder().dueDate(due).status(status).build();
    }

    @Test
    void aMissedFifteenthOfMarchStaysOnScreen() {
        ReportingDeadline march = d(LocalDate.of(2027, 3, 15), DeadlineStatus.UPCOMING);
        assertThat(policy.shown(march, today)).isTrue();
        assertThat(policy.missed(march, today)).isTrue();
    }

    @Test
    void leftoversOfTheOldWholeYearGenerationStayHidden() {
        ReportingDeadline old = d(LocalDate.of(2026, 5, 31), DeadlineStatus.UPCOMING);
        assertThat(policy.shown(old, today)).isFalse();
        assertThat(policy.missed(old, today)).isFalse();
    }

    @Test
    void theFirstDayOfTheRuleCounts() {
        ReportingDeadline first = d(LocalDate.of(2026, 9, 17), DeadlineStatus.UPCOMING);
        assertThat(policy.missed(first, today)).isTrue();
        assertThat(policy.missed(d(LocalDate.of(2026, 9, 16), DeadlineStatus.UPCOMING), today)).isFalse();
    }

    @Test
    void tickedAndUpcomingAreShownButNotMissed() {
        ReportingDeadline done = d(LocalDate.of(2026, 1, 25), DeadlineStatus.DONE);
        ReportingDeadline dueToday = d(today, DeadlineStatus.UPCOMING);
        assertThat(policy.shown(done, today)).isTrue();
        assertThat(policy.missed(done, today)).isFalse();
        assertThat(policy.shown(dueToday, today)).isTrue();
        assertThat(policy.missed(dueToday, today)).isFalse();
    }
}
