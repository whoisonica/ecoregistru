package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ro.ecoregistru.entity.WasteMovement;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WasteMovementRepository
        extends JpaRepository<WasteMovement, UUID>, JpaSpecificationExecutor<WasteMovement> {

    Optional<WasteMovement> findByIdAndCompany_IdAndDeletedFalse(UUID id, UUID companyId);

    List<WasteMovement> findAllByCompany_IdAndDeletedFalse(UUID companyId);

    /** Idempotency lookup for (future) offline sync. */
    Optional<WasteMovement> findByCompany_IdAndClientGeneratedId(UUID companyId, UUID clientGeneratedId);

    /** Incremental fetch (?since=) support for delta sync. */
    List<WasteMovement> findAllByCompany_IdAndUpdatedAtGreaterThan(UUID companyId, Instant since);
}
