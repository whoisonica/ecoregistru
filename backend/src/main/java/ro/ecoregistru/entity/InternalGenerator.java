package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/**
 * An internal generator (generator intern) — the source inside a work point that produces the
 * waste: the offices, the production hall, the canteen.
 *
 * <p>This is the third location level, and the only one that is not an address: the company has an
 * address, the work point has an address, and the internal generator sits inside the work point.
 * It is what HG 856/2002 anexa nr. 1 cap. 2 prints in the <em>"Secţia"</em> column — in every
 * filled Anexa 1 received from the specialist the value is constant across all twelve rows of a
 * sheet ("birouri", "productie"), which is what makes it a property of the source rather than of
 * the month.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "internal_generators")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InternalGenerator {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_point_id", nullable = false)
    WorkPoint workPoint;

    /** Printed verbatim as "Secţia" in Anexa 1 cap. 2, so it is the wording the client uses. */
    @Column(nullable = false)
    String name;

    /** What actually happens there, for the operator who picks it from a list months later. */
    @Column(length = 1000)
    String description;

    @Column(nullable = false)
    boolean active;

    @Column(nullable = false)
    Instant createdAt;
}
