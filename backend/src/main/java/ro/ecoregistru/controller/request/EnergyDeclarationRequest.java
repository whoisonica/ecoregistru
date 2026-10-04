package ro.ecoregistru.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** The year's answers and measures. Replaces everything: a missing answer is stored as empty. */
public record EnergyDeclarationRequest(
        @NotNull Integer year,
        Boolean sme,
        LocalDate auditDate,
        @Size(max = 255) String auditor,
        String auditScope,
        @DecimalMin("0") @DecimalMax("100") BigDecimal auditSharePct,
        Boolean poimInterest,
        Boolean poimProject,
        @Valid @Size(max = 20) List<EnergyMeasureRequest> measures
) {}
