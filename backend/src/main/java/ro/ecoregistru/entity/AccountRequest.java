package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.AccountRequestStatus;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.WasteOperationCode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * What a prospective client answered on the intake form, before an account exists.
 *
 * <p>EcoRegistru is a closed register: there is no self-registration, so this is the only way in.
 * A request is not a login and grants nothing — it sits here until a PLATFORM_ADMIN turns it into
 * a {@link Company}, and it is kept afterwards as the paper trail behind that account's profile.
 *
 * <p>The questions are the profile of the company, in the order a client can answer them: who they
 * are, where they work, what they do with waste, and — only for a collector — what they transport
 * with. Approving copies them onto the company, which is why the field names match.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "account_requests")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    // --- Who they are ---

    @Column(name = "company_name", nullable = false)
    String companyName;

    @Column(nullable = false, length = 20)
    String cui;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false, length = 20)
    CompanyType companyType;

    /** The registered office. */
    @Column(name = "company_address", length = 500)
    String companyAddress;

    /**
     * The site where waste is actually produced. Asked separately because it is routinely a
     * different address, and the legal records are kept per work point, not per company.
     */
    @Column(name = "work_point_name")
    String workPointName;

    @Column(name = "work_point_address", length = 500)
    String workPointAddress;

    // --- Who to talk to ---

    @Column(name = "contact_name")
    String contactName;

    @Column(name = "contact_email", nullable = false)
    String contactEmail;

    @Column(name = "contact_phone", length = 50)
    String contactPhone;

    // --- The environmental authorization ---

    @Column(name = "environmental_auth_number")
    String environmentalAuthNumber;

    @Column(name = "environmental_auth_expiry")
    LocalDate environmentalAuthExpiry;

    // --- Asked only of a collector ---

    @Column(name = "transport_means", length = 500)
    String transportMeans;

    @Column(name = "transport_license_number")
    String transportLicenseNumber;

    @Column(name = "transport_license_expiry")
    LocalDate transportLicenseExpiry;

    // --- What happens to the waste ---

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "account_request_operation_codes",
            joinColumns = @JoinColumn(name = "account_request_id"))
    @Column(name = "operation_code", length = 10, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    Set<WasteOperationCode> operationCodes = new LinkedHashSet<>();

    /**
     * Free text, on purpose. The 842-entry nomenclator is behind authentication, and a client
     * writing "carton, folie, moloz" is more useful than a client guessing six-digit codes.
     * Support maps it to real codes when creating the account.
     */
    @Column(name = "waste_codes_text", length = 2000)
    String wasteCodesText;

    @Column(length = 2000)
    String notes;

    // --- Lifecycle ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    AccountRequestStatus status;

    /** Set when the request became an account. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_company_id")
    Company createdCompany;

    @Column(name = "handled_by")
    UUID handledBy;

    @Column(name = "handled_at")
    Instant handledAt;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
