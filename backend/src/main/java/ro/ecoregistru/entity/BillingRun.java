package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/** One invoicing run as it ended (V63), shown on the Facturare screen. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "billing_runs")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BillingRun {

    public enum Kind { SCHEDULED, MANUAL }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, updatable = false)
    Instant startedAt;

    @Column(nullable = false, updatable = false)
    Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    Kind kind;

    /** Who pressed the button; null for the 06:30 run. */
    @Column(updatable = false)
    UUID triggeredBy;

    @Column(name = "result_json", nullable = false, columnDefinition = "text", updatable = false)
    String resultJson;
}
