package ro.ecoregistru.service.notification;

import ro.ecoregistru.entity.ReportingDeadline;

import java.util.List;

/**
 * Abstraction over how tenants are notified about reporting deadlines. Kept as an interface so
 * the scheduler depends on the behaviour, not the transport (email today; SMS/in-app later),
 * and so tests can substitute a double without touching SMTP.
 */
public interface NotificationService {

    /**
     * Notifies the given recipients that a deadline is approaching.
     *
     * @param daysUntil whole days from today until the due date (0 = due today).
     * @throws RuntimeException if delivery fails — the caller decides whether to mark the
     *                          deadline as warned (it should not, so the reminder is retried).
     */
    void sendDeadlineReminder(ReportingDeadline deadline, List<String> recipientEmails, long daysUntil);
}
