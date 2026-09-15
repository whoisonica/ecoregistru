package ro.ecoregistru.enums;

/** How a card payment was started (V47). */
public enum CardPaymentKind {
    /** The client pays on Netopia's hosted page, from {@code /abonament} or the invoice mail. */
    CHECKOUT,
    /** The daily run debits the card saved by an earlier payment, without the client. */
    TOKEN
}
