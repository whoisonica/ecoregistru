package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.ScaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un cântar al depozitului (D2.3, V68). Dacă e legal la o dată se calculează din el și din
 * istoricul lui ({@link ScaleEvent}), în {@code ScaleLegality}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "scales")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Scale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_point_id", nullable = false)
    WorkPoint workPoint;

    @Column(nullable = false, length = 100)
    String name;

    @Column(name = "serial_number", length = 100)
    String serialNumber;

    @Column(length = 100)
    String kind;

    /** I, II, III sau IIII, de pe plăcuță. */
    @Column(name = "accuracy_class", length = 4)
    String accuracyClass;

    /** Diviziunea de verificare „e”, în kg. */
    @Column(name = "division_kg", precision = 10, scale = 3)
    BigDecimal divisionKg;

    @Column(name = "commissioned_on")
    LocalDate commissionedOn;

    @Column(name = "brml_declared_on")
    LocalDate brmlDeclaredOn;

    @Column(name = "brml_reference", length = 100)
    String brmlReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    ScaleStatus status;

    /** Data pentru care s-a trimis alerta; vezi {@code ScaleExpiryAlertScheduler}. */
    @Column(name = "expiry_warning_sent_for")
    LocalDate expiryWarningSentFor;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
