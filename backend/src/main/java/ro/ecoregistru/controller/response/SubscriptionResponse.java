package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.enums.SubscriptionStatus;
import ro.ecoregistru.service.BillingCalculator.Invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * F1 — a subscription with the two invoices it produces, computed on today's counts: the first
 * period (with the implementation) and the one after it.
 */
public record SubscriptionResponse(
        UUID id,
        SubscriptionPlan plan,
        SubscriptionStatus status,
        BigDecimal monthlyPrice,
        BigDecimal implementationFee,
        BigDecimal extraWorkPointPrice,
        BigDecimal companyPriceTier1,
        BigDecimal companyPriceTier2,
        BigDecimal companyPriceTier3,
        BigDecimal packagingCompanyPrice,
        boolean founder,
        LocalDate startedAt,
        Invoice firstInvoice,
        Invoice monthlyInvoice
) {}
