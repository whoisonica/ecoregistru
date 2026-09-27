package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.CountMethod;
import ro.ecoregistru.enums.ShortageNature;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * D3.5 (V74) — un rând al listei de inventariere: sortiment × cod, scripticul fotografiat la deschidere, faptic, cum
 * s-a stabilit, explicația diferenței (pct. 39) și, la minus, natura lipsei.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "inventory_lines")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    Inventory inventory;

    @Column(name = "line_no", nullable = false)
    int lineNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    WasteArticle article;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "waste_code_id", nullable = false)
    WasteCode wasteCode;

    @Column(name = "book_kg", nullable = false, precision = 14, scale = 3)
    BigDecimal bookKg;

    @Column(name = "counted_kg", precision = 14, scale = 3)
    BigDecimal countedKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "count_method", length = 10)
    CountMethod countMethod;

    @Column(name = "technical_data")
    String technicalData;

    String explanation;

    @Enumerated(EnumType.STRING)
    @Column(name = "shortage_nature", length = 14)
    ShortageNature shortageNature;

    @Column(name = "responsible_person", length = 200)
    String responsiblePerson;

    /** Stoc depreciat, fără mișcare sau greu vandabil: pe listă separată (pct. 20). */
    @Column(name = "slow_moving", nullable = false)
    boolean slowMoving;

    /** Găsit fizic, lipsă din evidență: adăugat de mână, cu scriptic 0. */
    @Column(name = "added_manually", nullable = false)
    boolean addedManually;

    /** Faptic − scriptic; null cât nu s-a numărat. */
    public BigDecimal difference() {
        return countedKg == null ? null : countedKg.subtract(bookKg);
    }
}
