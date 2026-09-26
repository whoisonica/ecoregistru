package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.ReceivedForm;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceivedFormRepository extends JpaRepository<ReceivedForm, UUID> {

    @Query("select max(f.entryNo) from ReceivedForm f where f.workPoint.id = :workPointId")
    Integer findMaxEntryNo(@Param("workPointId") UUID workPointId);

    @Query("""
            select f from ReceivedForm f
            where f.companyId = :companyId and f.workPoint.id = :workPointId
              and f.receivedOn between :from and :to
            order by f.entryNo
            """)
    List<ReceivedForm> findForRegister(@Param("companyId") UUID companyId, @Param("workPointId") UUID workPointId,
                                       @Param("from") LocalDate from, @Param("to") LocalDate to);

    Optional<ReceivedForm> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCorrectsId(UUID correctsId);
}
