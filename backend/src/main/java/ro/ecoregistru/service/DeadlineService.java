package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.CompleteDeadlineRequest;
import ro.ecoregistru.controller.response.DeadlineGenerationResponse;
import ro.ecoregistru.controller.response.DeadlineResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.DeadlineStatus;
import ro.ecoregistru.enums.ReportType;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ReportingDeadlineRepository;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.DEADLINE_NOT_FOUND;

/**
 * Legal reporting deadlines for the current tenant (FAZA TERMENE).
 *
 * Deadline rules (docs/legislatie.md §1, high confidence):
 *  - AFM_MONTHLY — the 25th of each month, covering the previous month. Generated ONLY for
 *    companies with {@code afmObligation}; AFM is not universal, so we never falsely alarm.
 *  - SIM_ANNUAL — 15 March, for the previous calendar year's data. Generated for every company.
 *
 * Generation is additive and idempotent (unique on company+type+due_date): it never deletes,
 * so completion state and warning flags survive a re-run. Effective status (OVERDUE) is derived
 * at read time from the due date, so the list is correct without waiting for the scheduler.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeadlineService {

    ReportingDeadlineRepository deadlineRepository;
    CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<DeadlineResponse> list(int year) {
        UUID tenantId = TenantContext.require();
        LocalDate today = LocalDate.now();
        return deadlineRepository.findAllByCompany_IdAndDueDateBetweenOrderByDueDateAsc(
                        tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .stream()
                .map(d -> toResponse(d, today))
                .toList();
    }

    /**
     * Ensures the tenant has all the deadlines that fall due within the given calendar year.
     * Returns the number of newly created deadlines (existing ones are left untouched).
     */
    @Transactional
    public DeadlineGenerationResponse regenerateYear(int year) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));

        int created = 0;

        // SIM annual: 15 March of this year (covers the previous year).
        created += createIfMissing(company, ReportType.SIM_ANNUAL, LocalDate.of(year, Month.MARCH, 15));

        // AFM monthly: the 25th of each month this year — only if the company owes AFM.
        if (company.isAfmObligation()) {
            for (Month month : Month.values()) {
                created += createIfMissing(company, ReportType.AFM_MONTHLY,
                        LocalDate.of(year, month, 25));
            }
        }

        return new DeadlineGenerationResponse(year, created);
    }

    @Transactional
    public DeadlineResponse complete(UUID id, CompleteDeadlineRequest request) {
        ReportingDeadline deadline = require(id);
        deadline.setStatus(DeadlineStatus.DONE);
        deadline.setCompletedAt(Instant.now());
        deadline.setCompletionNote(request != null ? request.note() : null);
        return toResponse(deadline, LocalDate.now());
    }

    @Transactional
    public DeadlineResponse reopen(UUID id) {
        ReportingDeadline deadline = require(id);
        deadline.setStatus(DeadlineStatus.UPCOMING);
        deadline.setCompletedAt(null);
        deadline.setCompletionNote(null);
        return toResponse(deadline, LocalDate.now());
    }

    private int createIfMissing(Company company, ReportType type, LocalDate dueDate) {
        if (deadlineRepository.existsByCompany_IdAndReportTypeAndDueDate(company.getId(), type, dueDate)) {
            return 0;
        }
        deadlineRepository.save(ReportingDeadline.builder()
                .company(company)
                .reportType(type)
                .dueDate(dueDate)
                .status(DeadlineStatus.UPCOMING)
                .warned7Days(false)
                .warned1Day(false)
                .createdAt(Instant.now())
                .build());
        return 1;
    }

    private ReportingDeadline require(UUID id) {
        UUID tenantId = TenantContext.require();
        return deadlineRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(DEADLINE_NOT_FOUND));
    }

    private DeadlineResponse toResponse(ReportingDeadline d, LocalDate today) {
        return new DeadlineResponse(
                d.getId(),
                d.getReportType(),
                d.getDueDate(),
                effectiveStatus(d, today),
                d.getCompletedAt(),
                d.getCompletionNote());
    }

    /** DONE if completed; otherwise OVERDUE once the due date has passed; else UPCOMING. */
    private DeadlineStatus effectiveStatus(ReportingDeadline d, LocalDate today) {
        if (d.getStatus() == DeadlineStatus.DONE) {
            return DeadlineStatus.DONE;
        }
        return d.getDueDate().isBefore(today) ? DeadlineStatus.OVERDUE : DeadlineStatus.UPCOMING;
    }
}
