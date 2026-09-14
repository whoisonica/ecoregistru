package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.InvoiceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoice, UUID> {

    boolean existsBySubscription_IdAndPeriodStart(UUID subscriptionId, LocalDate periodStart);

    boolean existsBySubscription_Id(UUID subscriptionId);

    List<SubscriptionInvoice> findAllBySubscription_IdOrderByPeriodStartDesc(UUID subscriptionId);

    @Query("select i.id from SubscriptionInvoice i where i.status = :status order by i.createdAt")
    List<UUID> findIdsByStatus(@Param("status") InvoiceStatus status);

    /** Issued in FGO (paid or not) and not yet mailed to the client. */
    @Query("select i.id from SubscriptionInvoice i where i.issuedAt is not null and i.emailedAt is null order by i.createdAt")
    List<UUID> findIdsToEmail();
}
