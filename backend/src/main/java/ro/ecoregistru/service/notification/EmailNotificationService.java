package ro.ecoregistru.service.notification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.ReportType;
import ro.ecoregistru.service.EmailService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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
        String expiryDate = partner.getAuthorizationExpiry().format(DATE);
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
