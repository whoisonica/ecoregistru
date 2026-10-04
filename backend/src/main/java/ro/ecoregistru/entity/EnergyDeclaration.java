package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The year's answers on the energy declaration (SME, audit, POIM) and the filing receipt. A missing answer is
 * {@code null}, never a default. The receipt lives here, not in {@code attachments}, because there
 * {@code movement_id} is NOT NULL (same pattern as {@code scale_documents}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "energy_declarations")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnergyDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @Column(nullable = false)
    int year;

    Boolean sme;

    @Column(name = "audit_date")
    LocalDate auditDate;

    @Column(length = 255)
    String auditor;

    @Column(name = "audit_scope", columnDefinition = "TEXT")
    String auditScope;

    @Column(name = "audit_share_pct", precision = 5, scale = 2)
    BigDecimal auditSharePct;

    @Column(name = "poim_interest")
    Boolean poimInterest;

    @Column(name = "poim_project")
    Boolean poimProject;

    @Column(name = "receipt_public_id", length = 255)
    String receiptPublicId;

    @Column(name = "receipt_resource_type", length = 32)
    String receiptResourceType;

    @Column(name = "receipt_delivery_type", length = 32)
    String receiptDeliveryType;

    @Column(name = "receipt_format", length = 32)
    String receiptFormat;

    @Column(name = "receipt_file_name", length = 255)
    String receiptFileName;

    @Column(name = "receipt_size_bytes")
    Long receiptSizeBytes;

    @Column(name = "receipt_uploaded_at")
    Instant receiptUploadedAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
}
