package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import ro.ecoregistru.enums.EnergyCarrier;

import java.math.BigDecimal;

/** One cell of the year: a carrier in a month. A null quantity deletes the cell. */
public record EnergyConsumptionRequest(
        @NotNull Integer year,
        @NotNull EnergyCarrier carrier,
        @NotNull @Min(1) @Max(12) Integer month,
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal quantity,
        @PositiveOrZero @Digits(integer = 11, fraction = 4) BigDecimal tep
) {}
