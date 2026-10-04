package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.EnergyCarrier;

import java.util.List;

/**
 * The carriers (rows of Anexa 1) the company uses in one year. The set belongs to the year: it does not change the
 * other years. An empty list removes the year's own set, so the year reads the latest earlier one again.
 */
public record EnergyCarriersRequest(@NotNull Integer year, @NotNull List<@NotNull EnergyCarrier> carriers) {}
