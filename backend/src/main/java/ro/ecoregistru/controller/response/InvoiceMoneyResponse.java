package ro.ecoregistru.controller.response;

import java.math.BigDecimal;

/** F-B — the money figures on top of Clienți: paid this month (Bucharest time) and issued but unpaid, any month. */
public record InvoiceMoneyResponse(
        BigDecimal paidThisMonth,
        long paidThisMonthCount,
        BigDecimal unpaid,
        long unpaidCount
) {}
