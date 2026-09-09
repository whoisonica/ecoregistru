package ro.ecoregistru.controller.response;

import java.util.UUID;

/**
 * An attachment as the API describes it — deliberately without a URL.
 *
 * <p>Until 11-bis this record carried Cloudinary's {@code secure_url}, which made every attachment
 * a public link: no session, no tenant check, no expiry. The file is now read through
 * {@code GET /api/v1/movements/{id}/attachments/{attachmentId}/continut}, which the client builds
 * from these two ids and which checks who is asking.
 */
public record AttachmentResponse(
        UUID id,
        String fileName,
        String contentType
) {}
