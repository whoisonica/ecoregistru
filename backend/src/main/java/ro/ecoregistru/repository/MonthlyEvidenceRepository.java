package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.MonthlyEvidence;

import java.util.List;
import java.util.UUID;

public interface MonthlyEvidenceRepository extends JpaRepository<MonthlyEvidence, UUID> {

    List<MonthlyEvidence> findByCompany_IdAndYear(UUID companyId, int year);

    /** Wipe a tenant's cached lines for a year before regenerating them from movements. */
    void deleteByCompany_IdAndYear(UUID companyId, int year);

    /**
     * Last year the tenant has cached lines for; null when nothing was ever generated. Bounds the
     * cascade that rebuilds the years after a corrected one.
     */
    @Query("select max(e.year) from MonthlyEvidence e where e.company.id = :companyId")
    Integer findMaxYear(@Param("companyId") UUID companyId);
}
