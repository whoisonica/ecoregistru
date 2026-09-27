package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import ro.ecoregistru.enums.InventoryKind;
import ro.ecoregistru.enums.InventoryStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * D3.5 (V74) — inventarul unui depozit (depozitul = gestiunea), după Normele OMFP 2861/2009: decizia (pct. 6),
 * declarația gestionarului (pct. 8 lit. a)), liniile (pct. 15, 35, 39) și procesul-verbal (pct. 42–43). Data de început
 * e data de referință a scripticului. La aprobare scrie liniile de ajustare ({@code inventory_id} pe mișcare).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "inventories")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "company_id", nullable = false)
    UUID companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_point_id", nullable = false)
    WorkPoint workPoint;

    @Column(nullable = false)
    int number;

    @Column(name = "decision_number", length = 50)
    String decisionNumber;

    @Column(name = "decision_date")
    LocalDate decisionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    InventoryKind kind;

    /** Un inventar complet de alt fel ține loc de inventarierea anuală (pct. 2 alin. (2)). */
    @Column(name = "counts_as_annual", nullable = false)
    boolean countsAsAnnual;

    /** „Modul de efectuare” (pct. 6 alin. (1)), separat de metodă. */
    String mode;

    String method;

    @Column(name = "starts_on", nullable = false)
    LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    LocalDate endsOn;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "inventory_commission", joinColumns = @JoinColumn(name = "inventory_id"))
    @OrderColumn(name = "position")
    @Builder.Default
    List<InventoryCommissionMember> commission = new ArrayList<>();

    @Column(name = "keeper_name", nullable = false, length = 200)
    String keeperName;

    /** La predare-primire, gestionarul care preia (pct. 33). */
    @Column(name = "receiving_keeper_name", length = 200)
    String receivingKeeperName;

    /** Reprezentantul gestionarului absent (pct. 8 lit. g)). */
    @Column(name = "keeper_representative", length = 200)
    String keeperRepresentative;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    KeeperDeclaration declaration;

    @Column(name = "declaration_date")
    LocalDate declarationDate;

    @Column(name = "last_entry_doc", length = 200)
    String lastEntryDoc;

    @Column(name = "last_exit_doc", length = 200)
    String lastExitDoc;

    @Column(name = "pv_date")
    LocalDate pvDate;

    @Column(name = "pv_causes")
    String pvCauses;

    @Column(name = "pv_measures")
    String pvMeasures;

    @Column(name = "pv_slow_stock")
    String pvSlowStock;

    @Column(name = "pv_storage_findings")
    String pvStorageFindings;

    @Column(name = "pv_other")
    String pvOther;

    @Column(name = "keeper_objections")
    String keeperObjections;

    @Column(name = "commission_conclusions")
    String commissionConclusions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    InventoryStatus status;

    @Column(name = "closed_on")
    LocalDate closedOn;

    @Column(name = "approved_on")
    LocalDate approvedOn;

    @Column(name = "cancel_reason", length = 500)
    String cancelReason;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo")
    @Builder.Default
    List<InventoryLine> lines = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Version
    long version;
}
