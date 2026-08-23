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
        /** We hand waste over to them and we invoice them. */
        boolean client,
        /** They perform the service and they invoice us. */
        boolean supplier,
        boolean active,
        /** True when the authorization expires within 60 days (drives the UI badge). */
        boolean expiringSoon,

        // --- What Anexa 3 prints about them ---
        String address,
        String tradeRegisterNumber,
        String transportLicenseNumber,
        LocalDate transportLicenseExpiry
) {}
