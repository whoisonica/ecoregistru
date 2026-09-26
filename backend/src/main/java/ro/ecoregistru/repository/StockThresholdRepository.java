package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.StockThreshold;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockThresholdRepository extends JpaRepository<StockThreshold, UUID> {

    List<StockThreshold> findAllByCompanyIdAndWorkPoint_Id(UUID companyId, UUID workPointId);

    Optional<StockThreshold> findByWorkPoint_IdAndArticle_Id(UUID workPointId, UUID articleId);

    Optional<StockThreshold> findByIdAndCompanyId(UUID id, UUID companyId);
}
