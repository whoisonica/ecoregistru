package ro.ecoregistru.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** The year's RAM answers and lists; a save replaces them all, like the energy declaration. */
public record EnvironmentalReportRequest(
        @NotNull Integer year,
        Boolean incidents,
        @Size(max = 10000) String incidentsText,
        Boolean complaints,
        @Size(max = 10000) String complaintsText,
        Boolean inspected,
        Boolean damage,
        @Size(max = 10000) String damageText,
        @Size(max = 10000) String otherRequirements,
        @Valid @Size(max = 50) List<Inspection> inspections,
        @Valid @Size(max = 50) List<Monitoring> monitorings
) {
    public record Inspection(
            @Size(max = 255) String workPoint,
            @Size(max = 255) String institution,
            @Size(max = 100) String reportNumber,
            LocalDate reportDate,
            @Size(max = 10000) String measures) {}

    public record Monitoring(
            @Size(max = 255) String workPoint,
            @Size(max = 255) String factor,
            LocalDate performedOn,
            @Size(max = 100) String testReportNumber,
            LocalDate testReportDate,
            @Size(max = 10000) String result) {}
}
