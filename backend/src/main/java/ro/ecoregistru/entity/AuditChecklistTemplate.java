package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * A checklist item for the "control test" offered to prospects/clients.
 * Global template (not per tenant). Phase 1.5: schema only for now.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_checklist_templates")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditChecklistTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, length = 1000)
    String text;

    String category;

    @Column(nullable = false)
    int sortOrder;

    @Column(nullable = false)
    boolean active;
}
