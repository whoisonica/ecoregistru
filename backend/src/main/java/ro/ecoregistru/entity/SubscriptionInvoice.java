package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.SubscriptionPaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One billing period of a subscription, as invoiced through FGO (F2, {@code V44}).
 *
 * <p>The row is written <b>before</b> FGO is asked: its id is sent as {@code IdExtern} with
 * {@code VerificareDuplicat}, so a retried issue cannot produce a second invoice.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "subscription_invoices")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriptionInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    Subscription subscription;

    @Column(nullable = false)
    LocalDate periodStart;

    @Column(nullable = false)
    LocalDate periodEnd;

    /** Frozen when the period starts, together with {@link #linesJson}. Lei, without VAT. */
    @Column(nullable = false)
    BigDecimal total;

    /** {@code BillingCalculator.Line}s as JSON: what the invoice was reserved with. */
    @Column(name = "lines_json", nullable = false, columnDefinition = "text")
    String linesJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    InvoiceStatus status;

    LocalDate dueDate;

    String fgoSerie;
    String fgoNumar;
    String fgoLink;
    String fgoLinkPlata;

    BigDecimal amountPaid;

    /** Why the last issue attempt failed; cleared once FGO issues the invoice. */
    String lastError;

    Instant issuedAt;
    Instant paidAt;

    /** When the invoice was mailed to the client (V45); null until the mail actually left. */
    Instant emailedAt;

    // --- V47 ---

    /** Who settled it: Netopia's notification, or FGO reading the bank statement. */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    SubscriptionPaymentMethod paidBy;

    /** A card payment recorded in FGO ({@code factura/incasare}); retried by the run until it is. */
    Instant fgoCollectedAt;

    /** The reminders of §2.3, each sent once, marked only after it left. */
    Instant overdueMailedAt;
    Instant warningMailedAt;
    Instant readOnlyMailedAt;

    @Column(nullable = false)
    Instant createdAt;
}
