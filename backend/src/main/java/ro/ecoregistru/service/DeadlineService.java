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

import ro.ecoregistru.enums.AfmContribution;
import ro.ecoregistru.enums.MarketRole;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.DEADLINE_NOT_FOUND;

/**
 * Legal reporting deadlines for the current tenant (FAZA TERMENE).
 *
 * Deadline rules (docs/legislatie.md §1, high confidence):
 *  - the Environment Fund deadlines — one cadence per contribution the company owes
 *    (monthly on the 25th, quarterly after each quarter, or once on 25 January). AFM is not
 *    universal and its rhythms differ, so nothing is generated for a company that owes nothing.
 *    See {@link #afmDeadlines}.
 *  - SIM_ANNUAL — 15 March, for the previous calendar year's data. Generated for every company:
 *    this is the Anexa 1 evidence being filed, and art. 1 alin. (1) HG 856/2002 binds anyone who
 *    generates waste. See {@link ReportType#SIM_ANNUAL} for why there is no separate Anexa 1 type.
 *  - PACKAGING_ANNUAL — 25 February, the packaging report of Ordinul 794/2012 art. 6, at the
 *    county environmental agency. Only for a company whose profile says it puts packaging on the
 *    national market; an unanswered profile gets nothing. See {@link #packagingDeadline}.
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

        created += afmDeadlines(company, year);
        created += packagingDeadline(company, year);

        return new DeadlineGenerationResponse(year, created);
    }

    /**
     * The packaging report of Ordinul 794/2012, due 25 February at the county environmental agency
     * (art. 1 and art. 6). Audit point 3, built 04.09.2026.
     *
     * <p>Only for a company that <em>says</em> it puts packaging on the national market. A profile
     * with no answer produces nothing: {@link MarketRole#putsPackagingOnMarket(java.util.Collection)}
     * already returns {@code false} for an empty set, and {@link MarketRole#answered} is asserted
     * next to it so the intent reads as a decision rather than as a side effect of the default.
     * A trader gets nothing either — it sells goods somebody else packaged, so it never introduced
     * the packaging and does not file (Legea 249/2015; see {@link MarketRole}).
     */
    private int packagingDeadline(Company company, int year) {
        Set<MarketRole> roles = company.getMarketRoles();
        if (!MarketRole.answered(roles) || !MarketRole.putsPackagingOnMarket(roles)) {
            return 0;
        }
        return createIfMissing(company, ReportType.PACKAGING_ANNUAL,
                LocalDate.of(year, Month.FEBRUARY, 25));
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

    /**
     * The Environment Fund deadlines, one cadence per contribution owed (OUG 196/2005 art. 11).
     *
     * <p>Before 24.08.2026 this generated a monthly deadline for anyone with the {@code
     * afmObligation} flag, whatever they actually owed — so a client whose only contribution is
     * the yearly packaging one received eleven wrong reminders a year. Now each contribution
     * brings its own rhythm:
     *
     * <ul>
     *   <li>the 2% withheld at source — monthly, the 25th;</li>
     *   <li>the circular-economy contribution — quarterly, the 25th after each quarter;</li>
     *   <li>the packaging contribution — once, on 25 January.</li>
     * </ul>
     *
     * <p>An account that has not answered which contributions it owes keeps the old behaviour
     * exactly: the flag alone still produces the twelve monthly deadlines. Switching an alert off
     * on an assumption is worse than leaving one that is too loud, and this way the legacy path
     * fades out as accounts are filled in rather than going quiet all at once.
     */
    private int afmDeadlines(Company company, int year) {
        Set<AfmContribution> owed = company.getAfmContributions();
        if (owed.isEmpty()) {
            if (!company.isAfmObligation()) {
                return 0;
            }
            int created = 0;
            for (Month month : Month.values()) {
                created += createIfMissing(company, ReportType.AFM_MONTHLY,
                        LocalDate.of(year, month, 25));
            }
            return created;
        }

        int created = 0;
        for (AfmContribution contribution : owed) {
            switch (contribution.getCadence()) {
                case MONTHLY -> {
                    for (Month month : Month.values()) {
                        created += createIfMissing(company, ReportType.AFM_MONTHLY,
                                LocalDate.of(year, month, 25));
                    }
                }
                // The 25th of the month after each quarter: April, July, October, January.
                case QUARTERLY -> {
                    for (Month month : List.of(Month.APRIL, Month.JULY, Month.OCTOBER,
                            Month.JANUARY)) {
                        created += createIfMissing(company, ReportType.AFM_QUARTERLY,
                                LocalDate.of(year, month, 25));
                    }
                }
                case ANNUAL -> created += createIfMissing(company, ReportType.AFM_ANNUAL,
                        LocalDate.of(year, Month.JANUARY, 25));
            }
        }
        return created;
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
