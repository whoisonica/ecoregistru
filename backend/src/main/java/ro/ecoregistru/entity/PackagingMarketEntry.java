package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.PackagingMaterial;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One material row of <b>tabelul 1</b> of Anexa 1 Ambalaje (Ordinul 794/2012): how much packaging
 * of that material the company put on the national market in a year, in kilograms.
 *
 * <p><b>Why this is stored and the rest of the declaration is not.</b> Tabelul 2 — the packaging
 * waste actually handed over — is computed from the movements, like every other document this
 * application prints. Tabelul 1 cannot be: it is about goods sold, not waste recorded, and nothing
 * in the system knows how many kilograms of steel packaging left with the products. So it is asked.
 *
 * <p>Every figure is nullable, and a missing row prints an empty cell rather than a zero — the same
 * rule as the CAEN code and the unweighed quantity. "Total (col. 3+5)" and the summary rows
 * (Total plastic, Total metal, TOTAL) are not here at all: they are sums, computed when the form is
 * drawn, because two stored copies of one figure are two figures that can disagree on a filed form.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "packaging_market_entries")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PackagingMarketEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @Column(nullable = false)
    int year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    PackagingMaterial material;

    /** Col. 1 — "Ambalaje de desfacere fabricate/importate". */
    @Column(name = "sales_packaging", precision = 14, scale = 3)
    BigDecimal salesPackaging;

    /** Col. 3 — "Ambalaje primare: Total". */
    @Column(name = "primary_total", precision = 14, scale = 3)
    BigDecimal primaryTotal;

    /** Col. 4 — "din care: ambalaj reutilizabil". */
    @Column(name = "primary_reusable", precision = 14, scale = 3)
    BigDecimal primaryReusable;

    /** Col. 5 — "Ambalaje secundare şi de transport: Total". */
    @Column(name = "secondary_total", precision = 14, scale = 3)
    BigDecimal secondaryTotal;

    /** Col. 6 — "din care: ambalaj reutilizabil". */
    @Column(name = "secondary_reusable", precision = 14, scale = 3)
    BigDecimal secondaryReusable;

    /** Col. 7 — "Ambalaje cu conţinut periculos", a part of col. 3 and counted again there. */
    @Column(name = "hazardous_content", precision = 14, scale = 3)
    BigDecimal hazardousContent;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
}
