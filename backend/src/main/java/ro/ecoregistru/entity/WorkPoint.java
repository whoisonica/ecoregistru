package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/**
 * A physical work point (punct de lucru) of a Company. Legal waste records are per work point.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "work_points")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @Column(nullable = false)
    String name;

    String address;

    @Column(nullable = false)
    boolean active;

    /**
     * D2.5 (V71) — autorizația de mediu a amplasamentului: se emite pe punct de lucru. Goală = se citește a firmei.
     * Un transfer spre un depozit fără autorizație valabilă se refuză (HG 1061/2008 art. 1 alin. (3)).
     */
    @Column(name = "environmental_auth_number", length = 100)
    String environmentalAuthNumber;

    @Column(name = "environmental_auth_expiry")
    java.time.LocalDate environmentalAuthExpiry;

    /** D2.6 (V72) — seria registrului formularelor primite al depozitului, tipărită în antetul lui. */
    @Column(name = "received_forms_series", length = 20)
    String receivedFormsSeries;

    @Column(nullable = false)
    Instant createdAt;
}
