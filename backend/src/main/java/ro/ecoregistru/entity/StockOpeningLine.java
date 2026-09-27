package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

/** D3.5 (V74) — un rând al notei de preluare: sortiment (opțional) × cod, kg. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stock_opening_lines")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockOpeningLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opening_id", nullable = false)
    StockOpening opening;

    @Column(name = "line_no", nullable = false)
    int lineNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    WasteArticle article;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "waste_code_id", nullable = false)
    WasteCode wasteCode;

    @Column(nullable = false, precision = 14, scale = 3)
    BigDecimal kg;
}
