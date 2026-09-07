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
