package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import ro.ecoregistru.enums.PackagingMaterial;

import java.math.BigDecimal;

/**
 * One material row of tabelul 1 — the packaging put on the national market, in kilograms.
 *
 * <p>Every figure is optional, and null means "not answered": the form then prints an empty cell.
 * Sending a zero is a different statement — "nothing" — and it prints as 0.000.
 */
public record PackagingMarketRequest(
        @NotNull PackagingMaterial material,
        @NotNull Integer year,
        @PositiveOrZero BigDecimal salesPackaging,
        @PositiveOrZero BigDecimal primaryTotal,
        @PositiveOrZero BigDecimal primaryReusable,
        @PositiveOrZero BigDecimal secondaryTotal,
        @PositiveOrZero BigDecimal secondaryReusable,
        @PositiveOrZero BigDecimal hazardousContent
) {}
