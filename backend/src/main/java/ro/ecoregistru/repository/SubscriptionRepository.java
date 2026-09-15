package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
