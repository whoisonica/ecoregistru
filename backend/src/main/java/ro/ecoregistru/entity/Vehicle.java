package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un vehicul din flotă (D2.1, V53). Alegerea lui pe o operațiune precompletează numărul de
 * înmatriculare, dar operațiunea păstrează textul: documentul tipărește instantaneul de atunci,
 * ca la șoferi ({@link Driver}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vehicles")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    /** Cu majuscule și fără spații, ca „cj 12 abc” și „CJ12ABC” să fie același camion. */
    @Column(nullable = false, length = 20)
    String registration;

    @Column(length = 100)
    String kind;

    @Column(name = "standard_tare_kg", precision = 14, scale = 3)
    BigDecimal standardTareKg;

    /** Peste 3,5 t: numai atunci se completează licența de transport. */
    @Column(nullable = false)
    boolean heavy;

    @Column(name = "itp_expiry")
    LocalDate itpExpiry;

    @Column(name = "transport_license_number", length = 100)
    String transportLicenseNumber;

    @Column(name = "transport_license_expiry")
    LocalDate transportLicenseExpiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_work_point_id")
    WorkPoint homeWorkPoint;

    /** Transportatorul, când vehiculul nu e al firmei. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    Partner partner;

    /** Data pentru care s-a trimis alerta; vezi {@code VehicleExpiryAlertScheduler}. */
    @Column(name = "expiry_warning_sent_for")
    LocalDate expiryWarningSentFor;

    @Column(nullable = false)
    boolean active;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
