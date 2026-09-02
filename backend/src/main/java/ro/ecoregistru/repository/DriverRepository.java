package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.ecoregistru.entity.Driver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

    List<Driver> findAllByCompany_IdOrderByNameAsc(UUID companyId);

    /** Our own drivers: the rows with no partner. */
    List<Driver> findAllByCompany_IdAndPartnerIsNullOrderByNameAsc(UUID companyId);

    Optional<Driver> findByIdAndCompany_Id(UUID id, UUID companyId);
}
