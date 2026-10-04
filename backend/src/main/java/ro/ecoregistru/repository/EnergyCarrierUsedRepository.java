package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.EnergyCarrier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnergyCarrierUsedRepository extends JpaRepository<EnergyCarrierUsed, UUID> {

    List<EnergyCarrierUsed> findAllByCompany_Id(UUID companyId);

    @Modifying
    void deleteByCompany_IdAndCarrier(UUID companyId, EnergyCarrier carrier);
}
