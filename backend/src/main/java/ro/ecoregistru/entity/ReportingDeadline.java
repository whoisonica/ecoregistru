package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.DeadlineStatus;
import ro.ecoregistru.enums.ReportType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A legal reporting deadline for a tenant (e.g. monthly AFM declaration).
 * AFM_MONTHLY deadlines are auto-generated (due on the 25th of the following month).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "reporting_deadlines",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_deadline_scope",
                columnNames = {"company_id", "report_type", "due_date"}))
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportingDeadline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    ReportType reportType;

    @Column(name = "due_date", nullable = false)
    LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DeadlineStatus status;

    Instant completedAt;

    @Column(length = 500)
    String completionNote;

    /** Whether the T-7 warning email has been sent (avoid duplicates). */
    @Column(name = "warned_7_days", nullable = false)
    boolean warned7Days;

    /** Whether the T-1 warning email has been sent. */
    @Column(name = "warned_1_day", nullable = false)
    boolean warned1Day;

    @Column(nullable = false)
    Instant createdAt;
}
