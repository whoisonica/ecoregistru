package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/**
 * P2.13 — a consultancy: the environmental-consulting firm that keeps the records for several
 * companies. It owns the companies it manages ({@link Company#getConsultancy()}) and the
 * {@code CONSULTANT} accounts that work on them ({@link AppUser#getConsultancy()}).
 *
 * <p>Not a tenant. Nothing domain-shaped hangs off it; it only decides <i>which</i> tenants its
 * consultants may pick. See {@code V40} for why this is a firm and not a list on a person.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "consultancies")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Consultancy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String name;

    /** Same format and normalisation as {@link Company#getCui()}; the identity in the contract. */
    @Column(nullable = false, unique = true)
    String cui;

    @Column(nullable = false)
    Instant createdAt;
}
