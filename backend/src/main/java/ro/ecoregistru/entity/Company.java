package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.WasteOperationCode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
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

    // --- The account profile: what this client answered on the intake form ---
    //
    // Both sets narrow what the screens offer, and both are allowed to be empty: an empty set
    // means "not answered yet", so nothing is hidden. Narrowing on an empty answer would take
    // away options an existing account is already using.

    /**
     * The R/D operations this account works with. A joinery that only hands cardboard to a
     * recycler answers R3 and never has to scroll past D7 "evacuare în mări şi oceane".
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "company_operation_codes",
            joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "operation_code", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    Set<WasteOperationCode> authorizedOperationCodes = new LinkedHashSet<>();

    /**
     * The waste codes the environmental authorization covers — "ce generez" for a generator,
     * "cu ce transport" for a collector. Feeds the code picker, so the 842-entry European List
     * stops being a haystack for an account that deals with four codes.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "company_waste_codes",
            joinColumns = @JoinColumn(name = "company_id"),
            inverseJoinColumns = @JoinColumn(name = "waste_code_id"))
    @Builder.Default
    Set<WasteCode> authorizedWasteCodes = new LinkedHashSet<>();

    /**
     * What a collector transports with, and its goods-transport licence. These three are asked
     * only of a collector, and they are also what the Anexa 3 handover form prints on the
     * carrier's side ("Licenţa de transport mărfuri" + its expiry date).
     */
    @Column(name = "transport_means", length = 500)
    String transportMeans;

    @Column(name = "transport_license_number")
    String transportLicenseNumber;

    @Column(name = "transport_license_expiry")
    LocalDate transportLicenseExpiry;

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
