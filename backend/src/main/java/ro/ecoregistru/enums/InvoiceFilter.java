package ro.ecoregistru.enums;

/**
 * The filters of the Facturare screen (F-A, paged in F-B2). {@code ACTION} is the default: what needs a hand —
 * refused by FGO, or issued and past due.
 */
public enum InvoiceFilter {
    ACTION,
    FAILED,
    OVERDUE,
    UNPAID,
    PAID,
    ALL
}
