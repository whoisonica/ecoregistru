package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.Inventory;
import ro.ecoregistru.enums.InventoryStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByIdAndCompanyId(UUID id, UUID companyId);

    List<Inventory> findAllByCompanyIdOrderByNumberDesc(UUID companyId);

    boolean existsByWorkPoint_IdAndStatusIn(UUID workPointId, Collection<InventoryStatus> statuses);

    @Query("select coalesce(max(i.number), 0) from Inventory i where i.companyId = :companyId")
    int maxNumber(@Param("companyId") UUID companyId);
}
