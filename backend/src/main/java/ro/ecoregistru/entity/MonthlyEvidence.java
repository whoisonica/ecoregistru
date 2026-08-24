package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cached monthly evidence line of Anexa 1: totals for one (work point, month, waste code), over
 * the movements of the ANEXA_1 register only — goods taken over from third parties are kept out
 * by HG 856/2002 art. 2 alin. (1) and belong to the art. 48 register.
 *
 * <p>Fully regenerable from WasteMovement (movements are the source of truth). Quantities in KG.
 * A year always has 12 lines per (work point, code), including months with no activity: the form
 * is a 12-row table and the stock has to be readable in every row.
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

    /**
     * How much of {@link #totalGenerated} nobody recorded as generation: it follows from the
     * month's exits, because a quantity that left the site was necessarily produced there.
     *
     * <p>Anexa 1 cap. 1 heads its columns "Cantitatea de deşeuri Generate — din care: valorificată
     * | eliminată final | rămasă în stoc". The last three are parts of the first, so they cannot
     * exceed it. A client who records only handovers used to get a sheet reading generated 0,
     * recovered 300, stock −300; the specialist put it plainly on 25.08.2026 — "cum poţi să
     * valorifici ceva ce nu este generat?".
     *
     * <p>Kept apart from the recorded figure so the two can always be told from each other.
     */
    @Column(name = "implied_generated", nullable = false, precision = 14, scale = 3)
    @Builder.Default
    BigDecimal impliedGenerated = java.math.BigDecimal.ZERO;

    @Column(nullable = false, precision = 16, scale = 3)
    BigDecimal totalRecovered;

    @Column(nullable = false, precision = 16, scale = 3)
    BigDecimal totalDisposed;

    /**
     * How much of this month's recovered + disposed left the site as a handover rather than
     * through the company's own treatment. A memo column ("din care predat"), never a term of the
     * stock identity: a handover and the R/D operation it is reported under are two descriptions
     * of one physical exit, not two exits.
     */
    @Column(nullable = false, precision = 16, scale = 3)
    BigDecimal totalHandedOver;

    /**
     * Waste that left the site without an R/D code (handovers recorded before the code became
     * mandatory). It leaves the stock but enters neither official column — the line is incomplete
     * until someone fills the operation in. Guessing one would put a made-up figure on an official
     * form; see docs/surse-oficiale.md §1.2.
     */
    @Column(name = "total_unclassified_out", nullable = false, precision = 16, scale = 3)
    BigDecimal totalUnclassifiedOut;

    /**
     * At least one exit this month is still waiting for the recipient's weighbridge, so the totals
     * are provisional and the line cannot be reported as it stands.
     */
    @Column(name = "awaiting_weighing", nullable = false)
    boolean awaitingWeighing;

    /**
     * Cumulative closing stock at the end of this month — the "rămasă în stoc" column of
     * HG 856/2002 anexa nr. 1, cap. 1:
     * closingStock = previous month closingStock + generated − recovered − disposed − unclassifiedOut.
     * Cap. 1 has no "handed over" column, so a handover is already counted inside recovered or
     * disposed. Computed by the evidence engine (FAZA EVID).
     */
    @Column(name = "closing_stock", nullable = false, precision = 16, scale = 3)
    BigDecimal closingStock;

    @Column(nullable = false)
    Instant generatedAt;
}
