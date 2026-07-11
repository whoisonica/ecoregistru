package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.AuditResultStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * The result of a checklist item for a specific company (prospect or client).
 * Phase 1.5: schema only for now.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_results")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    AuditChecklistTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AuditResultStatus status;

    @Column(length = 1000)
    String note;

    @Column(nullable = false)
    Instant createdAt;
}
