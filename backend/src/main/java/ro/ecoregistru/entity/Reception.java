package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ro.ecoregistru.enums.Unit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A quantity of waste taken over from a third party — the primary document of a collection centre,
 * sorting station or landfill. The design principle behind the depot module is that everything else
 * derives from this record instead of being typed twice: the art. 48 chronological register, the 2%
 * AFM contribution withheld at source (OUG 196/2005 art. 9 alin. (1) lit. a), the landfill reception
 * register (HG 349/2005 art. 15 alin. (1) lit. d) and the SIATD confirmation clock.
 *
 * <p><b>A reception never feeds Anexa 1.</b> HG 856/2002 art. 2 alin. (1) keeps goods taken from
 * third parties out of it — see {@link ro.ecoregistru.enums.WasteRegister} and
 * docs/surse-oficiale.md §1.1. It belongs to the art. 48 register by construction, which is why it
 * carries no register discriminator of its own.
 *
 * <p>Etapa 2 creates the seam only. The screens that write here are Etapa 8, and the COLLECTED
 * movements recorded before then move into this table once, at that point.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "receptions")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reception {

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

    /**
     * Who handed the waste over. Nullable on purpose: a scrap yard also buys from natural persons,
     * and that case is a borderou de achiziţie with a CNP on it (OUG 31/2011) — a separate record
     * under its own GDPR regime, built in Etapa 9. Until then only company suppliers are modelled.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_partner_id")
    Partner supplierPartner;

    /** Weighing note / delivery note number, as written on the paper document. */
    String documentReference;

    /**
     * Purchase price per unit, when the waste was bought rather than merely accepted. Feeds the 2%
     * AFM contribution a collection centre withholds at source, so it is money, not a quantity.
     */
    @Column(name = "unit_price", precision = 14, scale = 4)
    BigDecimal unitPrice;

    /** Total paid, kept as recorded rather than recomputed — the paper document is the truth. */
    @Column(name = "total_value", precision = 16, scale = 2)
    BigDecimal totalValue;

    @Column(length = 1000)
    String notes;

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
}
