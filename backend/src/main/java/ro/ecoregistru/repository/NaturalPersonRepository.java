package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.NaturalPerson;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NaturalPersonRepository extends JpaRepository<NaturalPerson, UUID> {

    /** Ce rămâne în locul numelui unei persoane ale cărei date au trecut de termen. */
    String ERASED_NAME = "Persoană fizică (date șterse)";

    Optional<NaturalPerson> findByIdAndCompany_Id(UUID id, UUID companyId);

    List<NaturalPerson> findAllByCompany_IdOrderByNameAsc(UUID companyId);

    boolean existsByCompany_IdAndCnp(UUID companyId, String cnp);

    boolean existsByCompany_IdAndCnpAndIdNot(UUID companyId, String cnp, UUID id);

    /**
     * Anonimizează persoanele fără nicio operațiune de la {@code cutoff} încoace (Legea 82/1991 art. 25:
     * borderoul se păstrează 10 ani de la încheierea exercițiului). Rândul rămâne, fiindcă operațiunile
     * vechi îl numesc prin cheie străină; pleacă numele, CNP-ul, actul și domiciliul. O persoană creată
     * după {@code createdBefore} și nefolosită încă nu se atinge.
     *
     * <p>Actualizare în bloc, deci fără rânduri de jurnal de audit; jobul scrie în log câte a atins.
     */
    @Modifying
    @Query("update NaturalPerson p set p.name = '" + ERASED_NAME + "', p.cnp = null, p.identification = null, "
            + "p.address = null, p.active = false "
            + "where p.createdAt < :createdBefore "
            + "and (p.cnp is not null or p.identification is not null or p.address is not null "
            + "or p.name <> '" + ERASED_NAME + "') "
            + "and not exists (select 1 from WeighingOperation o where o.naturalPerson = p and o.date >= :cutoff)")
    int anonymizeUnusedSince(@Param("cutoff") LocalDate cutoff, @Param("createdBefore") Instant createdBefore);
}
