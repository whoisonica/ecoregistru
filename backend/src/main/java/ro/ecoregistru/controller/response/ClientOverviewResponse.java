package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * F-B (todo-clienti-abonamente.md) — ce arată tabelul Clienți pe lângă firmă: abonamentul, ultima factură și câți
 * utilizatori are. Separat de {@link CompanyResponse}, care ajunge și la firmă prin {@code /companies/current}:
 * banii unui client nu pleacă spre ecranele lui.
 *
 * <p>Abonamentul și factura sunt {@code null} pentru o firmă fără abonament propriu — și mereu pentru consultant.
 */
public record ClientOverviewResponse(
        UUID companyId,
        SubscriptionStatus subscriptionStatus,
        SubscriptionPlan plan,
        BigDecimal monthlyPrice,
        LastInvoice lastInvoice,
        long userCount
) {
    /** {@code number} e „WH 12”, {@code null} cât timp FGO n-a emis-o; {@code lastError} e motivul refuzului. */
    public record LastInvoice(
            String number,
            BigDecimal total,
            InvoiceStatus status,
            LocalDate dueDate,
            String lastError
    ) {}
}
