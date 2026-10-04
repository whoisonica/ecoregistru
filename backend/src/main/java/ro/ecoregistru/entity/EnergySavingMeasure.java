package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One of up to 20 energy-saving measures of a declaration (position 1-20). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "energy_saving_measures")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnergySavingMeasure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "declaration_id", nullable = false)
    EnergyDeclaration declaration;

    @Column(nullable = false)
    int position;

    @Column(length = 500)
    String name;

    @Column(name = "cost_estimated", precision = 14, scale = 3)
    BigDecimal costEstimated;

    @Column(name = "cost_actual", precision = 14, scale = 3)
    BigDecimal costActual;

    @Column(name = "savings_tep_estimated", precision = 14, scale = 3)
    BigDecimal savingsTepEstimated;

    @Column(name = "savings_tep_actual", precision = 14, scale = 3)
    BigDecimal savingsTepActual;

    @Column(name = "savings_cost_estimated", precision = 14, scale = 3)
    BigDecimal savingsCostEstimated;

    @Column(name = "savings_cost_actual", precision = 14, scale = 3)
    BigDecimal savingsCostActual;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
}
