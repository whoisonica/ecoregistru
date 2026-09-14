package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.WorkPoint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkPointRepository extends JpaRepository<WorkPoint, UUID> {

    List<WorkPoint> findAllByCompany_Id(UUID companyId);

    List<WorkPoint> findAllByCompany_IdAndActiveTrue(UUID companyId);

    /** What a direct subscription bills on: every active work point after the first. */
    long countByCompany_IdAndActiveTrue(UUID companyId);

    Optional<WorkPoint> findByIdAndCompany_Id(UUID id, UUID companyId);

    List<WorkPoint> findAllByIdInAndCompany_Id(Collection<UUID> ids, UUID companyId);
}
