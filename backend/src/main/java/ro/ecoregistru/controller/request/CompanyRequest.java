package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.CompanyType;

import java.time.LocalDate;

/**
 * Create/update payload for a tenant company. Platform-admin only (see CompanyController).
 * CUI format is validated in the service (INVALID_CUI) so we can normalize it too.
 */
public record CompanyRequest(
        @NotBlank String name,
        @NotBlank String cui,
        @NotNull CompanyType type,
        boolean afmObligation,
        String environmentalAuthNumber,
        LocalDate environmentalAuthExpiry,
        String address,
        String contactName,
        @Email String contactEmail,
        String contactPhone
) {}
