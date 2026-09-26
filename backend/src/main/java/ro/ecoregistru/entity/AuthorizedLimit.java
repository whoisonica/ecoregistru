package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * F3 (D3.4, V73) — o limită din autorizația de mediu a unui depozit, tastată din PDF: felul (stocat / tratat / ieșit),
 * codul (sau toate), cantitatea cu unitatea, perioada, durata maximă de stocare (OUG 92/2021 art. 34 alin. (2)).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "authorized_limits")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthorizedLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "company_id", nullable = false)
    UUID companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_point_id", nullable = false)
    WorkPoint workPoint;

    /** STORED, TREATED, OUTPUT. */
    @Column(nullable = false, length = 16)
    String kind;

    /** Null = toate codurile depozitului. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waste_code_id")
    WasteCode wasteCode;

    @Column(nullable = false, precision = 14, scale = 3)
    BigDecimal quantity;

    /** T, KG, M3. */
    @Column(nullable = false, length = 4)
    String unit;

    /** AT_ONCE, MONTH, YEAR. */
    @Column(nullable = false, length = 8)
    String period;

    @Column(name = "max_storage_days")
    Integer maxStorageDays;

    @Column(nullable = false)
    boolean approximate;

    @Column(length = 500)
    String note;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
