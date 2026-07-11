package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.PartnerType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * An authorized collector/carrier a Company works with. Per tenant.
 * Authorization expiry drives an alert when it is within 60 days.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "partners")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @Column(nullable = false)
    String name;

    String cui;

    /** Authorization number (nr. autorizație). */
    String authorizationNumber;

    /** Authorization expiry date; alert when within 60 days. */
    LocalDate authorizationExpiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    PartnerType type;

    @Column(nullable = false)
    boolean active;

    @Column(nullable = false)
    Instant createdAt;
}
