package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.WorkPoint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkPointRepository extends JpaRepository<WorkPoint, UUID> {

    List<WorkPoint> findAllByCompany_Id(UUID companyId);

    List<WorkPoint> findAllByCompany_IdAndActiveTrue(UUID companyId);

    Optional<WorkPoint> findByIdAndCompany_Id(UUID id, UUID companyId);
}
