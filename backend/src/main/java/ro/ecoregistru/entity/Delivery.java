package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperationCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The outbound half of the art. 48 register: goods taken over from third parties and passed on to
 * the next authorised operator. Together with {@link Reception} it carries what OUG 92/2021 art. 48
 * alin. (1) lit. a–c asks for — code, quantity, origin, destination, means of transport, the
 * treatment method envisaged, and the quantity entrusted for disposal.
 *
 * <p>Like a reception, a delivery stays out of Anexa 1 (HG 856/2002 art. 2 alin. (1)). Waste the
 * company generated itself and hands over is a {@link WasteMovement}, not a delivery.
 *
 * <p>Etapa 2 creates the seam only; the screens are Etapa 8.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "deliveries")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Delivery {

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

    /** The authorised operator the waste goes to — "destinaţia" of art. 48 alin. (1) lit. b. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_partner_id", nullable = false)
    Partner recipientPartner;

    /**
     * R1–R13 / D1–D15, the treatment the recipient performs — "metoda de tratare prevăzută pentru
     * deşeuri" (art. 48 alin. (1) lit. b). Nullable: it is required only "atunci când este relevant".
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_code", length = 10)
    WasteOperationCode operationCode;

    /** Delivery note / aviz number, as written on the paper document. */
    String documentReference;

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
