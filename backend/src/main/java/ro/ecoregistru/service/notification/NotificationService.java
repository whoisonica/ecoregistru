package ro.ecoregistru.service.notification;

import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.entity.Vehicle;

import java.time.LocalDate;
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

    /**
     * D2.1 — ITP-ul sau licența de transport a unui vehicul din flotă expiră în 30 de zile.
     *
     * @param daysUntil zile întregi până la cea mai apropiată expirare (0 = azi)
     * @throws RuntimeException if delivery fails — the caller must then leave the vehicle unmarked.
     */
    void sendVehicleExpiryWarning(Vehicle vehicle, List<String> recipientEmails, long daysUntil);

    /**
     * Sends an issued subscription invoice to whoever pays it, with FGO's PDF link (§9.4 of
     * plata-abonamente.md: the application sends it, not FGO).
     *
     * @throws RuntimeException if delivery fails — the caller must then leave the invoice unmarked,
     *                          so it is sent again on the next run.
     */
    void sendSubscriptionInvoice(SubscriptionInvoice invoice, String clientName, String recipientEmail);

    /**
     * A reminder about an issued invoice: overdue, read-only soon, read-only, or a refused card debit.
     *
     * @param reason Netopia's refusal, for {@link BillingReminder#CARD_FAILED}; null otherwise
     * @throws RuntimeException if delivery fails — the caller must then leave the invoice unmarked,
     *                          so the reminder is sent again on the next run.
     */
    void sendBillingReminder(SubscriptionInvoice invoice, String clientName, String recipientEmail,
                             BillingReminder kind, String reason);

    /**
     * P2.13, felia 2 — rezumatul zilnic al unui cabinet: un mail pe consultant, cu toate termenele
     * nefinalizate din zilele următoare ale firmelor cabinetului.
     *
     * @param deadlines termenele, deja ordonate, fiecare cu firma încărcată
     * @param today     ziua față de care se scrie „scadent mâine"
     * @throws RuntimeException if delivery fails — the digest is not retried; tomorrow's carries the same deadlines.
     */
    void sendConsultantDigest(String consultancyName, List<ReportingDeadline> deadlines,
                              List<String> recipientEmails, LocalDate today);
}
