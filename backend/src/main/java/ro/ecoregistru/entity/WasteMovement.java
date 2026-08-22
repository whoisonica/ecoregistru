package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ro.ecoregistru.enums.PhysicalState;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WasteRegister;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    @Column(nullable = false, precision = 14, scale = 3)
    BigDecimal quantity;

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
     * R1–R13 (recovery) / D1–D15 (disposal): the operation this quantity undergoes, and — through
     * {@link WasteOperationCode#treatmentPurpose()} — the Anexa 1 cap. 1 column it lands in.
     * Required for every movement that takes waste off the site (HANDED_OVER, RECOVERED, DISPOSED),
     * because cap. 3 and cap. 4 report the quantity together with its operation and its operator.
     * On HANDED_OVER the operation is the one the {@link #partner} performs; on the other two it is
     * the one this company performs itself. Null for GENERATED and COLLECTED.
     *
     * <p>Nullable in the schema: movements recorded before the rule existed cannot be classified
     * retroactively, and guessing would put a made-up figure on an official form.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_code", length = 10)
    WasteOperationCode operationCode;

    /** Required only for HANDED_OVER. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    Partner partner;

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
