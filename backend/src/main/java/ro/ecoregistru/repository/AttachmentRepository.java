package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.Attachment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    Optional<Attachment> findByIdAndMovement_Id(UUID id, UUID movementId);

    /**
     * Every attachment of a year's live movements, in one statement — for the control dossier,
     * which used to read them movement by movement (BUG-016). Filtered by date, not by a list of
     * movement ids, so a large year cannot run into Postgres's limit on bound parameters.
     */
    @Query("""
            select a from Attachment a
            where a.movement.company.id = :companyId and a.movement.deleted = false
              and a.movement.date between :from and :to
            order by a.createdAt, a.id""")
    List<Attachment> findAllOfLiveMovementsBetween(@Param("companyId") UUID companyId,
                                                   @Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);
}
