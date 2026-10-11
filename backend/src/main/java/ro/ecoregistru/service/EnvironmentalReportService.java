package ro.ecoregistru.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.EnvironmentalPermitRequest;
import ro.ecoregistru.controller.request.EnvironmentalReportRequest;
import ro.ecoregistru.controller.response.EnvironmentalReportResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.EnvironmentalInspection;
import ro.ecoregistru.entity.EnvironmentalMonitoring;
import ro.ecoregistru.entity.EnvironmentalReport;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.exception.UnprocessableEntityException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.EnvironmentalInspectionRepository;
import ro.ecoregistru.repository.EnvironmentalMonitoringRepository;
import ro.ecoregistru.repository.EnvironmentalReportRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.export.EnvironmentalReportDocument;
import ro.ecoregistru.service.export.EnvironmentalReportDocxGenerator;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.ENV_REPORT_DUE_INVALID;
import static ro.ecoregistru.exception.ErrorMessageEnum.ENV_REPORT_YEAR_INVALID;

/**
 * The raport anual de mediu (RAM) of the current tenant (Andreea, 09.10.2026): the authorization it is written under,
 * the year's answers and lists, and the Word document. The waste and packaging tables are not stored here: they are
 * read, at render time, from the same builders as the annual waste declaration and Anexa 1 Ambalaje, so the RAM can
 * never disagree with them.
 */
@Service
@RequiredArgsConstructor
public class EnvironmentalReportService {

    private static final int FIRST_YEAR = 2020;

    /** What the authorization usually asks about the waste record, offered until the year has its own text. */
    public static final String DEFAULT_OTHER_REQUIREMENTS = "Evidența gestiunii deșeurilor se ține conform "
            + "HG 856/2002, lunar, pe fiecare tip de deșeu generat, și se transmite anual autorității de mediu.";

    private final CompanyRepository companyRepository;
    private final EnvironmentalReportRepository reportRepository;
    private final EnvironmentalInspectionRepository inspectionRepository;
    private final EnvironmentalMonitoringRepository monitoringRepository;
    private final WorkPointRepository workPointRepository;
    private final DeadlineService deadlineService;
    private final EvidenceCalculator evidenceCalculator;
    private final PackagingService packagingService;
    private final EnvironmentalReportDocxGenerator docxGenerator;

    @Transactional(readOnly = true)
    public EnvironmentalReportResponse get(int year) {
        checkYear(year);
        return build(company(), year);
    }

    @Transactional
    public EnvironmentalReportResponse save(EnvironmentalReportRequest request) {
        checkYear(request.year());
        Company company = company();
        EnvironmentalReport report = reportRepository.findByCompany_IdAndYear(company.getId(), request.year())
                .orElseGet(() -> EnvironmentalReport.builder().company(company).year(request.year()).build());
        report.setIncidents(request.incidents());
        report.setIncidentsText(blankToNull(request.incidentsText()));
        report.setComplaints(request.complaints());
        report.setComplaintsText(blankToNull(request.complaintsText()));
        report.setInspected(request.inspected());
        report.setDamage(request.damage());
        report.setDamageText(blankToNull(request.damageText()));
        report.setOtherRequirements(blankToNull(request.otherRequirements()));
        report.setUpdatedAt(Instant.now());
        report = reportRepository.save(report);

        // Delete first and flush, so the new positions 1..n never meet the old ones.
        inspectionRepository.deleteByReport_Id(report.getId());
        monitoringRepository.deleteByReport_Id(report.getId());
        inspectionRepository.flush();
        int position = 1;
        for (EnvironmentalReportRequest.Inspection i : nz(request.inspections())) {
            inspectionRepository.save(EnvironmentalInspection.builder()
                    .report(report).position(position++).workPoint(blankToNull(i.workPoint()))
                    .institution(blankToNull(i.institution())).reportNumber(blankToNull(i.reportNumber()))
                    .reportDate(i.reportDate()).measures(blankToNull(i.measures())).updatedAt(Instant.now())
                    .build());
        }
        position = 1;
        for (EnvironmentalReportRequest.Monitoring m : nz(request.monitorings())) {
            monitoringRepository.save(EnvironmentalMonitoring.builder()
                    .report(report).position(position++).workPoint(blankToNull(m.workPoint()))
                    .factor(blankToNull(m.factor())).performedOn(m.performedOn())
                    .testReportNumber(blankToNull(m.testReportNumber())).testReportDate(m.testReportDate())
                    .result(blankToNull(m.result())).updatedAt(Instant.now())
                    .build());
        }
        inspectionRepository.flush();
        return build(company, request.year());
    }

    /** The authorization and the two due days; the open RAM and visa deadlines move with them. */
    @Transactional
    public EnvironmentalReportResponse savePermit(int year, EnvironmentalPermitRequest request) {
        checkYear(year);
        validDay(request.ramDueMonth(), request.ramDueDay());
        if ((request.visaDueMonth() == null) != (request.visaDueDay() == null)) {
            throw new UnprocessableEntityException(ENV_REPORT_DUE_INVALID);
        }
        if (request.visaDueMonth() != null) {
            validDay(request.visaDueMonth(), request.visaDueDay());
        }
        Company company = company();
        company.setEnvironmentalAuthNumber(blankToNull(request.number()));
        company.setEnvironmentalAuthIssuer(blankToNull(request.issuer()));
        company.setEnvironmentalAuthIssuedOn(request.issuedOn());
        company.setEnvironmentalAuthRevisedOn(request.revisedOn());
        company.setEnvironmentalAuthVisaOn(request.visaOn());
        company.setRamDueMonth(request.ramDueMonth());
        company.setRamDueDay(request.ramDueDay());
        company.setVisaDueMonth(request.visaDueMonth());
        company.setVisaDueDay(request.visaDueDay());
        company.setRamCopyTo(blankToNull(request.copyTo()));
        company.setAdministratorName(blankToNull(request.administratorName()));
        companyRepository.saveAndFlush(company);
        deadlineService.reschedulePermitDeadlines(company);
        return build(company, year);
    }

    /**
     * The RAM of the year as {@code .docx}. Not {@code readOnly}: the waste table is read through
     * {@link EvidenceCalculator#annualDeclaration}, which may refresh its cached lines.
     */
    @Transactional
    public byte[] docx(int year) {
        checkYear(year);
        Company company = company();
        List<WorkPoint> workPoints = workPointRepository.findAllByCompany_IdAndActiveTrue(company.getId()).stream()
                .sorted(Comparator.comparing(WorkPoint::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return docxGenerator.render(new EnvironmentalReportDocument(company, year, build(company, year), workPoints,
                evidenceCalculator.annualDeclaration(year, null), packagingService.declaration(year)));
    }

    private EnvironmentalReportResponse build(Company company, int year) {
        Optional<EnvironmentalReport> stored = reportRepository.findByCompany_IdAndYear(company.getId(), year);
        Optional<MonthDay> computed = DeadlineService.computedVisaDue(company.getEnvironmentalAuthIssuedOn());
        EnvironmentalReportResponse.Permit permit = new EnvironmentalReportResponse.Permit(
                company.getEnvironmentalAuthNumber(), company.getEnvironmentalAuthExpiry(),
                company.getEnvironmentalAuthIssuer(), company.getEnvironmentalAuthIssuedOn(),
                company.getEnvironmentalAuthRevisedOn(), company.getEnvironmentalAuthVisaOn(),
                company.getRamDueMonth(), company.getRamDueDay(), company.getVisaDueMonth(), company.getVisaDueDay(),
                computed.map(MonthDay::getMonthValue).orElse(null), computed.map(MonthDay::getDayOfMonth).orElse(null),
                company.getRamCopyTo(), company.getAdministratorName());
        if (stored.isEmpty()) {
            return new EnvironmentalReportResponse(year, permit, null, null, null, null, null, null, null,
                    DEFAULT_OTHER_REQUIREMENTS, List.of(), List.of());
        }
        EnvironmentalReport r = stored.get();
        return new EnvironmentalReportResponse(year, permit, r.getIncidents(), r.getIncidentsText(),
                r.getComplaints(), r.getComplaintsText(), r.getInspected(), r.getDamage(), r.getDamageText(),
                r.getOtherRequirements(),
                inspectionRepository.findAllByReport_IdOrderByPositionAsc(r.getId()).stream()
                        .map(i -> new EnvironmentalReportResponse.Inspection(i.getWorkPoint(), i.getInstitution(),
                                i.getReportNumber(), i.getReportDate(), i.getMeasures()))
                        .toList(),
                monitoringRepository.findAllByReport_IdOrderByPositionAsc(r.getId()).stream()
                        .map(m -> new EnvironmentalReportResponse.Monitoring(m.getWorkPoint(), m.getFactor(),
                                m.getPerformedOn(), m.getTestReportNumber(), m.getTestReportDate(), m.getResult()))
                        .toList());
    }

    /** 30 February does not exist in any year; 29 February is let through (it exists one year in four). */
    private static void validDay(int month, int day) {
        try {
            MonthDay.of(month, day);
        } catch (DateTimeException e) {
            throw new UnprocessableEntityException(ENV_REPORT_DUE_INVALID);
        }
    }

    private Company company() {
        return companyRepository.findById(TenantContext.require())
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
    }

    private static void checkYear(int year) {
        if (year < FIRST_YEAR || year > DeadlineService.today().getYear()) {
            throw new UnprocessableEntityException(ENV_REPORT_YEAR_INVALID);
        }
    }

    private static <T> List<T> nz(List<T> list) {
        return list == null ? List.of() : list.stream().filter(Objects::nonNull).toList();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
