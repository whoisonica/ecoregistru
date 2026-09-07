package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.WasteMovement;

import java.time.Instant;
import java.time.LocalDate;
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

    /**
     * The highest Anexa 3 number this company has used. Numbers are allocated on first generation
     * and kept, so a reprint is the same document; a unique index backs the allocation up.
     */
    @Query("select max(m.anexa3Number) from WasteMovement m where m.company.id = :companyId")
    Integer findMaxAnexa3Number(@Param("companyId") UUID companyId);

    /** All live movements for a tenant within a date range — the evidence engine's input. */
    List<WasteMovement> findAllByCompany_IdAndDeletedFalseAndDateBetween(
            UUID companyId, LocalDate from, LocalDate to);

    /**
     * The last time anything dated on or before {@code until} changed. Feeds the staleness check
     * that decides whether the cached evidence of a year still describes the movements.
     *
     * <p>Two things about the query are deliberate. It has <b>no {@code deletedFalse} filter</b>:
     * deleting a movement invalidates the cache exactly as editing one does, and a soft delete
     * bumps {@code updatedAt} on a row that stays. And it looks at every year <b>up to</b> the one
     * asked about, not only that year: stock is cumulative, so a correction on a 2024 movement
     * makes the 2026 lines wrong through the opening balance they carry.
     */
    @Query("select max(m.updatedAt) from WasteMovement m "
            + "where m.company.id = :companyId and m.date <= :until")
    Instant findLastChangeUpTo(@Param("companyId") UUID companyId, @Param("until") LocalDate until);
}
