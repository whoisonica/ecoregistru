package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ro.ecoregistru.entity.AnalysisBulletin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisBulletinRepository extends JpaRepository<AnalysisBulletin, UUID> {

    /**
     * Newest first: several bulletins on one code are its history, and the most recent one is the
     * one that answers "do you have the characterisation?".
     */
    List<AnalysisBulletin> findAllByCompany_IdOrderByIssueDateDescCreatedAtDesc(UUID companyId);

    Optional<AnalysisBulletin> findByIdAndCompany_Id(UUID id, UUID companyId);

    /**
     * Just the codes this company holds a bulletin for. Asked once per dossier build and once per
     * movement listing — never per row, which is why it returns the set rather than the entities.
     */
    @Query("select distinct b.wasteCode.code from AnalysisBulletin b where b.company.id = :companyId")
    List<String> findCoveredWasteCodes(UUID companyId);
}
