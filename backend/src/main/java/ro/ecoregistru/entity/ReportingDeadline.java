package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.DeadlineRecurrence;
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
// UPDATE doar pe coloanele schimbate, fiindcă rândul are doi scriitori fără versiune: omul, care
// bifează „Depus” (status, completedAt), și mementoul de dimineață, care ține rândul încărcat cât
// trimite mailurile pe rând și abia apoi scrie steagul warned*. Cu UPDATE pe tot rândul, bifa
// pusă între timp era rescrisă înapoi pe UPCOMING de flush-ul mementoului (29.09.2026).
@org.hibernate.annotations.DynamicUpdate
// Unicitatea (firmă, tip, scadență) e un index parțial din V81: nu privește termenele proprii.
@Table(name = "reporting_deadlines")
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

    /** V61 — mailul de a doua zi după un termen ratat a plecat (o singură dată). */
    @Column(name = "warned_missed", nullable = false)
    boolean warnedMissed;

    @Column(nullable = false)
    Instant createdAt;

    /** V81 — doar la {@link ReportType#CUSTOM}: numele dat de firmă („Măsurători de zgomot”). */
    @Column(length = 120)
    String title;

    /** V81 — doar la {@link ReportType#CUSTOM}: cât de des revine. */
    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    DeadlineRecurrence recurrence;

    /** V81 — doar la {@link ReportType#CUSTOM}: ce e de făcut, cu cine (laboratorul, contractul). */
    @Column(length = 500)
    String details;

    /** V81 — doar la {@link ReportType#CUSTOM}: aparițiile aceluiași termen care se repetă. */
    @Column(name = "series_id")
    UUID seriesId;

    public boolean isCustom() {
        return reportType == ReportType.CUSTOM;
    }
}
