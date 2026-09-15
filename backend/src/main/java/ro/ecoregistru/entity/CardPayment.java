package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.CardPaymentKind;
import ro.ecoregistru.enums.CardPaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One attempt to pay an invoice by card through Netopia (F3, {@code V47}). The row is written before
 * Netopia is called: its {@link #orderId} is what the notification comes back with.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "card_payments")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CardPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    SubscriptionInvoice invoice;

    @Column(nullable = false, unique = true, length = 64)
    String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    CardPaymentKind kind;

    @Column(nullable = false)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    CardPaymentStatus status;

    String ntpId;

    String paymentUrl;

    /** Netopia's code and message for a refusal, or ours when Netopia could not be reached. */
    String error;

    @Column(nullable = false)
    Instant createdAt;

    @Column(nullable = false)
    Instant updatedAt;
}
