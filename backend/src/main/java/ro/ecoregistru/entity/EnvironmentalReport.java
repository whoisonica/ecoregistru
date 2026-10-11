package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.Instant;
import java.util.UUID;

/**
 * The year's answers on the raport anual de mediu (RAM). A missing answer is {@code null}, never a default "no": the
 * printed report says "Nu a fost cazul" only when someone said so.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "environmental_reports")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnvironmentalReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @Column(nullable = false)
    int year;

    /** Incidents, accidents, accidental pollution. */
    Boolean incidents;

    @Column(name = "incidents_text", columnDefinition = "TEXT")
    String incidentsText;

    Boolean complaints;

    @Column(name = "complaints_text", columnDefinition = "TEXT")
    String complaintsText;

    /** Whether the Guard or another institution inspected the company this year; the list is the inspections. */
    Boolean inspected;

    /** Measures for preventing and repairing environmental damage (Cap. 3). */
    Boolean damage;

    @Column(name = "damage_text", columnDefinition = "TEXT")
    String damageText;

    @Column(name = "other_requirements", columnDefinition = "TEXT")
    String otherRequirements;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
}
