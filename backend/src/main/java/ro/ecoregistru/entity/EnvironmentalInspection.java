package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One inspection of the RAM's year: who came, where, the report's number and date, and the measures ordered. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "environmental_inspections")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnvironmentalInspection {

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

    String institution;

    @Column(name = "report_number", length = 100)
    String reportNumber;

    @Column(name = "report_date")
    LocalDate reportDate;

    @Column(columnDefinition = "TEXT")
    String measures;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
}
