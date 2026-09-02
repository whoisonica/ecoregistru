package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.PartnerType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
 *   <li>{@link #type} — what they do with the waste (generator, collector, recoverer), plus the
 *       independent {@link #carrier} tick for "can also haul it";</li>
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

    /**
     * Printed by Anexa 3 la HG 1061/2008 under "Date de identificare destinatar" and, when this
     * partner does the hauling, under "Date de identificare transportator": the form asks for an
     * address and a trade-register number next to the CUI.
     */
    @Column(length = 500)
    String address;

    /**
     * Where the waste is actually unloaded, when that is not the registered office.
     *
     * <p>One partner, several depots: a collector receives a load at whichever of its work points
     * is nearest, and Anexa 3 names that one — "P.L. ILFOV, Şos. de Centura nr. 2-8, Bragadiru" on
     * the filled model. Which one received a given load is a fact about the transport, so the
     * movement points at it; this list is only what the partner has.
     */
    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    List<PartnerWorkPoint> workPoints = new ArrayList<>();

    /**
     * @deprecated superseded by {@link #workPoints} in V23, which moved the single address into
     *         the new table. Kept unread as a safety net until a later migration drops the column.
     */
    @Deprecated
    @Column(name = "work_point_address", length = 500)
    String workPointAddress;

    @Column(name = "trade_register_number", length = 50)
    String tradeRegisterNumber;

    /**
     * They can haul the waste themselves — the tick that puts them in the "Transportatori" group
     * of the movement form's carrier select and reveals the two licence fields below.
     *
     * <p>A tick and not a {@link PartnerType} value, for the same reason the commercial role is
     * two flags: the same firm is routinely a collector <em>and</em> a carrier, and an exclusive
     * enum would force it to exist twice. A pure haulage firm is the other case — it does nothing
     * with the waste, so {@link #type} is null and only this is set.
     */
    @Column(name = "is_carrier", nullable = false)
    boolean carrier;

    /**
     * His drivers, for the "Date de identificare delegat" rubric of Anexa 3. Only meaningful on a
     * carrier; our own drivers are the rows of the same table with no partner.
     */
    @OneToMany(mappedBy = "partner", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    List<Driver> drivers = new ArrayList<>();

    /** "Licenţa de transport mărfuri nepericuloase nr." + its expiry, for the carrier column. */
    @Column(name = "transport_license_number")
    String transportLicenseNumber;

    @Column(name = "transport_license_expiry")
    LocalDate transportLicenseExpiry;

    /** Authorization expiry date; alert when within 60 days. */
    LocalDate authorizationExpiry;

    /**
     * What they do with the waste. Nullable since V28: a pure haulage firm does nothing with it,
     * it moves it, and typing it "Colector" would be a guessed value on a printed rubric — the
     * audit file prints the column, and the Anexa 3 "Destinat:" ticks are prefilled from it. Null
     * is only allowed together with {@link #carrier}; the service enforces that.
     */
    @Enumerated(EnumType.STRING)
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
