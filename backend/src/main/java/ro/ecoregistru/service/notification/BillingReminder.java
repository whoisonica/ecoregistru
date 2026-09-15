package ro.ecoregistru.service.notification;

/** The mails about paying, after the invoice itself (plata-abonamente.md §2.2–§2.3). */
public enum BillingReminder {
    /** Due date + 1. */
    OVERDUE,
    /** Due date + 8: read-only in 7 days. Only with read-only switched on (§9.6). */
    READ_ONLY_WARNING,
    /** Due date + 15: the account now only reads. */
    READ_ONLY,
    /** A debit of the saved card was refused, or asked for the client. */
    CARD_FAILED
}
