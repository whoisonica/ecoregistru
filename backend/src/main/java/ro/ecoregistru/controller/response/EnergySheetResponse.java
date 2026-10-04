package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.EnergyCarrier;
import ro.ecoregistru.service.energy.EnergyYear.CarrierTotal;
import ro.ecoregistru.service.energy.EnergyYear.Cell;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The energy sheet of one year: ticked carriers, monthly cells, the computed totals, the answers and the contact.
 * {@code totalTep} adds the complete carriers; {@code knownTep} every entered month (the lower bound of the year),
 * and {@code overThreshold} is read on {@code knownTep}.
 */
public record EnergySheetResponse(
        int year,
        List<EnergyCarrier> carriers,
        List<Cell> cells,
        List<CarrierTotal> totals,
        BigDecimal totalTep,
        BigDecimal knownTep,
        int monthsComplete,
        boolean overThreshold,
        Declaration declaration,
        Contact contact,
        Receipt receipt
) {
    public record Declaration(
            Boolean sme, LocalDate auditDate, String auditor, String auditScope, BigDecimal auditSharePct,
            Boolean poimInterest, Boolean poimProject, List<Measure> measures) {}

    public record Measure(
            int position, String name, BigDecimal costEstimated, BigDecimal costActual,
            BigDecimal savingsTepEstimated, BigDecimal savingsTepActual,
            BigDecimal savingsCostEstimated, BigDecimal savingsCostActual) {}

    public record Contact(
            String fax, String website, String activitySector, String name, String email, String phone,
            String mobile, LocalDate attestedOn) {}

    public record Receipt(String fileName, long sizeBytes, Instant uploadedAt) {}
}
