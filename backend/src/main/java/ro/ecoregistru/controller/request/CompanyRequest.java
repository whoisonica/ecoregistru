package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.MarketRole;
import ro.ecoregistru.enums.WasteOperationCode;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Create/update payload for a tenant company. Platform-admin only (see CompanyController).
 * CUI format is validated in the service (INVALID_CUI) so we can normalize it too.
 *
 * <p>The last five fields are the account profile — the answers support transcribes from the
 * client's intake form. Both sets may be empty, and empty means "not answered yet": the screens
 * then offer everything rather than nothing.
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
        String contactPhone,

        /** The R/D operations this account works with; narrows the movement form. */
        Set<WasteOperationCode> authorizedOperationCodes,
        /**
         * Producător / importator / comerciant. Decides the packaging declaration and the AFM
         * packaging contribution — never the fişa de gestiune, which every generator keeps.
         */
        Set<MarketRole> marketRoles,
        /** The waste codes its authorization covers; narrows the code picker. */
        Set<UUID> authorizedWasteCodeIds,
        /** Asked of a collector: what it transports with, and its goods-transport licence. */
        String transportMeans,
        String transportLicenseNumber,
        LocalDate transportLicenseExpiry,

        /** Printed by Anexa 3 next to the CUI, and the series of this company's forms. */
        String tradeRegisterNumber,
        String anexa3Series
) {}
