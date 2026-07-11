package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.CompanyType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The tenant. Every domain record belongs to exactly one Company.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "companies")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String name;

    /** Romanian fiscal code (CUI/CIF), e.g. "RO12345678" or "12345678". Globally unique. */
    @Column(nullable = false, unique = true)
    String cui;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    CompanyType type;

    /** Environmental authorization number (autorizație de mediu), nullable. */
    String environmentalAuthNumber;

    /** Expiry date of the environmental authorization, nullable. */
    LocalDate environmentalAuthExpiry;

    String address;
    String contactName;
    String contactEmail;
    String contactPhone;

    @Column(nullable = false)
    boolean active;

    /**
     * Whether this company has a monthly AFM (Fondul pentru Mediu) obligation.
     * AFM is NOT universal — the monthly deadline is auto-generated only when this is true
     * (see docs/legislatie.md §1.D). Defaults to false so we never falsely alarm clients.
     */
    @Column(name = "afm_obligation", nullable = false)
    boolean afmObligation;

    @Column(nullable = false)
    Instant createdAt;
}
