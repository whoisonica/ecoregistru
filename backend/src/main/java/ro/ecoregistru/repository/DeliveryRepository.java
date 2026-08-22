package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.Delivery;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Tenant-scoped access to the outbound half of the art. 48 register. */
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByIdAndCompany_IdAndDeletedFalse(UUID id, UUID companyId);

    List<Delivery> findAllByCompany_IdAndDeletedFalse(UUID companyId);

    List<Delivery> findAllByCompany_IdAndDeletedFalseAndDateBetween(
            UUID companyId, LocalDate from, LocalDate to);

    /** Idempotency lookup for (future) offline sync. */
    Optional<Delivery> findByCompany_IdAndClientGeneratedId(UUID companyId, UUID clientGeneratedId);
}
