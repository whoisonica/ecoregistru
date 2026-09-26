package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un rând din registrul formularelor primite al unui depozit (D2.6, V72). Append-only: nu se modifică, se corectează
 * cu un rând nou ({@link #correctsId}). Baza refuză orice UPDATE.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Immutable
@Table(name = "received_forms")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReceivedForm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "company_id", nullable = false)
    UUID companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_point_id", nullable = false)
    WorkPoint workPoint;

    @Column(name = "entry_no", nullable = false)
    int entryNo;

    @Column(name = "received_on", nullable = false)
    LocalDate receivedOn;

    /** ANEXA_3 sau ANEXA_2. */
    @Column(name = "form_kind", nullable = false, length = 16)
    String formKind;

    @Column(name = "form_series", length = 20)
    String formSeries;

    @Column(name = "form_number", nullable = false, length = 30)
    String formNumber;

    @Column(name = "form_date")
    LocalDate formDate;

    @Column(name = "sender_name", nullable = false)
    String senderName;

    @Column(name = "sender_cui", length = 20)
    String senderCui;

    @Column(name = "waste_description", length = 500)
    String wasteDescription;

    @Column(name = "quantity_kg", precision = 12, scale = 3)
    BigDecimal quantityKg;

    @Column(name = "weighing_operation_id")
    UUID weighingOperationId;

    @Column(name = "corrects_id")
    UUID correctsId;

    @Column(name = "correction_reason", length = 500)
    String correctionReason;

    @Column(name = "created_by", nullable = false)
    UUID createdBy;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
