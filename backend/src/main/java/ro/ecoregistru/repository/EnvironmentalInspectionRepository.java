package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import ro.ecoregistru.entity.EnvironmentalInspection;
import java.util.List;
import java.util.UUID;

public interface EnvironmentalInspectionRepository extends JpaRepository<EnvironmentalInspection, UUID> {

    List<EnvironmentalInspection> findAllByReport_IdOrderByPositionAsc(UUID reportId);

    @Modifying
    void deleteByReport_Id(UUID reportId);
}
