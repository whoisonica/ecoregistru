package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.CompanyType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A tenant company. The list is the platform-admin tenant switcher's source of truth
 * (only id/name are needed there), but the extra fields let the client-management screen
 * prefill its edit dialog without a separate detail endpoint.
 */
public record CompanyResponse(
        UUID id,
        String name,
        String cui,
        CompanyType type,
        boolean active,
        boolean afmObligation,
        String environmentalAuthNumber,
        LocalDate environmentalAuthExpiry,
        String address,
        String contactName,
        String contactEmail,
        String contactPhone
) {}
