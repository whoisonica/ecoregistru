package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.MonthlyEvidence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MonthlyEvidenceRepository extends JpaRepository<MonthlyEvidence, UUID> {

    List<MonthlyEvidence> findByCompany_IdAndYear(UUID companyId, int year);

    /**
     * Takes the rebuild lock for one (tenant, year), waiting for whoever holds it.
     *
     * <p>It exists because reading the evidence <b>writes</b>: {@code list()} rebuilds a year that
     * has fallen behind its movements. Two reads landing on the same stale year both entered the
     * rebuild, and {@code deleteByCompany_IdAndYear} loads the rows before deleting them — so the
     * second one deleted rows the first had already replaced and failed with "Row was updated or
     * deleted by another transaction". The client saw a report that would not open.
     *
     * <p><b>An advisory lock, not a row lock.</b> There is no row that stands for "the cached year"
     * — the rebuild deletes every row it could have locked. Locking the company row instead would
     * serialise unrelated years and block plain company edits for the length of a rebuild. This
     * locks the pair itself, needs no column and therefore no migration, and Postgres releases it
     * at commit or rollback, so a rebuild that throws cannot leave it held.
     *
     * <p>The key is the tenant's uuid hashed into an int4 plus the year, which is what the two-key
     * form of the function takes. A hash collision between two tenants costs one of them a wait,
     * never a wrong answer.
     */
    @Query(value = "select 1 from (select pg_advisory_xact_lock("
            + "hashtext(cast(:companyId as text)), cast(:year as int))) locked",
            nativeQuery = true)
    Integer lockForRebuild(@Param("companyId") UUID companyId, @Param("year") int year);

    /** Wipe a tenant's cached lines for a year before regenerating them from movements. */
    void deleteByCompany_IdAndYear(UUID companyId, int year);

    /**
     * Last year the tenant has cached lines for; null when nothing was ever generated. Bounds the
     * cascade that rebuilds the years after a corrected one.
     */
    @Query("select max(e.year) from MonthlyEvidence e where e.company.id = :companyId")
    Integer findMaxYear(@Param("companyId") UUID companyId);

    /**
     * When the oldest cached line of a year was written; null when the year has none.
     *
     * <p>The <b>oldest</b>, not the newest: a regeneration wipes the year and writes every line in
     * one transaction, so they share an instant, and taking the minimum is what keeps a partially
     * written year from passing as fresh.
     */
    @Query("select min(e.generatedAt) from MonthlyEvidence e "
            + "where e.company.id = :companyId and e.year = :year")
    Instant findOldestGeneratedAt(@Param("companyId") UUID companyId, @Param("year") int year);

    /**
     * The earliest year, at or before {@code year}, whose lines were written before
     * {@code changedAt}; null when every cached year up to it is newer than that.
     *
     * <p>It exists because stock carries: a correction on a 2024 movement leaves the 2026 opening
     * balance wrong, so rebuilding 2026 alone would rebuild it on the same wrong figure. This says
     * where the rebuild has to start.
     */
    @Query("select min(e.year) from MonthlyEvidence e where e.company.id = :companyId "
            + "and e.year <= :year and e.generatedAt < :changedAt")
    Integer findEarliestStaleYear(@Param("companyId") UUID companyId,
                                  @Param("year") int year,
                                  @Param("changedAt") Instant changedAt);
}
