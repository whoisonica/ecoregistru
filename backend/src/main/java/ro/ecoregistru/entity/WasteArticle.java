package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/**
 * Un sortiment al firmei: denumirea comercială („Cupru”, „Carton balotat”) legată de un cod LER
 * (V46). Pe registre se tipărește codul, iar pe ecran și pe borderou, sortimentul.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "waste_articles")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WasteArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "waste_code_id", nullable = false)
    WasteCode wasteCode;

    @Column(nullable = false, length = 160)
    String name;

    /**
     * Deșeu metalic feros sau neferos: cere borderoul cu CNP (OUG 31/2011 art. 1) și impozitul
     * reținut la PF (Codul fiscal art. 114 alin. (2) lit. m²)). Propus din cod, confirmat de om.
     */
    @Column(nullable = false)
    boolean metal;

    /** Ce OUG 31/2011 art. 1 alin. (1) interzice să fie cumpărat de la o persoană fizică. */
    @Column(name = "forbidden_from_individuals", nullable = false)
    boolean forbiddenFromIndividuals;

    @Column(nullable = false)
    boolean active;

    /**
     * F5 — un sortiment balotat („Carton balotat”) se face din altul, vrac, cu același cod de deșeu (regula ANPM 2022:
     * balotarea unui singur flux păstrează codul). Null = sortiment obișnuit.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_article_id")
    WasteArticle sourceArticle;

    /** F5 — cât cântărește un balot, de regulă; operatorul scrie doar câți baloți a făcut. Doar împreună cu sursa. */
    @Column(name = "bale_weight_kg", precision = 10, scale = 3)
    java.math.BigDecimal baleWeightKg;

    /** F5 — din sortimentul ăsta se fac baloți: are sursa și greutatea balotului. */
    public boolean isBaled() {
        return sourceArticle != null && baleWeightKg != null;
    }

    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
