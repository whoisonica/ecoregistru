package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.WasteOperationCode;

import java.time.LocalDate;
import java.util.Set;

/**
 * The intake form, as the client fills it in. Public — this is the only entry point into a closed
 * register, and it grants nothing: it creates a request, never an account.
 *
 * <p>Only four answers are required, because a form that refuses to be sent is a form nobody
 * sends: who you are (name + CUI), what kind of business (which decides the rest of the screen),
 * and an email to answer on. Everything else is what support will have to chase otherwise.
 */
public record AccountRequestSubmission(
        @NotBlank String companyName,
        @NotBlank String cui,
        @NotNull CompanyType companyType,
        String companyAddress,
        String workPointName,
        String workPointAddress,

        String contactName,
        @NotBlank @Email String contactEmail,
        String contactPhone,

        String environmentalAuthNumber,
        LocalDate environmentalAuthExpiry,

        /** Asked only of a collector; the form hides these for a plain generator. */
        String transportMeans,
        String transportLicenseNumber,
        LocalDate transportLicenseExpiry,

        Set<WasteOperationCode> operationCodes,
        String wasteCodesText,
        String notes
) {}
