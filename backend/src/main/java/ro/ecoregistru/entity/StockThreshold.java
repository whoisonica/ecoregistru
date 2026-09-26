package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** F3 (D3.3, V73) — pragul de stoc al unui sortiment într-un depozit: sub minim sau peste maxim, alertă. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stock_thresholds")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "company_id", nullable = false)
    UUID companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_point_id", nullable = false)
    WorkPoint workPoint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    WasteArticle article;

    @Column(name = "min_kg", precision = 14, scale = 3)
    BigDecimal minKg;

    @Column(name = "max_kg", precision = 14, scale = 3)
    BigDecimal maxKg;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
