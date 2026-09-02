package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.PartnerType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PartnerResponse(
        UUID id,
        String name,
        String cui,
        String authorizationNumber,
        LocalDate authorizationExpiry,
        /** What they do with the waste; null means a pure haulage firm — see {@code carrier}. */
        PartnerType type,
        /** We hand waste over to them and we invoice them. */
        boolean client,
        /** They perform the service and they invoice us. */
        boolean supplier,
        /** They can haul the waste; drives the "Transportatori" group of the movement form. */
        boolean carrier,
        boolean active,
        /** True when the authorization expires within 60 days (drives the UI badge). */
        boolean expiringSoon,

        // --- What Anexa 3 prints about them ---
        String address,
        List<PartnerWorkPointResponse> workPoints,
        String tradeRegisterNumber,
        String transportLicenseNumber,
        LocalDate transportLicenseExpiry,
        List<DriverResponse> drivers
) {}
