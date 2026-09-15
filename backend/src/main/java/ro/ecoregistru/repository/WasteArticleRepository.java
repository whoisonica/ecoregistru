package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.WasteArticle;

import java.util.Optional;
import java.util.UUID;

public interface WasteArticleRepository extends JpaRepository<WasteArticle, UUID> {

    Optional<WasteArticle> findByIdAndCompany_Id(UUID id, UUID companyId);
}
