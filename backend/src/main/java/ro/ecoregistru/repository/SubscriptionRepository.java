package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByCompany_Id(UUID companyId);

    Optional<Subscription> findByConsultancy_Id(UUID consultancyId);

    boolean existsByCompany_Id(UUID companyId);

    /** „X din 30" next to the founder box. */
    long countByFounderTrue();

    /** F2 — the subscriptions the daily run may invoice today. */
    List<Subscription> findAllByStatusNotAndStartedAtLessThanEqual(SubscriptionStatus status, LocalDate day);

    /** The ones the run skips because they start later, named back to whoever pressed the button. */
    List<Subscription> findAllByStatusNotAndStartedAtAfter(SubscriptionStatus status, LocalDate day);

    List<Subscription> findAllByStatusIn(Collection<SubscriptionStatus> statuses);

    /**
     * F4 — the status of whoever pays for a company: its own subscription, or its cabinet's. Empty when
     * nobody does, which is a client not billed and not restricted.
     */
    @Query("select s.status from Subscription s left join s.company c left join s.consultancy k"
            + " where c.id = :companyId"
            + " or k.id = (select co.consultancy.id from Company co where co.id = :companyId)")
    List<SubscriptionStatus> findStatusPayingFor(@Param("companyId") UUID companyId);

    /** F-B — the direct clients' subscriptions, for the Clients table (the company fetched with them). */
    @Query("select s from Subscription s join fetch s.company")
    List<Subscription> findAllOfCompanies();

    @Query("select s.status from Subscription s where s.consultancy.id = :consultancyId")
    List<SubscriptionStatus> findStatusOfConsultancy(@Param("consultancyId") UUID consultancyId);
}
