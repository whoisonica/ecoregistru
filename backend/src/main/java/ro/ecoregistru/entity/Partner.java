package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.PartnerType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A company this tenant works with. Per tenant. Authorization expiry drives an alert when it is
 * within 60 days.
 *
 * <p>A partner carries two independent facts, and the split is what the meeting of 23.08.2026
 * asked for:
 *
 * <ul>
 *   <li>{@link #type} — what they are authorised to do with waste (collector, carrier, both);</li>
 *   <li>{@link #client} / {@link #supplier} — which way the invoice travels. A <em>client</em>
 *       takes our waste and we invoice them (the cardboard we sell). A <em>supplier</em> provides
 *       the service, gives us the traceability documents and invoices us (the mixed waste we pay
 *       to have taken away).</li>
 * </ul>
 *
 * The two commercial roles are separate flags rather than one enum because the same partner is
 * routinely both: we sell them cardboard and buy a bin-emptying service from them.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "partners")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @Column(nullable = false)
    String name;

    String cui;

    /** Authorization number (nr. autorizație). */
    String authorizationNumber;

    /** Authorization expiry date; alert when within 60 days. */
    LocalDate authorizationExpiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PartnerType type;

    /** We hand waste over to them and we invoice them. */
    @Column(name = "is_client", nullable = false)
    boolean client;

    /** They perform the service and they invoice us. */
    @Column(name = "is_supplier", nullable = false)
    boolean supplier;

    @Column(nullable = false)
    boolean active;

    @Column(nullable = false)
    Instant createdAt;
}
