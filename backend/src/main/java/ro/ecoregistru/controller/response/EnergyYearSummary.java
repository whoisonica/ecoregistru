package ro.ecoregistru.controller.response;

import java.time.LocalDate;

/** One year with energy data, for the audit-file tab. {@code filedOn} is the day the ENERGY_ANNUAL deadline was ticked. */
public record EnergyYearSummary(int year, int monthsComplete, boolean overThreshold, LocalDate filedOn,
                                boolean hasReceipt) {}
