package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.Consultancy;

import java.util.UUID;

public interface ConsultancyRepository extends JpaRepository<Consultancy, UUID> {
    boolean existsByCui(String cui);

    /** Ca la firme: același cabinet, cu sau fără „RO”. */
    @Query("select count(c) > 0 from Consultancy c where upper(c.cui) = :digits or upper(c.cui) = concat('RO', :digits)")
    boolean existsByCuiDigits(@Param("digits") String digits);
}
