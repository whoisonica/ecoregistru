package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.PackagingOrigin;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PartnerResponse(
        UUID id,
        String name,
        String cui,
        String authorizationNumber,
        LocalDate authorizationExpiry,
        LocalDate authorizationIssueDate,
        String visaDecisionNumber,
        LocalDate visaDecisionDate,
        LocalDate visaValidUntil,
        /** The earlier of the expiry and the end of the visa — the date every warning reads (V41). */
        LocalDate authorizationValidUntil,
        /** What they do with the waste; null means a pure haulage firm — see {@code carrier}. */
        PartnerType type,
        /** We hand waste over to them and we invoice them. */
        boolean client,
        /** They perform the service and they invoice us. */
        boolean supplier,
        /** They can haul the waste; drives the "Transportatori" group of the movement form. */
        boolean carrier,
        boolean active,
        /** Provenienţa they represent on anexa 3 ambalaje; null while unanswered. */
        PackagingOrigin packagingOrigin,
        /** True when {@code authorizationValidUntil} is within 60 days (drives the UI badge). */
        boolean expiringSoon,

        // --- What Anexa 3 prints about them ---
        String address,
        List<PartnerWorkPointResponse> workPoints,
        String tradeRegisterNumber,
        String transportLicenseNumber,
        LocalDate transportLicenseExpiry,
        List<DriverResponse> drivers
) {}
