package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/**
 * A file attached to a WasteMovement (e.g. handover document photo). Stored on Cloudinary.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "attachments")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movement_id", nullable = false)
    WasteMovement movement;

    /** Cloudinary secure URL. */
    @Column(nullable = false)
    String url;

    /** Cloudinary public_id, needed to delete the asset. */
    @Column(nullable = false)
    String publicId;

    String fileName;
    String contentType;

    @Column(nullable = false)
    Instant createdAt;
}
