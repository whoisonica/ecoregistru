package ro.ecoregistru.service.export;

import ro.ecoregistru.controller.response.EnvironmentalReportResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WorkPoint;
import java.util.List;

/**
 * Everything the RAM prints for one year: the company and its active work points, the year's answers, and the two
 * centralised tables it repeats — the annual waste declaration (one per work point) and Anexa 1 Ambalaje.
 */
public record EnvironmentalReportDocument(
        Company company,
        int year,
        EnvironmentalReportResponse report,
        List<WorkPoint> workPoints,
        List<AnnualDeclaration> waste,
        PackagingDeclaration packaging) {
}
