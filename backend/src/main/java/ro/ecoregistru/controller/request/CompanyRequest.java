package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.AfmContribution;
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
import ro.ecoregistru.enums.Unit;

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
        /**
         * Which Environment Fund contributions it owes, each with its own rhythm. Empty (or
         * absent) leaves the answer unmade, and the legacy {@code afmObligation} flag keeps
         * producing the monthly deadline it always did.
         */
        Set<AfmContribution> afmContributions,
        /** The waste codes its authorization covers; narrows the code picker. */
        Set<UUID> authorizedWasteCodeIds,
        /** Asked of a collector: what it transports with, and its goods-transport licence. */
        String transportMeans,
        String transportLicenseNumber,
        LocalDate transportLicenseExpiry,

        /** Printed by Anexa 3 next to the CUI, and the series of this company's forms. */
        String tradeRegisterNumber,
        String anexa3Series,

        /**
         * The header of the annual declaration: the CAEN activity code, and the job title of the
         * person who signs it. Both may be null - the rubric then prints empty rather than guessed.
         */
        String caenCode,
        Unit anexa3Unit,
        String contactRole,

        /**
         * The person designated for waste management (OUG 92/2021 art. 23 alin. (4)-(5)). Distinct
         * from {@code contactRole}, which is the annual declaration's signature block.
         */
        String wasteManagerName,
        String wasteManagerRole,
        Boolean wasteManagerExternal,
        String wasteManagerTraining
) {}
