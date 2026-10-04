package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import ro.ecoregistru.enums.EnergyCarrier;

import java.time.Instant;
import java.util.UUID;

/**
 * A row of Anexa 1 (energie) ticked as used by the company in one year; unticking keeps its months in
 * {@link EnergyConsumption}. A year without rows of its own reads the set of the latest earlier year that has one.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "energy_carriers_used")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnergyCarrierUsed {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @Column(nullable = false)
    int year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    EnergyCarrier carrier;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
}
