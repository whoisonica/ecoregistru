package ro.ecoregistru.controller.response;

import java.time.LocalDate;

/**
 * One year with energy data, for the audit-file tab. {@code filedOn} is the day the ENERGY_ANNUAL deadline was
 * ticked; {@code receiptOn} is the day the EfEnClima receipt was uploaded (Bucharest time), which counts as proof of
 * filing when the deadline has no ticked row — the 30.06.2026 deadline exists only as a computed past row.
 */
public record EnergyYearSummary(int year, int monthsComplete, boolean overThreshold, LocalDate filedOn,
                                LocalDate receiptOn, boolean hasReceipt) {}
