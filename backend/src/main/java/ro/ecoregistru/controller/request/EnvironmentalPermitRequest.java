package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * The environmental authorization as the RAM prints it and as its two deadlines read it; written on the company. The
 * visa day is both or neither: neither = 60 days before the anniversary of {@code issuedOn}.
 */
public record EnvironmentalPermitRequest(
        @Size(max = 128) String number,
        @Size(max = 255) String issuer,
        LocalDate issuedOn,
        LocalDate revisedOn,
        LocalDate visaOn,
        @NotNull @Min(1) @Max(12) Integer ramDueMonth,
        @NotNull @Min(1) @Max(31) Integer ramDueDay,
        @Min(1) @Max(12) Integer visaDueMonth,
        @Min(1) @Max(31) Integer visaDueDay,
        @Size(max = 255) String copyTo,
        @Size(max = 255) String administratorName
) {}
