package ro.ecoregistru.enums;

/**
 * Where a subscription is on the payment road (plata-abonamente.md §2). F1 only ever writes
 * {@link #PENDING}; the others are set by the invoicing and read-only slices.
 */
public enum SubscriptionStatus {
    /** Created by the platform; the client has not paid the first invoice yet. */
    PENDING,
    ACTIVE,
    /** An invoice is past its due date. */
    PAST_DUE,
    /** 15 days past due: the account reads and downloads, and writes nothing. */
    READ_ONLY,
    CANCELLED
}
