package ro.ecoregistru.controller.response;

import java.time.Instant;

/**
 * P2.14 — the consultancy's report header, as its consultants edit it. The logo itself is a separate
 * download ({@code GET /api/v1/consultancy/branding/logo}); {@code updatedAt} changes with it, so the
 * screen can ask for it again.
 */
public record ConsultancyBrandingResponse(
        String consultancyName,
        String headerLine,
        boolean hasLogo,
        Instant updatedAt
) {}
