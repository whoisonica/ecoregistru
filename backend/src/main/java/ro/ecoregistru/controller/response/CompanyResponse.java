package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.WasteOperationCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A tenant company. The list is the platform-admin tenant switcher's source of truth
 * (only id/name are needed there), but the extra fields let the client-management screen
 * prefill its edit dialog without a separate detail endpoint.
 *
 * <p>It is also what every screen reads from {@code GET /api/v1/companies/current} to know what to
 * offer: the type decides the operations, the profile decides the R/D codes and the waste codes.
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
        String contactPhone,

        // --- the account profile ---
        /** Empty means the intake form has not been answered: nothing is narrowed. */
        Set<WasteOperationCode> authorizedOperationCodes,
        List<WasteCodeResponse> authorizedWasteCodes,
        String transportMeans,
        String transportLicenseNumber,
        LocalDate transportLicenseExpiry,
        String tradeRegisterNumber,
        String anexa3Series
) {}
