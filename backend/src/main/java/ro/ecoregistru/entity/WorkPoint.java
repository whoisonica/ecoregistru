package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/**
 * A physical work point (punct de lucru) of a Company. Legal waste records are per work point.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "work_points")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    @Column(nullable = false)
    String name;

    String address;

    @Column(nullable = false)
    boolean active;

    @Column(nullable = false)
    Instant createdAt;
}
