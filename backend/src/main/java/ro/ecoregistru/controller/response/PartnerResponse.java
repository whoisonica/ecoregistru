package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.PartnerType;

import java.time.LocalDate;
import java.util.UUID;

public record PartnerResponse(
        UUID id,
        String name,
        String cui,
        String authorizationNumber,
        LocalDate authorizationExpiry,
        PartnerType type,
        boolean active,
        /** True when the authorization expires within 60 days (drives the UI badge). */
        boolean expiringSoon
) {}
