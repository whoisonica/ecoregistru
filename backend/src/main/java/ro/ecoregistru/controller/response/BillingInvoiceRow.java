package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.SubscriptionPaymentMethod;
import ro.ecoregistru.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One invoice of any client, on the Facturare screen (F-A). {@code ownerKind}/{@code ownerId} open the client's
 * subscription; {@code lastError} is the reason FGO refused it, in the words {@code BillingRunService} keeps.
 */
public record BillingInvoiceRow(
        UUID id,
        String client,
        String ownerKind,
        UUID ownerId,
        SubscriptionStatus subscriptionStatus,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal total,
        InvoiceStatus status,
        LocalDate dueDate,
        String fgoSerie,
        String fgoNumar,
        String fgoLink,
        BigDecimal amountPaid,
        String lastError,
        Instant issuedAt,
        Instant paidAt,
        SubscriptionPaymentMethod paidBy,
        Instant emailedAt,
        Instant overdueMailedAt,
        Instant paymentCheckedAt
) {}
