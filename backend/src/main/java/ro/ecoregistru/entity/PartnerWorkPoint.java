package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/**
 * A work point of a partner: where the waste is actually unloaded, when that is not their
 * registered office.
 *
 * <p>Anexa 3 la HG 1061/2008 asks for it by name — the filled model writes the recipient as
 * "P.L. ILFOV, Şos. de Centura nr. 2-8, Bragadiru". Until 25.08.2026 a partner had exactly one such
 * address; a collector with three depots had to keep three partners or accept the wrong address on
 * the paper. Now the partner has as many as it has, and the movement says which one received the
 * load — because that is a fact about the transport, not about the partner.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "partner_work_points")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PartnerWorkPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    Partner partner;

    /** How the client calls it — "Depozit Florești", "P.L. Ilfov". Optional; the address is not. */
    String name;

    @Column(nullable = false, length = 500)
    String address;

    @Column(nullable = false)
    boolean active;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    /** What Anexa 3 prints: the name when there is one, then the address. */
    public String label() {
        return name == null || name.isBlank() ? address : name + ", " + address;
    }
}
