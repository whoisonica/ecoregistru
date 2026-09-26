package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.ScaleEvent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScaleEventRepository extends JpaRepository<ScaleEvent, UUID> {

    List<ScaleEvent> findAllByScale_IdOrderByDateDescCreatedAtDesc(UUID scaleId);

    List<ScaleEvent> findAllByScale_IdIn(Collection<UUID> scaleIds);

    Optional<ScaleEvent> findByIdAndScale_Id(UUID id, UUID scaleId);
}
