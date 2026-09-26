package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.Scale;
import ro.ecoregistru.enums.ScaleStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScaleRepository extends JpaRepository<Scale, UUID> {

    @Query("""
            select s from Scale s join fetch s.workPoint
             where s.company.id = :companyId
             order by s.workPoint.name, s.name""")
    List<Scale> findAllForCompany(@Param("companyId") UUID companyId);

    Optional<Scale> findByIdAndCompany_Id(UUID id, UUID companyId);

    Optional<Scale> findByCompany_IdAndWorkPoint_IdAndName(UUID companyId, UUID workPointId, String name);

    /** Pentru alertă: cântarele în uz ale tuturor firmelor, cu firma odată (destinatarii). */
    @Query("select s from Scale s join fetch s.company where s.status = :status")
    List<Scale> findAllByStatusWithCompany(@Param("status") ScaleStatus status);
}
