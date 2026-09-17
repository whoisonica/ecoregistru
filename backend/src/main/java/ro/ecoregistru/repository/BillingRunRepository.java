package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.BillingRun;

import java.util.Optional;
import java.util.UUID;

public interface BillingRunRepository extends JpaRepository<BillingRun, UUID> {

    Optional<BillingRun> findFirstByOrderByStartedAtDesc();
}
