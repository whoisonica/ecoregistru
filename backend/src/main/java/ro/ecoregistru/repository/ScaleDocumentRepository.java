package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.ScaleDocument;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScaleDocumentRepository extends JpaRepository<ScaleDocument, UUID> {

    List<ScaleDocument> findAllByScale_IdIn(Collection<UUID> scaleIds);

    Optional<ScaleDocument> findByIdAndScale_Id(UUID id, UUID scaleId);

    Optional<ScaleDocument> findByScale_IdAndScaleEventIdIsNull(UUID scaleId);

    Optional<ScaleDocument> findByScale_IdAndScaleEventId(UUID scaleId, UUID scaleEventId);

    List<ScaleDocument> findAllByScale_Id(UUID scaleId);
}
