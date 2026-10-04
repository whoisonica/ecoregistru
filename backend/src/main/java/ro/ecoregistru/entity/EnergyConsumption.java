package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import ro.ecoregistru.enums.EnergyCarrier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One month of one carrier of Anexa 1 (energie). {@code tep} is written by hand only for coal and other fuels. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "energy_consumptions")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnergyConsumption {

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

    @Column(nullable = false)
    int month;

    @Column(nullable = false, precision = 14, scale = 3)
    BigDecimal quantity;

    @Column(precision = 14, scale = 4)
    BigDecimal tep;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;
}
