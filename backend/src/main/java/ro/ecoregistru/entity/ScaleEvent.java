package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.ScaleEventKind;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Un rând din istoricul unui cântar: verificare cu buletin, reparație sau incident (D2.3, V68). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "scale_events")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScaleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scale_id", nullable = false)
    Scale scale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    ScaleEventKind kind;

    @Column(name = "event_date", nullable = false)
    LocalDate date;

    /** ADMIS / RESPINS; doar la verificare. */
    Boolean admitted;

    @Column(name = "bulletin_number", length = 60)
    String bulletinNumber;

    /** Doar la ADMIS: cel mult un an de la verificare. */
    @Column(name = "valid_until")
    LocalDate validUntil;

    @Column(length = 255)
    String laboratory;

    @Column(length = 255)
    String verifier;

    @Column(length = 1000)
    String notes;

    @Column(name = "created_by", nullable = false, updatable = false)
    UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
}
