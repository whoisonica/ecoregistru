package ro.ecoregistru.controller.response;

import java.time.LocalDate;
import java.util.UUID;

/**
 * An analysis bulletin, as the screen and the dossier read it.
 *
 * <p>There is deliberately <b>no URL here</b>, for the same reason {@code AttachmentResponse} lost
 * its own at 11-bis: for an authenticated Cloudinary asset the signed URL <em>is</em> the
 * credential and it does not expire. The bytes come from
 * {@code GET /analysis-bulletins/{id}/continut}, which checks the tenant first.
 *
 * @param wasteCode the six-digit code, with the asterisk the act gives it when hazardous
 * @param issueDate the date on the laboratory's paper, not the day it was uploaded
 */
public record AnalysisBulletinResponse(
        UUID id,
        UUID wasteCodeId,
        String wasteCode,
        String wasteCodeName,
        boolean hazardous,
        LocalDate issueDate,
        String laboratory,
        String fileName
) {}
