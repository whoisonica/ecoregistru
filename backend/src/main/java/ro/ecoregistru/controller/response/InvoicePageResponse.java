package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.InvoiceFilter;

import java.util.List;
import java.util.Map;

/** F-B2 — one page of the Facturare table, with how many invoices each filter key holds for the same month and search. */
public record InvoicePageResponse(
        List<BillingInvoiceRow> content,
        long total,
        int page,
        int size,
        Map<InvoiceFilter, Long> counts
) {}
