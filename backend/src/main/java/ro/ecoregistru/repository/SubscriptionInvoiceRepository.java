package ro.ecoregistru.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.SubscriptionPaymentMethod;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SubscriptionInvoiceRepository
        extends JpaRepository<SubscriptionInvoice, UUID>, JpaSpecificationExecutor<SubscriptionInvoice> {

    /** F-B2 — a page of the Facturare table, the payer fetched with it (no N+1). */
    @Override
    @EntityGraph(attributePaths = {"subscription", "subscription.company", "subscription.consultancy"})
    Page<SubscriptionInvoice> findAll(Specification<SubscriptionInvoice> spec, Pageable pageable);

    /** F-B — {@code [sum, count]} of the invoices in a status, optionally paid since an instant. */
    @Query("select coalesce(sum(i.total), 0), count(i) from SubscriptionInvoice i where i.status = :status"
            + " and (i.status <> ro.ecoregistru.enums.InvoiceStatus.PAID or i.paidAt >= :paidSince)")
    List<Object[]> sumAndCount(@Param("status") InvoiceStatus status, @Param("paidSince") Instant paidSince);

    boolean existsBySubscription_IdAndPeriodStart(UUID subscriptionId, LocalDate periodStart);

    boolean existsBySubscription_Id(UUID subscriptionId);

    List<SubscriptionInvoice> findAllBySubscription_IdOrderByPeriodStartDesc(UUID subscriptionId);

    /** F-B — each direct client's latest period, one query for the whole Clients table. */
    @Query("select i from SubscriptionInvoice i join fetch i.subscription s join fetch s.company"
            + " where i.periodStart = (select max(j.periodStart) from SubscriptionInvoice j where j.subscription = s)")
    List<SubscriptionInvoice> findLatestOfCompanies();

    @Query("select i.id from SubscriptionInvoice i where i.status = :status order by i.createdAt")
    List<UUID> findIdsByStatus(@Param("status") InvoiceStatus status);

    /** Issued in FGO (paid or not) and not yet mailed to the client. */
    @Query("select i.id from SubscriptionInvoice i where i.issuedAt is not null and i.emailedAt is null order by i.createdAt")
    List<UUID> findIdsToEmail();

    /** F3 — paid by card and not yet recorded in FGO: the run retries {@code factura/incasare}. */
    @Query("select i.id from SubscriptionInvoice i where i.status = :paid and i.paidBy = :card"
            + " and i.fgoCollectedAt is null and i.fgoNumar is not null order by i.createdAt")
    List<UUID> findIdsToRecordInFgo(@Param("paid") InvoiceStatus paid, @Param("card") SubscriptionPaymentMethod card);

    /** F3 — issued, unpaid, on a subscription paying by a saved card. */
    @Query("select i.id from SubscriptionInvoice i join i.subscription s where i.status = :issued"
            + " and s.paymentMethod = :card and s.cardToken is not null order by i.createdAt")
    List<UUID> findIdsToDebit(@Param("issued") InvoiceStatus issued, @Param("card") SubscriptionPaymentMethod card);

    /** F4 — issued, unpaid and past the due date: the reminders of §2.3. */
    @Query("select i.id from SubscriptionInvoice i where i.status = :issued and i.dueDate < :today order by i.createdAt")
    List<UUID> findIdsOverdue(@Param("issued") InvoiceStatus issued, @Param("today") LocalDate today);
}
