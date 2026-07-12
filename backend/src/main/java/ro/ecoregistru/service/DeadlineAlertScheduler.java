package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.DeadlineStatus;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.ReportingDeadlineRepository;
import ro.ecoregistru.service.notification.NotificationService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Sends reminder emails before reporting deadlines (FAZA TERMENE, T2). Runs daily as a system
 * job across all tenants (no TenantContext). Two reminders per deadline, de-duplicated via the
 * warned7Days / warned1Day flags on the deadline:
 *   - an early reminder while 2–7 days remain,
 *   - a final reminder on the last day or two (due today / tomorrow).
 *
 * A flag is set only after a successful send, so a delivery failure is retried the next day
 * rather than silently swallowed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeadlineAlertScheduler {

    private static final int EARLY_WINDOW_DAYS = 7;
    private static final int FINAL_WINDOW_DAYS = 1;

    ReportingDeadlineRepository deadlineRepository;
    AppUserRepository appUserRepository;
    NotificationService notificationService;

    /** Daily at 07:00 (server time). Cron is overridable via app.alerts.deadline-cron. */
    @Scheduled(cron = "${app.alerts.deadline-cron:0 0 7 * * *}")
    public void runDailyDeadlineReminders() {
        dispatchReminders(LocalDate.now());
    }

    /**
     * Core logic, separated from scheduling so it can be driven deterministically in tests.
     * Processes every UPCOMING deadline due within the next {@value #EARLY_WINDOW_DAYS} days.
     */
    @Transactional
    public void dispatchReminders(LocalDate today) {
        List<ReportingDeadline> candidates = deadlineRepository.findByStatusAndDueDateBetween(
                DeadlineStatus.UPCOMING, today, today.plusDays(EARLY_WINDOW_DAYS));

        int sent = 0;
        for (ReportingDeadline deadline : candidates) {
            long daysUntil = ChronoUnit.DAYS.between(today, deadline.getDueDate());
            boolean finalWindow = daysUntil <= FINAL_WINDOW_DAYS;

            if (finalWindow && !deadline.isWarned1Day()) {
                if (notify(deadline, daysUntil)) {
                    deadline.setWarned1Day(true);
                    sent++;
                }
            } else if (!finalWindow && !deadline.isWarned7Days()) {
                if (notify(deadline, daysUntil)) {
                    deadline.setWarned7Days(true);
                    sent++;
                }
            }
        }
        if (sent > 0) {
            log.info("Deadline reminders: sent {} of {} candidate deadline(s).", sent, candidates.size());
        }
    }

    /** Returns true only if the reminder was delivered, so the caller may mark the flag. */
    private boolean notify(ReportingDeadline deadline, long daysUntil) {
        List<String> recipients = appUserRepository
                .findAllByCompany_IdAndEnabledTrue(deadline.getCompany().getId())
                .stream().map(AppUser::getEmail).toList();
        if (recipients.isEmpty()) {
            return false; // no one to tell yet — leave unmarked so it retries when users exist
        }
        try {
            notificationService.sendDeadlineReminder(deadline, recipients, daysUntil);
            return true;
        } catch (Exception e) {
            log.error("Failed to send reminder for deadline {} ({})",
                    deadline.getId(), deadline.getReportType(), e);
            return false;
        }
    }
}
