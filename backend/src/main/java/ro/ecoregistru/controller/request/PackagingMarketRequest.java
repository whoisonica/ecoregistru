package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import ro.ecoregistru.enums.PackagingMaterial;

import jakarta.validation.constraints.Digits;
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
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal salesPackaging,
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal primaryTotal,
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal primaryReusable,
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal secondaryTotal,
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal secondaryReusable,
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal hazardousContent
) {}
