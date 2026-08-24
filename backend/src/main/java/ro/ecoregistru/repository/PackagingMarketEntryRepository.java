package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.PackagingMarketEntry;
import ro.ecoregistru.enums.PackagingMaterial;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PackagingMarketEntryRepository extends JpaRepository<PackagingMarketEntry, UUID> {

    List<PackagingMarketEntry> findAllByCompany_IdAndYear(UUID companyId, int year);

    Optional<PackagingMarketEntry> findByCompany_IdAndYearAndMaterial(
            UUID companyId, int year, PackagingMaterial material);
}
