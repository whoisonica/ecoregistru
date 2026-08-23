package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.AccountRequestStatus;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.MarketRole;
import ro.ecoregistru.enums.WasteOperationCode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** An intake request, as PLATFORM_ADMIN reads it. Never returned to the public endpoint. */
public record AccountRequestResponse(
        UUID id,
        String companyName,
        String cui,
        CompanyType companyType,
        String companyAddress,
        String workPointName,
        String workPointAddress,
        String contactName,
        String contactEmail,
        String contactPhone,
        String environmentalAuthNumber,
        LocalDate environmentalAuthExpiry,
        String transportMeans,
        String transportLicenseNumber,
        LocalDate transportLicenseExpiry,
        Set<MarketRole> marketRoles,
        Set<WasteOperationCode> operationCodes,
        String wasteCodesText,
        String notes,
        AccountRequestStatus status,
        UUID createdCompanyId,
        Instant handledAt,
        Instant createdAt
) {}
