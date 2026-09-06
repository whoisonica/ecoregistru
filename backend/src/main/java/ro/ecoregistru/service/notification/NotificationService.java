package ro.ecoregistru.service.notification;

import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.ReportingDeadline;

import java.util.List;

/**
 * Abstraction over how tenants are notified about things with a date on them — reporting deadlines
 * and, since V30, a partner authorization about to lapse. Kept as an interface so the schedulers
 * depend on the behaviour, not the transport (email today; SMS/in-app later), and so tests can
 * substitute a double without touching SMTP.
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

    /**
     * Notifies the given recipients that a partner's environmental authorization is about to
     * expire, while there is still time to do something about it.
     *
     * <p>OUG 92/2021 art. 23 alin. (1) makes a handover legal only towards an <em>authorized</em>
     * operator, and alin. (2) adds that handing the waste over does not discharge responsibility.
     * So this is the tenant's own exposure, not the partner's.
     *
     * @param daysUntil whole days from today until the expiry date (0 = expires today).
     * @throws RuntimeException if delivery fails — the caller must then leave the partner
     *                          unmarked, so the warning is retried tomorrow.
     */
    void sendPartnerAuthorizationWarning(Partner partner, List<String> recipientEmails, long daysUntil);
}
