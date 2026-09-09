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

    /**
     * The Cloudinary URL as returned at upload time.
     *
     * <p>Kept, but no longer handed to anyone: since 11-bis it is only the fallback for rows
     * uploaded before that change, which are {@code type=upload} and therefore publicly
     * deliverable. For anything uploaded since, the delivery URL is built and signed on demand
     * ({@link ro.ecoregistru.service.CloudinaryStorageService#signedUrl}) and never leaves the
     * backend — an authenticated asset's own {@code secure_url} is itself a permanent signed
     * link, so storing and forwarding it would recreate exactly the hole this replaced.
     */
    @Column(nullable = false)
    String url;

    /** Cloudinary public_id, needed to delete the asset and to build its delivery URL. */
    @Column(nullable = false)
    String publicId;

    /**
     * Cloudinary's three coordinates for the asset, needed to rebuild a signed URL server-side.
     * Null on rows predating 11-bis — that is the signal to fall back to {@link #url}.
     *
     * <p>{@code resourceType} is Cloudinary's own classification, not ours: {@code resource_type:
     * auto} files PDFs under {@code image}, which is why the account-level "deliver PDF and ZIP"
     * restriction applies to them.
     */
    String resourceType;

    /** {@code authenticated} since 11-bis; {@code upload} (public) for older rows, or null. */
    String deliveryType;

    /** Cloudinary's format ("pdf", "jpg"); part of the string the signature is computed over. */
    String format;

    String fileName;
    String contentType;

    @Column(nullable = false)
    Instant createdAt;
}
