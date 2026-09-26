package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/**
 * Un fișier al cântarului (V70): buletinul unei verificări, sau — fără rând de istoric — dovada declarării la
 * BRML. Stocat în Cloudinary ca atașamentele mișcărilor, livrat doar prin API.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "scale_documents")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScaleDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scale_id", nullable = false)
    Scale scale;

    /** Null = dovada BRML a cântarului; altfel buletinul acestui rând de istoric. */
    @Column(name = "scale_event_id")
    UUID scaleEventId;

    @Column(name = "public_id", nullable = false)
    String publicId;

    @Column(name = "resource_type", nullable = false)
    String resourceType;

    @Column(name = "delivery_type", nullable = false)
    String deliveryType;

    String format;

    @Column(name = "file_name")
    String fileName;

    @Column(name = "content_type")
    String contentType;

    @Column(name = "size_bytes", nullable = false)
    long sizeBytes;

    @Column(name = "created_by", nullable = false, updatable = false)
    UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
}
