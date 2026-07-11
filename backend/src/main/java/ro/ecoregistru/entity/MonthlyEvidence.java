package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cached monthly evidence line: totals per operation for one (work point, month, waste code).
 * Fully regenerable from WasteMovement (movements are the source of truth). Quantities in KG.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "monthly_evidences",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_evidence_scope",
                columnNames = {"company_id", "work_point_id", "year", "month", "waste_code_id"}))
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MonthlyEvidence {

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
    int year;

    @Column(nullable = false)
    int month;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "waste_code_id", nullable = false)
    WasteCode wasteCode;

    @Column(nullable = false, precision = 16, scale = 3)
    BigDecimal totalGenerated;

    @Column(nullable = false, precision = 16, scale = 3)
    BigDecimal totalCollected;

    @Column(nullable = false, precision = 16, scale = 3)
    BigDecimal totalHandedOver;

    @Column(nullable = false, precision = 16, scale = 3)
    BigDecimal totalRecovered;

    @Column(nullable = false, precision = 16, scale = 3)
    BigDecimal totalDisposed;

    /**
     * Cumulative closing stock at the end of this month (Anexa 1, Cap. 1).
     * closingStock = previous month closingStock + generated + collected − handedOver − recovered − disposed.
     * Computed by the evidence engine (FAZA EVID).
     */
    @Column(name = "closing_stock", nullable = false, precision = 16, scale = 3)
    BigDecimal closingStock;

    @Column(nullable = false)
    Instant generatedAt;
}
