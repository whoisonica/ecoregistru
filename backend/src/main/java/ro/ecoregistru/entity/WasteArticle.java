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

    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
