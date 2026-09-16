package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/**
 * Un import din Excel salvat (V62). Mișcările lui poartă {@link WasteMovement#getImportBatchId()}, ca
 * importul să se poată anula întreg. Partenerii și punctele de lucru create de el nu se leagă: pot fi
 * folosite de atunci și pe mișcări scrise de mână, deci anularea nu le atinge.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "import_batches")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "company_id", nullable = false, updatable = false)
    UUID companyId;

    @Column(length = 255)
    String fileName;

    @Column(nullable = false, updatable = false)
    UUID createdBy;

    @Column(nullable = false, updatable = false)
    Instant createdAt;

    @Column(nullable = false)
    int workPointsNew;

    @Column(nullable = false)
    int partnersNew;

    @Column(nullable = false)
    int movementsNew;

    Instant undoneAt;
    UUID undoneBy;
}
