package ro.ecoregistru.controller.response;

import ro.ecoregistru.service.BillingCalculator.Invoice;

/** F-C — the two invoices a subscription not saved yet would produce, for the „Abonamentul” step. */
public record SubscriptionPreviewResponse(Invoice firstInvoice, Invoice monthlyInvoice) {}
