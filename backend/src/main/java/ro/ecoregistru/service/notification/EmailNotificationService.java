package ro.ecoregistru.service.notification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import ro.ecoregistru.entity.Driver;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.entity.Vehicle;
import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.ReportType;
import ro.ecoregistru.service.EmailService;
import ro.ecoregistru.service.SubscriptionStatusRules;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Email implementation of {@link NotificationService} (Romanian). Renders one message per
 * recipient via the shared {@link EmailService}; templates under resources/templates/mail/.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailNotificationService implements NotificationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    EmailService emailService;

    // Not final, so Lombok leaves it out of the constructor (see EmailService).
    @NonFinal
    @Value("${app.frontend-base-url}")
    String frontendBaseUrl;

    @Override
    public void sendDeadlineReminder(ReportingDeadline deadline, List<String> recipientEmails, long daysUntil) {
        String reportLabel = label(deadline.getReportType());
        String dueDate = deadline.getDueDate().format(DATE);
        String subject = "Termen de raportare — " + reportLabel + " (" + when(daysUntil) + ")";

        for (String to : recipientEmails) {
            Context ctx = new Context(Locale.of("ro"));
            ctx.setVariable("reportLabel", reportLabel);
            ctx.setVariable("dueDate", dueDate);
            ctx.setVariable("daysUntil", daysUntil);
            ctx.setVariable("whenText", when(daysUntil));
            emailService.send(to, subject, "mail/deadline_reminder", ctx);
        }
    }

    /**
     * The 60-day warning that a partner's environmental authorization is running out (V30).
     *
     * <p>The subject names the partner, not the rule: what the reader has to do is check one
     * specific company, and a subject that says "Autorizaţie de mediu" without a name is a subject
     * nobody opens twice. The body says what is at stake — art. 23 alin. (1) — because otherwise
     * the natural reading is "the partner has a problem", when in fact the exposure is the
     * tenant's own.
     */
    @Override
    public void sendPartnerAuthorizationWarning(Partner partner, List<String> recipientEmails, long daysUntil) {
        String expiryDate = partner.authorizationValidUntil().format(DATE);
        String subject = "Autorizația de mediu a partenerului " + partner.getName()
                + " expiră (" + whenExpiry(daysUntil) + ")";

        for (String to : recipientEmails) {
            Context ctx = new Context(Locale.of("ro"));
            ctx.setVariable("partnerName", partner.getName());
            ctx.setVariable("partnerCui", partner.getCui());
            ctx.setVariable("authorizationNumber", partner.getAuthorizationNumber());
            ctx.setVariable("expiryDate", expiryDate);
            ctx.setVariable("daysUntil", daysUntil);
            ctx.setVariable("whenText", whenExpiry(daysUntil));
            emailService.send(to, subject, "mail/partner_authorization_expiring", ctx);
        }
    }

    /** D2.1 — subiectul numește mașina, ca la partener: cititorul are de verificat un camion anume. */
    @Override
    public void sendVehicleExpiryWarning(Vehicle vehicle, List<String> recipientEmails, long daysUntil) {
        String subject = "ITP-ul sau licența vehiculului " + vehicle.getRegistration() + " " + whenExpiry(daysUntil);
        for (String to : recipientEmails) {
            Context ctx = new Context(Locale.of("ro"));
            ctx.setVariable("registration", vehicle.getRegistration());
            ctx.setVariable("kind", vehicle.getKind());
            ctx.setVariable("itpExpiry", vehicle.getItpExpiry() == null ? null : vehicle.getItpExpiry().format(DATE));
            ctx.setVariable("licenseNumber", vehicle.getTransportLicenseNumber());
            ctx.setVariable("licenseExpiry", vehicle.getTransportLicenseExpiry() == null ? null
                    : vehicle.getTransportLicenseExpiry().format(DATE));
            ctx.setVariable("whenText", whenExpiry(daysUntil));
            ctx.setVariable("settingsUrl", frontendBaseUrl + "/setari#flota");
            emailService.send(to, subject, "mail/vehicle_expiring", ctx);
        }
    }

    /** D2.2 — ca la vehicul: subiectul numește șoferul. */
    @Override
    public void sendDriverAttestationWarning(Driver driver, List<String> recipientEmails, long daysUntil) {
        String subject = "Atestatul șoferului " + driver.getName() + " " + whenExpiry(daysUntil);
        for (String to : recipientEmails) {
            Context ctx = new Context(Locale.of("ro"));
            ctx.setVariable("name", driver.getName());
            ctx.setVariable("attestationNumber", driver.getAttestationNumber());
            ctx.setVariable("attestationExpiry", driver.getAttestationExpiry().format(DATE));
            ctx.setVariable("whenText", whenExpiry(daysUntil));
            ctx.setVariable("settingsUrl", frontendBaseUrl + "/setari#soferi");
            emailService.send(to, subject, "mail/driver_attestation_expiring", ctx);
        }
    }

    /**
     * §9.4 of plata-abonamente.md — the invoice goes out from our address, not from FGO's: one sender
     * for everything about paying. The subject carries the number, which is what the client writes
     * on the transfer.
     */
    @Override
    public void sendSubscriptionInvoice(SubscriptionInvoice invoice, String clientName, String recipientEmail) {
        String number = invoice.getFgoSerie() + " " + invoice.getFgoNumar();
        Context ctx = new Context(Locale.of("ro"));
        ctx.setVariable("clientName", clientName);
        ctx.setVariable("number", number);
        ctx.setVariable("period", invoice.getPeriodStart().format(DATE) + " – " + invoice.getPeriodEnd().format(DATE));
        ctx.setVariable("total", lei(invoice.getTotal()));
        ctx.setVariable("dueDate", invoice.getDueDate() == null ? null : invoice.getDueDate().format(DATE));
        ctx.setVariable("paid", invoice.getStatus() == InvoiceStatus.PAID);
        ctx.setVariable("pdfUrl", invoice.getFgoLink());
        ctx.setVariable("payUrl", invoice.getFgoLinkPlata());
        ctx.setVariable("accountUrl", frontendBaseUrl + "/abonament");
        emailService.send(recipientEmail, "Factura " + number + " — abonamentul WasteHouse",
                "mail/subscription_invoice", ctx);
    }

    /**
     * F3/F4 — one template for the four reminders: they say the same things (which invoice, how much, what
     * to do) and differ in the first sentence. The subject names the invoice, as the invoice mail does.
     */
    @Override
    public void sendBillingReminder(SubscriptionInvoice invoice, String clientName, String recipientEmail,
                                    BillingReminder kind, String reason) {
        String number = invoice.getFgoSerie() + " " + invoice.getFgoNumar();
        LocalDate due = invoice.getDueDate();
        Context ctx = new Context(Locale.of("ro"));
        ctx.setVariable("kind", kind.name());
        ctx.setVariable("clientName", clientName);
        ctx.setVariable("number", number);
        ctx.setVariable("total", lei(invoice.getTotal()));
        ctx.setVariable("dueDate", due == null ? null : due.format(DATE));
        ctx.setVariable("readOnlyOn", due == null ? null
                : due.plusDays(SubscriptionStatusRules.READ_ONLY_AFTER_DAYS).format(DATE));
        ctx.setVariable("reason", reason);
        ctx.setVariable("pdfUrl", invoice.getFgoLink());
        ctx.setVariable("accountUrl", frontendBaseUrl + "/abonament");
        String subject = switch (kind) {
            case OVERDUE -> "Factura " + number + " a trecut de scadență";
            case READ_ONLY_WARNING -> "Factura " + number + ": în 7 zile contul trece în doar-citire";
            case READ_ONLY -> "Contul WasteHouse e acum doar pentru citire — factura " + number;
            case CARD_FAILED -> "Plata cu cardul pentru factura " + number + " nu a trecut";
        };
        emailService.send(recipientEmail, subject, "mail/billing_reminder", ctx);
    }

    /**
     * P2.13, felia 2 — rezumatul zilnic al cabinetului. Subiectul spune câte termene sunt, fiindcă asta
     * decide dacă mailul se deschide azi sau mâine; firmele sunt în corp.
     */
    @Override
    public void sendConsultantDigest(String consultancyName, List<ReportingDeadline> deadlines,
                                     List<String> recipientEmails, LocalDate today) {
        List<Map<String, String>> rows = deadlines.stream()
                .map(d -> Map.of(
                        "company", d.getCompany().getName(),
                        "label", label(d.getReportType()),
                        "dueDate", d.getDueDate().format(DATE),
                        "whenText", when(ChronoUnit.DAYS.between(today, d.getDueDate()))))
                .toList();
        String countText = deadlines.size() == 1 ? "1 termen"
                : deadlines.size() + (deadlines.size() >= 20 ? " de termene" : " termene");
        String subject = consultancyName + ": " + countText + " în următoarele 7 zile";

        for (String to : recipientEmails) {
            Context ctx = new Context(Locale.of("ro"));
            ctx.setVariable("consultancyName", consultancyName);
            ctx.setVariable("countText", countText);
            ctx.setVariable("rows", rows);
            ctx.setVariable("panelUrl", frontendBaseUrl + "/cabinet");
            emailService.send(to, subject, "mail/consultant_digest", ctx);
        }
    }

    /** "389 lei", "1.234,50 lei". */
    private static String lei(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.of("ro"));
        format.setMinimumFractionDigits(amount.stripTrailingZeros().scale() > 0 ? 2 : 0);
        format.setMaximumFractionDigits(2);
        return format.format(amount) + " lei";
    }

    /** Human phrasing of the remaining time, used in the subject and body. */
    private String when(long daysUntil) {
        if (daysUntil <= 0) return "scadent astăzi";
        if (daysUntil == 1) return "scadent mâine";
        return "scadent în " + daysUntil + " zile";
    }

    /**
     * Same idea as {@link #when(long)}, but for an expiry rather than a due date. Separate because
     * "scadent" is wrong about an authorization: a deadline is something you meet, an authorization
     * is something that lapses.
     *
     * <p>Zero and negative are handled even though the scheduler never sends them — its window
     * starts today (see V30) — so that a future caller with a different window does not print
     * "expiră în -3 zile".
     */
    private String whenExpiry(long daysUntil) {
        if (daysUntil < 0) return "expirată";
        if (daysUntil == 0) return "expiră astăzi";
        if (daysUntil == 1) return "expiră mâine";
        return "expiră în " + daysUntil + (daysUntil >= 20 ? " de zile" : " zile");
    }

    /*
     * The "de" above is grammar, not decoration: Romanian inserts it before the noun from 20
     * upwards, so "în 60 de zile" but "în 5 zile". It matters here and not in when(), whose
     * window is seven days and never reaches the threshold — which is why the two phrasings stay
     * separate methods rather than one shared helper that would be right for only one of them.
     */

    /**
     * What the client is actually being reminded of. SIM_ANNUAL is named after the document, not
     * the portal: what gets filed by 15 March is the evidence itself — the Anexa 1 sheets of
     * HG 856/2002 — uploaded into the system APM provides (OUG 92/2021 art. 48 alin. (1)).
     * "Raportarea SIM" named the channel and left the client guessing what to prepare.
     */
    private String label(ReportType type) {
        return switch (type) {
            case SIM_ANNUAL -> "Evidența gestiunii deșeurilor generate (anual, 15 martie)";
            case AFM_MONTHLY -> "Declarația AFM (lunară, 25) — contribuția de 2% reținută la sursă";
            case AFM_QUARTERLY -> "Declarația AFM (trimestrială, 25) — contribuția pentru economia circulară";
            case AFM_ANNUAL -> "Declarația AFM (anuală, 25 ianuarie) — contribuția pentru ambalaje "
                    + "și notificarea că obiectivele se îndeplinesc individual";
            case PACKAGING_ANNUAL -> "Anexa 1 Ambalaje (anual, 25 februarie) — la agenția "
                    + "județeană de mediu";
            // Două obligații pe un singur rând, ca la AFM_ANNUAL: aceeași zi, același destinatar.
            // Eticheta le numește pe amândouă, fiindcă firma care primește alerta poate să datoreze
            // doar una din ele și trebuie să vadă care.
            case APM_ANNUAL_APRIL -> "Raportare anuală la APM (30 aprilie) — uleiuri uzate "
                    + "(art. 31) și deșeuri din construcții (art. 17 alin. (7))";
            // Al patrulea termen anual, găsit pe 11.09.2026. Eticheta numește documentul, nu
            // articolul: clientul trebuie să știe ce pregătește, iar programul de prevenire nu
            // seamănă cu nimic altceva din calendar.
            case APM_ANNUAL_MAY -> "Programul de prevenire și reducere a cantităților de deșeuri "
                    + "(anual, 31 mai) — la agenția județeană de mediu, cu progresul înregistrat";
            case OTHER -> "Raportare";
        };
    }
}
