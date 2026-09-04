package ro.ecoregistru.service.notification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.ReportType;
import ro.ecoregistru.service.EmailService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Email implementation of {@link NotificationService} (Romanian). Renders one reminder per
 * recipient via the shared {@link EmailService}; template at resources/templates/mail/deadline_reminder.html.
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

    /** Human phrasing of the remaining time, used in the subject and body. */
    private String when(long daysUntil) {
        if (daysUntil <= 0) return "scadent astăzi";
        if (daysUntil == 1) return "scadent mâine";
        return "scadent în " + daysUntil + " zile";
    }

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
            case OTHER -> "Raportare";
        };
    }
}
