package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.PartnerType;

import java.time.LocalDate;

/**
 * Create/update payload for a partner. {@code client} and {@code supplier} are the commercial
 * role and at least one has to be set — the service rejects a partner with neither, because a
 * screen that colours rows by role cannot show a row that has none.
 */
public record PartnerRequest(
        @NotBlank String name,
        String cui,
        String authorizationNumber,
        LocalDate authorizationExpiry,
        @NotNull PartnerType type,
        boolean client,
        boolean supplier
) {}
