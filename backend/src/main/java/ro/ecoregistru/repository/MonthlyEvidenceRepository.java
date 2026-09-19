package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.MonthlyEvidence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MonthlyEvidenceRepository extends JpaRepository<MonthlyEvidence, UUID> {

    List<MonthlyEvidence> findByCompany_IdAndYear(UUID companyId, int year);

    /**
     * Takes the tenant's evidence rebuild lock, waiting for whoever holds it.
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
     * <p><b>One lock per tenant, not per year</b> (BUG-036). A rebuild cascades: reading 2026 after a
     * 2024 correction rewrites 2024 and 2025 too, and a "Regenerează" walks forward. Under a lock
     * per (tenant, year), three reads on three years rebuilt 2024 under three different locks, and
     * two of them failed on rows the third had already deleted. Serialising a tenant's rebuilds
     * costs a wait only when two of them are due at once; the fresh path takes no lock at all.
     *
     * <p>The key is the tenant's uuid hashed into an int4, with 0 as the second key of the two-key
     * form. A hash collision between two tenants costs one of them a wait, never a wrong answer.
     */
    @Query(value = "select 1 from (select pg_advisory_xact_lock("
            + "hashtext(cast(:companyId as text)), 0)) locked",
            nativeQuery = true)
    Integer lockForRebuild(@Param("companyId") UUID companyId);

    /** Wipe a tenant's cached lines for a year before regenerating them from movements. */
    void deleteByCompany_IdAndYear(UUID companyId, int year);

    /**
     * BUG-031 — marks stale the cached lines of the years a movement was moved out of.
     *
     * <p>Staleness is read from the movements dated up to the end of the year, by their
     * <b>current</b> date. A movement moved from 2025 into 2026 no longer passes the 2025 filter,
     * so 2025 looked fresh and kept counting it. Backdating {@code generatedAt} to the epoch makes
     * the next read of any of these years, or of a later one, rebuild from here.
     *
     * <p>Marked, not deleted: the lines are the next year's opening balance until they are rebuilt.
     * Wiped, a rebuild of 2026 found no 2025 lines, opened 2026 at zero and, being fresh after that,
     * kept the zero for good. No {@code clearAutomatically}: it runs in the middle of an update and
     * would detach the movement being edited.
     */
    @Modifying
    @Query("update MonthlyEvidence e set e.generatedAt = :stale where e.company.id = :companyId "
            + "and e.year >= :fromYear and e.year < :toYear")
    int markYearsStale(@Param("companyId") UUID companyId,
                       @Param("fromYear") int fromYear,
                       @Param("toYear") int toYear,
                       @Param("stale") Instant stale);

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

    /** First year the tenant has cached lines for; null when nothing was ever generated. */
    @Query("select min(e.year) from MonthlyEvidence e where e.company.id = :companyId")
    Integer findMinYear(@Param("companyId") UUID companyId);
}
