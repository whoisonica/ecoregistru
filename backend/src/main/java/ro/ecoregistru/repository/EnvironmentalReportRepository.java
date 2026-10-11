package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.EnvironmentalReport;
import java.util.Optional;
import java.util.UUID;

public interface EnvironmentalReportRepository extends JpaRepository<EnvironmentalReport, UUID> {

    Optional<EnvironmentalReport> findByCompany_IdAndYear(UUID companyId, int year);
}
