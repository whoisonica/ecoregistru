package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ro.ecoregistru.enums.StockOpeningSource;
import ro.ecoregistru.enums.StockOpeningStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * D3.5 (V74) — nota de preluare a soldurilor unui depozit: stocul care exista înainte ca firma să-și țină evidența în
 * aplicație, luat din fișele de magazie sau din analiticul contabil la data de tăiere. Nu e inventar (vezi V74).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stock_openings")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockOpening {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "company_id", nullable = false)
    UUID companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_point_id", nullable = false)
    WorkPoint workPoint;

    /** Dat la confirmare; ciorna n-are număr. */
    Integer number;

    @Column(name = "cut_off_date", nullable = false)
    LocalDate cutOffDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    StockOpeningSource source;

    @Column(name = "keeper_name", length = 200)
    String keeperName;

    @Column(name = "accountant_name", length = 200)
    String accountantName;

    String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    StockOpeningStatus status;

    @Column(name = "confirmed_on")
    LocalDate confirmedOn;

    @OneToMany(mappedBy = "opening", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo")
    @Builder.Default
    List<StockOpeningLine> lines = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Version
    long version;
}
