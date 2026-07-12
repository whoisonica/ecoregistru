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

    private String label(ReportType type) {
        return switch (type) {
            case SIM_ANNUAL -> "Raportarea SIM (anuală) — ANPM";
            case AFM_MONTHLY -> "Declarația AFM (lunară) — Fondul pentru Mediu";
            case OTHER -> "Raportare";
        };
    }
}
