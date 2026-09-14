package ro.ecoregistru.controller.response;

import java.time.Instant;
import java.util.UUID;

/**
 * P2.13 — a consultancy, as the platform admin lists it. {@code companyCount} is also the figure
 * the contract bills on (art. 5.1: per managed company, counted at month end).
 */
public record ConsultancyResponse(
        UUID id,
        String name,
        String cui,
        long companyCount,
        long consultantCount,
        Instant createdAt
) {}
