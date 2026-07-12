package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.DeadlineStatus;
import ro.ecoregistru.enums.ReportType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportingDeadlineRepository extends JpaRepository<ReportingDeadline, UUID> {

    List<ReportingDeadline> findAllByCompany_IdAndDueDateBetweenOrderByDueDateAsc(
            UUID companyId, LocalDate from, LocalDate to);

    Optional<ReportingDeadline> findByIdAndCompany_Id(UUID id, UUID companyId);

    boolean existsByCompany_IdAndReportTypeAndDueDate(UUID companyId, ReportType reportType, LocalDate dueDate);

    /**
     * Cross-tenant query for the alert scheduler: deadlines in a given status whose due date
     * falls in [from, to]. Not tenant-scoped — the scheduler is a system job, not a request.
     */
    List<ReportingDeadline> findByStatusAndDueDateBetween(
            DeadlineStatus status, LocalDate from, LocalDate to);
}
