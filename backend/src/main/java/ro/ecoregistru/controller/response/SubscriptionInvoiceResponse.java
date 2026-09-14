package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** F2 — one invoice of a subscription, as the platform sees it in the Abonament dialog. */
public record SubscriptionInvoiceResponse(
        UUID id,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal total,
        InvoiceStatus status,
        LocalDate dueDate,
        String fgoSerie,
        String fgoNumar,
        String fgoLink,
        String fgoLinkPlata,
        BigDecimal amountPaid,
        String lastError,
        Instant paidAt
) {}
