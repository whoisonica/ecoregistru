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

public interface EnergyConsumptionRepository extends JpaRepository<EnergyConsumption, UUID> {

    List<EnergyConsumption> findAllByCompany_IdAndYear(UUID companyId, int year);

    Optional<EnergyConsumption> findByCompany_IdAndYearAndCarrierAndMonth(
            UUID companyId, int year, EnergyCarrier carrier, int month);

    /** Years with a consumption row or a declaration row, newest first. */
    @Query(value = """
            SELECT year FROM energy_consumptions WHERE company_id = :companyId
            UNION
            SELECT year FROM energy_declarations WHERE company_id = :companyId
            ORDER BY year DESC
            """, nativeQuery = true)
    List<Integer> yearsWithData(@Param("companyId") UUID companyId);
}
