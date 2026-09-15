package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PaymentMethod;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Operațiunea de depozit: capul numerotat care grupează liniile de cântar (V46). Liniile sunt
 * {@link WasteMovement}-uri cu {@code weighingOperation} setat, ca stocul și registrele existente
 * să le citească fără rescriere.
 *
 * <p>Poartă doar data, fără oră; ordinea din zi o dă {@link #number}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "weighing_operations")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WeighingOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    /** Depozitul în care se face operațiunea. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_point_id", nullable = false)
    WorkPoint workPoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    WeighingOperationType type;

    /** Pe firmă și pe tip; dat la creare, păstrat la anulare. */
    @Column(nullable = false)
    int number;

    @Column(nullable = false)
    LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    Partner partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "natural_person_id")
    NaturalPerson naturalPerson;

    /** „Originea” de la art. 48 alin. (1) lit. a). La o persoană fizică e mereu POPULATIE. */
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    PackagingOrigin origin;

    /** Șoferul ales din listă, dacă a fost ales. Documentele tipăresc totuși {@link #driverName}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    Driver driver;

    @Column(name = "driver_name")
    String driverName;

    @Column(name = "vehicle_registration", length = 50)
    String vehicleRegistration;

    @Column(name = "order_number", length = 60)
    String orderNumber;

    /** Mașina plină, pe toată operațiunea. */
    @Column(name = "gross_kg", precision = 14, scale = 3)
    BigDecimal grossKg;

    /** Mașina goală, pe toată operațiunea. */
    @Column(name = "tare_kg", precision = 14, scale = 3)
    BigDecimal tareKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 10)
    PaymentMethod paymentMethod;

    @Column(name = "receipt_number", length = 60)
    String receiptNumber;

    /** 2% AFM reținut la sursă, calculat la finalizare și păstrat. */
    @Column(name = "afm_contribution", precision = 16, scale = 2)
    BigDecimal afmContribution;

    /** Impozitul reținut la metalele de la PF, calculat la finalizare și păstrat. */
    @Column(name = "income_tax", precision = 16, scale = 2)
    BigDecimal incomeTax;

    /** Numărul borderoului PF, dat la prima tipărire și păstrat. */
    @Column(name = "borderou_number")
    Integer borderouNumber;

    /** Declarația „provin din gospodăria proprie”, obligatorie la metal de la PF. */
    @Column(name = "own_household")
    Boolean ownHousehold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    WeighingOperationStatus status;

    @Column(name = "finalized_at")
    Instant finalizedAt;

    @Column(name = "finalized_by")
    UUID finalizedBy;

    @Column(name = "cancelled_at")
    Instant cancelledAt;

    @Column(name = "cancelled_by")
    UUID cancelledBy;

    @Column(name = "cancel_reason", length = 1000)
    String cancelReason;

    @Column(length = 1000)
    String notes;

    @Column(name = "created_by", nullable = false, updatable = false)
    UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Version
    Long version;
}
