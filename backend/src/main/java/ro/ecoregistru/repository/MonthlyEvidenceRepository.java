package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.MonthlyEvidence;

import java.util.List;
import java.util.UUID;

public interface MonthlyEvidenceRepository extends JpaRepository<MonthlyEvidence, UUID> {

    List<MonthlyEvidence> findByCompany_IdAndYear(UUID companyId, int year);

    /** Wipe a tenant's cached lines for a year before regenerating them from movements. */
    void deleteByCompany_IdAndYear(UUID companyId, int year);
}
