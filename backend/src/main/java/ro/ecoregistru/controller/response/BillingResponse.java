package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.enums.SubscriptionStatus;
import ro.ecoregistru.service.BillingCalculator.Invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The subscription an account pays, as the client sees it on {@code /abonament}.
 *
 * <p>Narrower than {@link SubscriptionResponse} on purpose: no grid prices, and only invoices FGO
 * has issued. A DRAFT is ours, and so is the FGO error written on it.
 *
 * @param billingEmail where the invoices are mailed: the billing email, or the company's contact email
 */
public record BillingResponse(
        String clientName,
        SubscriptionPlan plan,
        SubscriptionStatus status,
        LocalDate startedAt,
        boolean founder,
        Invoice nextInvoice,
        String billingEmail,
        String billingCounty,
        String billingCity,
        String billingAddress,
        List<IssuedInvoice> invoices
) {
    public record IssuedInvoice(
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
            Instant paidAt
    ) {}
}
