package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.EnergyCarrier;

import java.util.List;

/** The carriers (rows of Anexa 1) the company uses; the set belongs to the company, not to a year. */
public record EnergyCarriersRequest(@NotNull List<@NotNull EnergyCarrier> carriers) {}
