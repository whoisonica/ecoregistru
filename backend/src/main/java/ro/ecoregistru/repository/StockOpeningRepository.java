package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.StockOpening;
import ro.ecoregistru.enums.StockOpeningStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockOpeningRepository extends JpaRepository<StockOpening, UUID> {

    Optional<StockOpening> findByIdAndCompanyId(UUID id, UUID companyId);

    List<StockOpening> findAllByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    boolean existsByWorkPoint_IdAndStatus(UUID workPointId, StockOpeningStatus status);

    Optional<StockOpening> findFirstByWorkPoint_IdAndStatus(UUID workPointId, StockOpeningStatus status);

    @Query("select coalesce(max(o.number), 0) from StockOpening o where o.companyId = :companyId")
    int maxNumber(@Param("companyId") UUID companyId);
}
