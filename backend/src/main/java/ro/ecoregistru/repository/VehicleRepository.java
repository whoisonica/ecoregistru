package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.Vehicle;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    @Query("""
            select v from Vehicle v
              left join fetch v.homeWorkPoint
              left join fetch v.partner
             where v.company.id = :companyId
             order by v.registration""")
    List<Vehicle> findAllForCompany(@Param("companyId") UUID companyId);

    Optional<Vehicle> findByIdAndCompany_Id(UUID id, UUID companyId);

    Optional<Vehicle> findByCompany_IdAndRegistration(UUID companyId, String registration);

    /** Vehiculele active cu ITP-ul sau licența expirând în fereastră; firma vine odată, pentru destinatari. */
    @Query("""
            select v from Vehicle v join fetch v.company
             where v.active = true
               and (v.itpExpiry between :from and :to or v.transportLicenseExpiry between :from and :to)""")
    List<Vehicle> findWarningCandidates(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
