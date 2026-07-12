package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.DeadlineStatus;
import ro.ecoregistru.enums.ReportType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A reporting deadline as shown on the Termene screen. {@code status} is the effective
 * status derived at read time: DONE if completed, else OVERDUE if past due, else UPCOMING.
 */
public record DeadlineResponse(
        UUID id,
        ReportType reportType,
        LocalDate dueDate,
        DeadlineStatus status,
        Instant completedAt,
        String completionNote
) {}
