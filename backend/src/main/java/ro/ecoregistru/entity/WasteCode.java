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
     * Code and name folded to lowercase ASCII, so a search for "deseuri" finds "deșeuri".
     * A generated column (V17): Postgres recomputes it on every write, which is why it is mapped
     * read-only here — Hibernate must never try to insert or update it.
     */
    @Column(name = "search_text", insertable = false, updatable = false)
    String searchText;
}
