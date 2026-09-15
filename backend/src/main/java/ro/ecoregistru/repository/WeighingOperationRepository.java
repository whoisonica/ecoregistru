package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.enums.WeighingOperationType;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WeighingOperationRepository extends JpaRepository<WeighingOperation, UUID> {

    Optional<WeighingOperation> findByIdAndCompany_Id(UUID id, UUID companyId);

    List<WeighingOperation> findAllByCompany_IdOrderByDateDescNumberDesc(UUID companyId);

    boolean existsByNaturalPerson_Id(UUID naturalPersonId);

    /** Persoanele fizice ale firmei care apar pe cel puțin o operațiune, într-o singură interogare pentru listă. */
    @Query("select distinct o.naturalPerson.id from WeighingOperation o "
            + "where o.company.id = :companyId and o.naturalPerson is not null")
    Set<UUID> findNaturalPersonIdsWithOperations(@Param("companyId") UUID companyId);

    @Query("select max(o.number) from WeighingOperation o where o.company.id = :companyId and o.type = :type")
    Integer findMaxNumber(@Param("companyId") UUID companyId, @Param("type") WeighingOperationType type);

    /**
     * Ține numerotarea unei firme pe un tip până la sfârșitul tranzacției. A doua creare simultană
     * așteaptă commitul primei și abia apoi citește maximul, deci nu poate lua același număr.
     *
     * <p>Lacăt consultativ, nu reîncercare pe indexul unic: în Postgres un INSERT picat strică toată
     * tranzacția, deci o reîncercare ar cere o tranzacție nouă. Indexul rămâne plasa de dedesubt.
     * Funcția întoarce {@code void}, de aceea e învelită într-un {@code select 1}.
     */
    @Query(value = "select 1 from (select pg_advisory_xact_lock(hashtextextended(:key, 0))) l",
            nativeQuery = true)
    Integer lockNumbering(@Param("key") String key);
}
