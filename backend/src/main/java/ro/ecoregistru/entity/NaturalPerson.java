package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/**
 * O persoană fizică de la care depozitul cumpără (V46). Nu e partener: n-are CUI și nici autorizație.
 *
 * <p>CNP-ul și actul de identitate sunt obligatorii doar la o operațiune cu metal (OUG 31/2011); la
 * restul se ține doar numele (Legea 190/2018 art. 4 alin. (2)). Amândouă se redactează în jurnalul
 * de audit, după numele câmpurilor, ca la șoferi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "natural_persons")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NaturalPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @Column(nullable = false, length = 160)
    String name;

    @Column(length = 13)
    String cnp;

    /** Seria și numărul actului de identitate. */
    @Column(length = 100)
    String identification;

    /** Domiciliul, cum îl cere borderoul. */
    @Column(length = 500)
    String address;

    @Column(nullable = false)
    boolean active;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
