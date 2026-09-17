package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.SubscriptionPaymentMethod;
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
 * @param clientCui    F-E — shown next to the billing data, which the client edits; the CUI it cannot
 * @param payee        F-E — where a transfer goes, the same account the invoice prints
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
        List<IssuedInvoice> invoices,
        /* F3 */
        SubscriptionPaymentMethod paymentMethod,
        String cardPanMasked,
        String cardExpiry,
        /** Whether „Plătește cu cardul" can start: Netopia's keys are on the server. */
        boolean cardPaymentAvailable,
        /* F4 */
        LocalDate endsOn,
        /** The day the account turns read-only if the oldest unpaid invoice stays unpaid; null when off or nothing is late. */
        LocalDate readOnlyOn,
        /* F-E */
        String clientCui,
        Payee payee
) {
    public record Payee(String name, String cui, String iban, String bank) {}

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
            Instant paidAt,
            SubscriptionPaymentMethod paidBy,
            String lastCardError,
            /** F-E — the last time FGO was asked whether it is paid, by the run or by „Verifică plata”. */
            Instant paymentCheckedAt
    ) {}
}
