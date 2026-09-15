package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.WasteArticle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WasteArticleRepository extends JpaRepository<WasteArticle, UUID> {

    Optional<WasteArticle> findByIdAndCompany_Id(UUID id, UUID companyId);

    /** Tot catalogul, cu inactivele: ecranul de setări le arată ca să se poată reactiva. */
    @EntityGraph(attributePaths = "wasteCode")
    List<WasteArticle> findAllByCompany_IdOrderByNameAsc(UUID companyId);

    /** Aceeași regulă ca indexul `uq_waste_articles_name` (pe `lower(name)`), dar cu mesaj, nu cu 500. */
    boolean existsByCompany_IdAndNameIgnoreCase(UUID companyId, String name);

    boolean existsByCompany_IdAndNameIgnoreCaseAndIdNot(UUID companyId, String name, UUID id);
}
