package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.Reception;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Tenant-scoped access to the inbound half of the art. 48 register. */
public interface ReceptionRepository extends JpaRepository<Reception, UUID> {

    Optional<Reception> findByIdAndCompany_IdAndDeletedFalse(UUID id, UUID companyId);

    List<Reception> findAllByCompany_IdAndDeletedFalse(UUID companyId);

    List<Reception> findAllByCompany_IdAndDeletedFalseAndDateBetween(
            UUID companyId, LocalDate from, LocalDate to);

    /** Idempotency lookup for (future) offline sync. */
    Optional<Reception> findByCompany_IdAndClientGeneratedId(UUID companyId, UUID clientGeneratedId);
}
