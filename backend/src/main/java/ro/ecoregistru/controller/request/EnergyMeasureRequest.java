package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** One energy-saving measure; its position is the order it arrives in. */
public record EnergyMeasureRequest(
        @Size(max = 500) String name,
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal costEstimated,
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal costActual,
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal savingsTepEstimated,
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal savingsTepActual,
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal savingsCostEstimated,
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal savingsCostActual
) {}
