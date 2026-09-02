package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/**
 * A delegate who can be named on Anexa 3 la HG 1061/2008, under "Date de identificare delegat şi
 * nr. de înmatriculare mijloc de transport".
 *
 * <p>{@link #partner} is nullable and that is the point of the table:
 *
 * <ul>
 *   <li>set — a driver of that carrier, edited inside the partner's form the way its work points
 *       are;</li>
 *   <li>null — <em>our own</em> drivers, which is the "— transportăm noi —" case of the movement
 *       form. Without them that case would stay on free text forever. They are managed in
 *       Settings.</li>
 * </ul>
 *
 * <p>What the movement stores stays the three text columns from V10, not a foreign key: picking a
 * driver prefills them and nothing more. The form prints a snapshot — the ID papers of that day,
 * the truck of that day — and a movement recorded last year has to print tomorrow exactly what it
 * printed yesterday, even if the man has since changed his ID card or left the firm.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "drivers")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    /** The carrier he drives for; null means he is ours. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    Partner partner;

    @Column(nullable = false)
    String name;

    /**
     * How he is identified on the paper — an ID series and number, or a CNP. One free-text field,
     * because the form has one rubric and practice writes now one, now the other.
     */
    @Column(length = 100)
    String identification;

    /** The truck he usually comes with. "Usually": on the movement it stays editable. */
    @Column(name = "vehicle_registration", length = 50)
    String vehicleRegistration;

    @Column(nullable = false)
    boolean active;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
