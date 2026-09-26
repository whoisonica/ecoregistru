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

    /** Vehiculul ales din flotă (D2.1). Documentele tipăresc totuși {@link #vehicleRegistration}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    Vehicle vehicle;

    /** Cântarul folosit (D2.3, V68). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scale_id")
    Scale scale;

    /** Starea cântarului la finalizare ({@code ScaleLegality.State}); rămâne așa și dacă istoricul se completează. */
    @Column(name = "scale_state", length = 16)
    String scaleState;

    /** De ce s-a finalizat cu un cântar nelegal — confirmarea celui care aprobă. */
    @Column(name = "scale_override_reason", length = 1000)
    String scaleOverrideReason;

    // --- D2.5 (V71): transferul între depozitele firmei ---

    /** Depozitul de destinație; doar la TRANSFER (constrângere în bază). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_work_point_id")
    WorkPoint targetWorkPoint;

    /** Plecarea: de aici marfa e „în tranzit”. */
    @Column(name = "dispatched_at")
    java.time.Instant dispatchedAt;

    @Column(name = "dispatched_by")
    UUID dispatchedBy;

    /** Data recepției la destinație — data intrării în registrul depozitului B. */
    @Column(name = "received_on")
    java.time.LocalDate receivedOn;

    /** Cântarul depozitului B și starea lui la recepție, ca la plecare. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_scale_id")
    Scale receiptScale;

    @Column(name = "receipt_scale_state", length = 20)
    String receiptScaleState;

    @Column(name = "receipt_scale_override_reason", length = 500)
    String receiptScaleOverrideReason;

    @Column(name = "receipt_gross_kg", precision = 12, scale = 3)
    BigDecimal receiptGrossKg;

    @Column(name = "receipt_tare_kg", precision = 12, scale = 3)
    BigDecimal receiptTareKg;

    /** Cât pot diferi cele două cântare, la recepție (HG 710/2015, dublat în exploatare); null = necunoscut. */
    @Column(name = "tolerance_kg", precision = 12, scale = 3)
    BigDecimal toleranceKg;

    /** Primit − plecat, în kg. Negativ = lipsă la B. */
    @Column(name = "difference_kg", precision = 12, scale = 3)
    BigDecimal differenceKg;

    /** Peste toleranță: NIR-ul 14-3-1A și decizia comisiei (OMFP 2634/2015). */
    @Column(name = "nir_number", length = 40)
    String nirNumber;

    @Column(name = "difference_reason", length = 1000)
    String differenceReason;

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

    /** Valoarea pe care s-au calculat cei 2% AFM: toată intrarea, fără TVA (V51). */
    @Column(name = "afm_base", precision = 16, scale = 2)
    BigDecimal afmBase;

    /** 2% AFM reținut la sursă, calculat la finalizare și păstrat. */
    @Column(name = "afm_contribution", precision = 16, scale = 2)
    BigDecimal afmContribution;

    /** Valoarea pe care s-a calculat impozitul: doar liniile de metal ale unei intrări PF (V51). */
    @Column(name = "income_tax_base", precision = 16, scale = 2)
    BigDecimal incomeTaxBase;

    /** Impozitul reținut la metalele de la PF, calculat la finalizare și păstrat. */
    @Column(name = "income_tax", precision = 16, scale = 2)
    BigDecimal incomeTax;

    /** Numărul borderoului PF, dat la prima tipărire și păstrat. */
    @Column(name = "borderou_number")
    Integer borderouNumber;

    /** D1.13 — seria și numărul Anexei 3, alocate la prima tipărire și păstrate (V52). */
    @Column(name = "anexa3_series", length = 20)
    String anexa3Series;

    @Column(name = "anexa3_number")
    Integer anexa3Number;

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
