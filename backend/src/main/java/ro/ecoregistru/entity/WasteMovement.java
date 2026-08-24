package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ro.ecoregistru.enums.PhysicalState;
import ro.ecoregistru.enums.TransportDestination;
import ro.ecoregistru.enums.TransportMeans;
import ro.ecoregistru.enums.WasteDestination;
import ro.ecoregistru.enums.StorageType;
import ro.ecoregistru.enums.TreatmentMethod;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WasteRegister;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The central record: one movement of waste. Source of truth for evidences.
 * Soft-deleted (history matters for audits). Idempotent on clientGeneratedId
 * to make future offline mobile sync safe.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "waste_movements")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WasteMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_point_id", nullable = false)
    WorkPoint workPoint;

    @Column(nullable = false)
    LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "waste_code_id", nullable = false)
    WasteCode wasteCode;

    /**
     * How much left, in {@link #unit}. Null only when {@link #weighedAtUnloading} is set: a corner
     * shop has no weighbridge, so the collector weighs the load at the depot and the figure comes
     * back afterwards. The filled Anexa 3 model works exactly this way — its quantity is written in
     * by hand after weighing — and zero or an estimate would be a made-up number on an official
     * form and in the Anexa 1 stock.
     */
    @Column(precision = 14, scale = 3)
    BigDecimal quantity;

    /**
     * The load is weighed by the recipient at unloading, so the quantity is not known yet. The
     * movement is recorded, the transport form prints with the quantity cell empty, and the
     * monthly evidence line is reported incomplete until someone fills the weight in.
     */
    @Column(name = "weighed_at_unloading", nullable = false)
    boolean weighedAtUnloading;

    /**
     * Volume in cubic metres — the only measure available to whoever has no scale, and a rubric
     * the form itself carries ("17 mc" on the filled model). Never a substitute for the weight:
     * Anexa 1 is kept in kilograms.
     */
    @Column(name = "volume_m3", precision = 12, scale = 3)
    BigDecimal volumeM3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    WasteOperation operation;

    /**
     * Which legal register this quantity belongs to. Most of the time the operation decides
     * (COLLECTED is always the art. 48 register, everything else defaults to Anexa 1), which is
     * why {@link #applyDefaultRegister()} fills it in. It is stored explicitly because there is one
     * case the operation cannot decide: handing over, recovering or disposing of goods taken from
     * third parties stays in the art. 48 register and must never reach Anexa 1
     * (HG 856/2002 art. 2 alin. (1) — docs/surse-oficiale.md §1.1).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    WasteRegister register;

    /** Physical state of the waste (Anexa 1 identification field). Nullable — captured when known. */
    @Enumerated(EnumType.STRING)
    @Column(name = "physical_state")
    PhysicalState physicalState;

    /**
     * Anexa 1 cap. 2, "Stocare: Tipul" — what the waste sits in until it leaves. Nullable:
     * movements recorded before this slice have none, and the export leaves the cell empty rather
     * than inventing a container.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", length = 10)
    StorageType storageType;

    /**
     * Anexa 1 cap. 2, "Tratare: Modul" — what is done to the waste on site. The third column of
     * that chapter, "Scopul", is not stored: it follows from {@link #operationCode}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "treatment_method", length = 10)
    TreatmentMethod treatmentMethod;

    /** Anexa 1 cap. 2, "Transport: Mijlocul" — how the waste travelled (nota 4). */
    @Enumerated(EnumType.STRING)
    @Column(name = "transport_means", length = 10)
    TransportMeans transportMeans;

    /**
     * Anexa 1 cap. 2, "Transport: Destinaţia" — where it ends up (nota 5). Distinct from
     * {@link #transportDestinations}, which is the "Destinat:" box of Anexa 3: that one says what
     * a transport is for and takes several ticks, this one takes exactly one value.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "waste_destination", length = 10)
    WasteDestination wasteDestination;

    /**
     * R1–R13 (recovery) / D1–D15 (disposal): the operation this quantity undergoes, and — through
     * {@link WasteOperationCode#treatmentPurpose()} — the Anexa 1 cap. 1 column it lands in.
     * Required for every movement that takes waste off the site (RECOVERED, DISPOSED), because
     * cap. 3 and cap. 4 report the quantity together with its operation and its operator. When a
     * {@link #partner} is named, this is the operation that partner performs — handing waste to a
     * recycler is a RECOVERED performed by them, not an operation of its own. Null for GENERATED
     * and COLLECTED.
     *
     * <p>Nullable in the schema: movements recorded before the rule existed cannot be classified
     * retroactively, and guessing would put a made-up figure on an official form.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_code", length = 10)
    WasteOperationCode operationCode;

    /**
     * Who performed the operation, when it was not this company: the recycler the waste was handed
     * to, the landfill that took it. This is "agentul economic care efectuează operaţia" of Anexa 1
     * cap. 3 / cap. 4. Null means the company did it on its own site.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    Partner partner;

    /**
     * Which internal generator (section) the waste came from — printed as "Secţia" in Anexa 1
     * cap. 2. Nullable: movements recorded before the notion existed have none, and the export
     * leaves the cell empty rather than inventing a section.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_generator_id")
    InternalGenerator internalGenerator;

    // --- Anexa 3 la HG 1061/2008: the transport form printed from this movement ---

    /** "Data descărcării". {@link #date} is the loading date the form asks for above it. */
    @Column(name = "unload_date")
    LocalDate unloadDate;

    /**
     * Which of the recipient's work points took the load. Null means "the one they have", which is
     * the ordinary case and what every movement recorded before V23 means.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_work_point_id")
    PartnerWorkPoint partnerWorkPoint;

    /**
     * Who hauls the waste. Often the recipient itself, sometimes a separate carrier; null means
     * we transport it ourselves, and the form then prints our own details in that column.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_partner_id")
    Partner transportPartner;

    /** "Date de identificare delegat şi nr. de înmatriculare mijloc de transport". */
    @Column(name = "driver_name")
    String driverName;

    /**
     * How the driver is identified on the paper — an ID series and number, or a CNP. One free-text
     * rubric rather than a CNP column on purpose: a structured CNP would pull in the GDPR regime
     * that OUG 31/2011 imposes on the borderou de achiziţie (Etapa 9), and nothing here needs the
     * number as data.
     */
    @Column(name = "driver_identification", length = 100)
    String driverIdentification;

    @Column(name = "vehicle_registration", length = 50)
    String vehicleRegistration;

    /** The "Destinat:" ticks. More than one is normal — see {@link TransportDestination}. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "waste_movement_transport_destinations",
            joinColumns = @JoinColumn(name = "waste_movement_id"))
    @Column(name = "destination", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    Set<TransportDestination> transportDestinations = new LinkedHashSet<>();

    /** Allocated on the first generation and kept, so a reprint is the same document. */
    @Column(name = "anexa3_series", length = 20)
    String anexa3Series;

    @Column(name = "anexa3_number")
    Integer anexa3Number;

    /**
     * The unit this one transport form prints its quantity in, overriding the company's standing
     * choice. Null is the normal case: fall back to {@code Company.anexa3Unit}, and then to
     * {@link #unit}, the unit the quantity was recorded in.
     *
     * <p>Answer A3.4, 24.08.2026: "da, e bine să poată selecta la introducerea mişcării". The
     * form travels with the load, and two recipients can want two different units from the same
     * company. The figure is converted exactly, by moving the decimal point.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "anexa3_unit", length = 10)
    Unit anexa3Unit;

    /** Free text, e.g. aviz nr. */
    String documentReference;

    @Column(length = 1000)
    String notes;

    @OneToMany(mappedBy = "movement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Attachment> attachments = new ArrayList<>();

    /** Optional client-supplied UUID; unique per tenant. Enables idempotent create. */
    @Column(name = "client_generated_id")
    UUID clientGeneratedId;

    // --- audit / soft delete ---

    @Column(nullable = false)
    boolean deleted;

    Instant deletedAt;
    UUID deletedBy;

    @Column(nullable = false, updatable = false)
    UUID createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    Instant updatedAt;

    @Version
    Long version;

    /**
     * Fills in the register the operation implies, so a movement written straight through the
     * repository — the dev seeder, a test fixture — cannot land without one. Never overrides an
     * explicit choice; the service validates that the two agree.
     */
    @PrePersist
    @PreUpdate
    void applyDefaultRegister() {
        if (register == null) {
            register = operation == WasteOperation.COLLECTED
                    ? WasteRegister.ART_48
                    : WasteRegister.ANEXA_1;
        }
    }
}
