package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.Partner;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    List<Partner> findAllByCompany_Id(UUID companyId);

    Optional<Partner> findByIdAndCompany_Id(UUID id, UUID companyId);

    /** Partners whose authorization expires on/before the cutoff (for expiry alerts). */
    List<Partner> findAllByActiveTrueAndAuthorizationExpiryNotNullAndAuthorizationExpiryLessThanEqual(LocalDate cutoff);
}
