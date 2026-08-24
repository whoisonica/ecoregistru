package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.PartnerWorkPoint;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerWorkPointRepository extends JpaRepository<PartnerWorkPoint, UUID> {

    List<PartnerWorkPoint> findAllByPartner_IdOrderByNameAsc(UUID partnerId);

    Optional<PartnerWorkPoint> findByIdAndPartner_Company_Id(UUID id, UUID companyId);
}
