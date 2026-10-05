package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.SubscriptionPaymentMethod;
import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.enums.SubscriptionStatus;
import ro.ecoregistru.service.BillingCalculator.Invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * F1 — a subscription with the two invoices it produces, computed on today's counts: the first
 * period (with the implementation) and the one after it; after a switch of the billing length, the
 * first two periods not yet invoiced.
 *
 * <p>F2 — plus its billing data and the invoices already reserved or issued, newest first.
 */
public record SubscriptionResponse(
        UUID id,
        SubscriptionPlan plan,
        /* 05.10.2026 — the employee-count tier (1–5), null before the tiers and off the generator plans. */
        Integer sizeTier,
        boolean customPrice,
        /* 05.10.2026 — 1 (monthly) or 12 (annual); on annual, monthlyPrice is the price per year. */
        int billingMonths,
        SubscriptionStatus status,
        BigDecimal monthlyPrice,
        BigDecimal implementationFee,
        BigDecimal extraWorkPointPrice,
        BigDecimal companyPriceTier1,
        BigDecimal companyPriceTier2,
        BigDecimal companyPriceTier3,
        BigDecimal packagingCompanyPrice,
        boolean founder,
        boolean twelveMonthCommitment,
        LocalDate startedAt,
        Invoice firstInvoice,
        Invoice monthlyInvoice,
        String billingEmail,
        String billingCounty,
        String billingCity,
        String billingAddress,
        List<SubscriptionInvoiceResponse> invoices,
        /* F3/F4 — the card as the client sees it (never the token), and the last day of a stopped one. */
        SubscriptionPaymentMethod paymentMethod,
        String cardPanMasked,
        String cardExpiry,
        LocalDate endsOn
) {}
