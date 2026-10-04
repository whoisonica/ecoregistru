package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.EnergyCarrierUsed;

import java.util.List;
import java.util.UUID;

public interface EnergyCarrierUsedRepository extends JpaRepository<EnergyCarrierUsed, UUID> {

    /** The year's own rows. */
    List<EnergyCarrierUsed> findAllByCompany_IdAndYear(UUID companyId, int year);

    /** The rows of the year and of every earlier year, newest year first: the first year listed is the one in force. */
    List<EnergyCarrierUsed> findAllByCompany_IdAndYearLessThanEqualOrderByYearDesc(UUID companyId, int year);
}
