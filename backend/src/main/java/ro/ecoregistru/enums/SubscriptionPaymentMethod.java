package ro.ecoregistru.enums;

/**
 * How a subscription is paid (plata-abonamente.md, decision 1). Not chosen yet counts as a transfer.
 *
 * <p>Not {@link PaymentMethod}: that one is how a depot operation is paid (virament / numerar, V46).
 */
public enum SubscriptionPaymentMethod {
    /** Netopia: the client pays each invoice on Netopia's page, or the saved card is debited. */
    CARD,
    /** Bank transfer to the account on the invoice; FGO matches it from the statement. */
    TRANSFER
}
