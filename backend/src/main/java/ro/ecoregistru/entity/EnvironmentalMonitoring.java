package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One monitoring of the RAM's year: the factor (noise, air, water...), when, and the test report's number and date. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "environmental_monitorings")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnvironmentalMonitoring {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    EnvironmentalReport report;

    @Column(nullable = false)
    int position;

    @Column(name = "work_point")
    String workPoint;

    String factor;

    @Column(name = "performed_on")
    LocalDate performedOn;

    @Column(name = "test_report_number", length = 100)
    String testReportNumber;

    @Column(name = "test_report_date")
    LocalDate testReportDate;

    @Column(columnDefinition = "TEXT")
    String result;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
}
