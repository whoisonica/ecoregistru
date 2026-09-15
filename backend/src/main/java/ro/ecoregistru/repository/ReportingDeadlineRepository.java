package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** P2.13, felia 2 — termenele nefinalizate ale unei firme; „depășit" se socotește la citire. */
    List<ReportingDeadline> findAllByCompany_IdAndStatusNotAndDueDateBetweenOrderByDueDateAsc(
            UUID companyId, DeadlineStatus status, LocalDate from, LocalDate to);

    /** P2.13, felia 2 — are firma termenele anului generate? Fără ele, „0 depășite" nu spune nimic. */
    boolean existsByCompany_IdAndDueDateBetween(UUID companyId, LocalDate from, LocalDate to);

    /**
     * P2.13, felia 2 — rezumatul zilnic al unui cabinet: termenele nefinalizate ale firmelor lui active,
     * cu firma adusă în aceeași interogare (mailul îi scrie numele).
     */
    @Query("select d from ReportingDeadline d join fetch d.company c "
            + "where c.consultancy.id = :consultancyId and c.active = true "
            + "and d.status <> ro.ecoregistru.enums.DeadlineStatus.DONE "
            + "and d.dueDate between :from and :to order by d.dueDate, c.name")
    List<ReportingDeadline> findOpenForConsultancy(@Param("consultancyId") UUID consultancyId,
                                                   @Param("from") LocalDate from, @Param("to") LocalDate to);
}
