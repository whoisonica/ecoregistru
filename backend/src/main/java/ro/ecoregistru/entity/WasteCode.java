package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * A waste code from the European List of Waste (Lista Europeană a Deșeurilor).
 * Global nomenclator (NOT per tenant). Seeded via Flyway from a CSV.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "waste_codes")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WasteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    /** 6-digit code formatted as "NN NN NN". Unique. */
    @Column(nullable = false, unique = true)
    String code;

    @Column(nullable = false)
    String name;

    /** Hazardous waste (codes marked with '*' in the LED). */
    @Column(nullable = false)
    boolean hazardous;

    /**
     * The hazardous codes this non-hazardous code names, comma separated — the mirror pair of
     * OUG 92/2021 art. 8 alin. (2). Null when the code is not a mirror entry, and always null on a
     * hazardous code: the article conditions only the classification <em>as non-hazardous</em>.
     *
     * <p>Derived once, in {@code V37}, from the official name in Decision 2014/955/EU, which spells
     * the pair out ("altele decât cele specificate la 17 05 03*"). Not a hand-kept list — see the
     * migration for why the rule is "the name cites a hazardous code" rather than one phrase, and
     * for the single row that the hazardous filter saves from a false warning.
     */
    @Column(name = "mirror_of")
    String mirrorOf;

    /**
     * Code and name folded to lowercase ASCII, so a search for "deseuri" finds "deșeuri".
     * A generated column (V17): Postgres recomputes it on every write, which is why it is mapped
     * read-only here — Hibernate must never try to insert or update it.
     */
    @Column(name = "search_text", insertable = false, updatable = false)
    String searchText;
}
