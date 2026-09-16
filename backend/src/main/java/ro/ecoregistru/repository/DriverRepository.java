package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.Driver;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

    List<Driver> findAllByCompany_IdOrderByNameAsc(UUID companyId);

    /** Our own drivers: the rows with no partner. */
    List<Driver> findAllByCompany_IdAndPartnerIsNullOrderByNameAsc(UUID companyId);

    Optional<Driver> findByIdAndCompany_Id(UUID id, UUID companyId);

    /**
     * D2.2 — șoferii noștri activi cu atestatul expirând în fereastră. Ai transportatorilor nu: fișa lor
     * nu are atestat, iar actele lor sunt treaba transportatorului.
     */
    @Query("""
            select d from Driver d join fetch d.company
             where d.active = true and d.partner is null
               and d.attestationExpiry between :from and :to""")
    List<Driver> findAttestationWarningCandidates(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
