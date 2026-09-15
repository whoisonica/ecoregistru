package ro.ecoregistru.enums;

/** Where one card payment attempt is (V47). */
public enum CardPaymentStatus {
    /** Sent to Netopia; waiting for the client on the payment page, or for the notification. */
    STARTED,
    PAID,
    /** Refused, or needs the client (3-D Secure) where nobody is there; see {@code error}. */
    FAILED
}
