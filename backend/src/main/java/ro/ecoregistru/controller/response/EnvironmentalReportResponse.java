package ro.ecoregistru.controller.response;

import java.time.LocalDate;
import java.util.List;

/**
 * The RAM of one year: the company's authorization (with the visa day computed from it), the year's answers and
 * lists. {@code otherRequirements} comes back with the default text while the year has none of its own.
 */
public record EnvironmentalReportResponse(
        int year,
        Permit permit,
        Boolean incidents,
        String incidentsText,
        Boolean complaints,
        String complaintsText,
        Boolean inspected,
        Boolean damage,
        String damageText,
        String otherRequirements,
        List<Inspection> inspections,
        List<Monitoring> monitorings
) {
    /**
     * @param visaComputedMonth the visa day counted from {@code issuedOn}, shown next to the field written by hand;
     *                          null without an issue date
     */
    public record Permit(
            String number, LocalDate expiry, String issuer, LocalDate issuedOn, LocalDate revisedOn,
            LocalDate visaOn, int ramDueMonth, int ramDueDay, Integer visaDueMonth, Integer visaDueDay,
            Integer visaComputedMonth, Integer visaComputedDay, String copyTo, String administratorName) {}

    public record Inspection(String workPoint, String institution, String reportNumber, LocalDate reportDate,
                             String measures) {}

    public record Monitoring(String workPoint, String factor, LocalDate performedOn, String testReportNumber,
                             LocalDate testReportDate, String result) {}
}
