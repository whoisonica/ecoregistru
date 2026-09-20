package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.WasteMovement;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WasteMovementRepository
        extends JpaRepository<WasteMovement, UUID>, JpaSpecificationExecutor<WasteMovement> {

    Optional<WasteMovement> findByIdAndCompany_IdAndDeletedFalse(UUID id, UUID companyId);

    List<WasteMovement> findAllByCompany_IdAndDeletedFalse(UUID companyId);

    /**
     * Liniile unei operațiuni de depozit, în ordinea cântăririi, cu codul și sortimentul aduse odată:
     * răspunsul le scrie pe amândouă, iar fără fetch fiecare linie ar costa încă două interogări
     * (aceeași formă ca BUG-016).
     */
    @Query("""
            select m from WasteMovement m
              join fetch m.wasteCode
              left join fetch m.article
            where m.weighingOperation.id = :operationId
            order by m.lineNo
            """)
    List<WasteMovement> findAllByWeighingOperation_IdOrderByLineNoAsc(@Param("operationId") UUID operationId);

    /** Liniile mai multor operațiuni dintr-o singură interogare, pentru listă. Vezi metoda de mai sus. */
    @Query("""
            select m from WasteMovement m
              join fetch m.wasteCode
              left join fetch m.article
            where m.weighingOperation.id in :operationIds
            order by m.lineNo
            """)
    List<WasteMovement> findAllByWeighingOperation_IdInOrderByLineNoAsc(
            @Param("operationIds") java.util.Collection<UUID> operationIds);

    /** Idempotency lookup for (future) offline sync. */
    Optional<WasteMovement> findByCompany_IdAndClientGeneratedId(UUID companyId, UUID clientGeneratedId);

    /**
     * Leagă mișcările abia create de importul lor (V62). Update în bloc, dinadins: nu mărește {@code version},
     * iar anularea recunoaște după {@code version = 0} un rând pe care nu l-a atins nimeni de la import.
     */
    @Modifying(flushAutomatically = true)
    @Query("update WasteMovement m set m.importBatchId = :batch where m.id in :ids")
    int tagImportBatch(@Param("batch") UUID batch, @Param("ids") java.util.Collection<UUID> ids);

    List<WasteMovement> findAllByImportBatchIdAndDeletedFalse(UUID importBatchId);

    /** Incremental fetch (?since=) support for delta sync. */
    List<WasteMovement> findAllByCompany_IdAndUpdatedAtGreaterThan(UUID companyId, Instant since);

    /**
     * The highest Anexa 3 number this company has used. Numbers are allocated on first generation
     * and kept, so a reprint is the same document; a unique index backs the allocation up.
     */
    @Query("select max(m.anexa3Number) from WasteMovement m where m.company.id = :companyId")
    Integer findMaxAnexa3Number(@Param("companyId") UUID companyId);

    /**
     * Every live row, <b>including the lines of depot operations still in progress or cancelled</b>.
     * A plain fetch, for screens and tests; nothing that adds up stock or prints a register may use
     * it — those read {@link #findCounted} / {@link #findCountedBetween}.
     */
    List<WasteMovement> findAllByCompany_IdAndDeletedFalseAndDateBetween(
            UUID companyId, LocalDate from, LocalDate to);

    /*
     * D1.3 — ce contează. O mișcare fără operațiune de depozit contează ca până acum; o linie de
     * operațiune contează doar când operațiunea e FINALIZATĂ. Una în lucru n-a ieșit încă de pe
     * cântar, iar una anulată n-a avut loc: niciuna nu intră în stoc, în art. 48, în Anexa 1 sau în
     * termene. Aceeași condiție e scrisă în toate interogările de mai jos, pe `left join`, ca o
     * mișcare veche (fără operațiune) să nu fie pierdută de un join interior.
     */

    /** All movements that count, every year — the art. 48 register carries stock from earlier years. */
    @Query("select m from WasteMovement m left join m.weighingOperation o "
            + "where m.company.id = :companyId and m.deleted = false "
            + "and (o is null or o.status = ro.ecoregistru.enums.WeighingOperationStatus.FINALIZED)")
    List<WasteMovement> findCounted(@Param("companyId") UUID companyId);

    /**
     * P2.13, felia 2 — câte mișcări care contează stau pe un cod-oglindă declarat nepericulos fără
     * niciun document atașat. Aceeași regulă ca badge-ul din listă ({@code WasteMovementMapper#mirrorClassificationUnproven}).
     */
    @Query("select count(m) from WasteMovement m left join m.weighingOperation o "
            + "where m.company.id = :companyId and m.deleted = false and m.date between :from and :to "
            + "and (o is null or o.status = ro.ecoregistru.enums.WeighingOperationStatus.FINALIZED) "
            + "and m.wasteCode.mirrorOf is not null and m.attachments is empty")
    long countUnprovenMirrorClassifications(@Param("companyId") UUID companyId,
                                            @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Câte mișcări contează într-un interval — aceeași regulă ca {@link #findCountedBetween}, numai numărul. */
    @Query("select count(m) from WasteMovement m left join m.weighingOperation o "
            + "where m.company.id = :companyId and m.deleted = false and m.date between :from and :to "
            + "and (o is null or o.status = ro.ecoregistru.enums.WeighingOperationStatus.FINALIZED)")
    long countCountedBetween(@Param("companyId") UUID companyId,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to);

    /** The movements that count within a date range — the evidence engine's input. */
    @Query("select m from WasteMovement m left join m.weighingOperation o "
            + "where m.company.id = :companyId and m.deleted = false and m.date between :from and :to "
            + "and (o is null or o.status = ro.ecoregistru.enums.WeighingOperationStatus.FINALIZED)")
    List<WasteMovement> findCountedBetween(@Param("companyId") UUID companyId,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);

    /**
     * The distinct six-digit waste codes a tenant recorded in a date range — the input of a signal
     * that asks "does this company handle X?", such as the used-oil half of the 30 April deadline
     * (OUG 92/2021 art. 49 alin. (9)).
     *
     * <p>A projection rather than a movement fetch: the question is about the <em>set</em> of
     * codes, and a company with three years of movements would otherwise load thousands of rows to
     * answer it. Deleted movements are excluded — a code that exists only on a deleted row is not
     * something the company handles.
     */
    @Query("select distinct m.wasteCode.code from WasteMovement m left join m.weighingOperation o "
            + "where m.company.id = :companyId and m.deleted = false "
            + "and m.date between :from and :to "
            + "and (o is null or o.status = ro.ecoregistru.enums.WeighingOperationStatus.FINALIZED)")
    List<String> findDistinctWasteCodes(@Param("companyId") UUID companyId,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);

    /**
     * Whether the company took over packaging waste ({@code 15 01}, Ordinul 794/2012 art. 8 alin. (3))
     * in a date range — the signal of the Anexa 3 deadline. Same exclusions as
     * {@link #findDistinctWasteCodes}: deleted rows and unfinished weighings do not count.
     */
    @Query("select count(m) > 0 from WasteMovement m left join m.weighingOperation o "
            + "where m.company.id = :companyId and m.deleted = false "
            + "and m.operation = ro.ecoregistru.enums.WasteOperation.COLLECTED "
            + "and m.wasteCode.code like '15 01%' "
            + "and m.date between :from and :to "
            + "and (o is null or o.status = ro.ecoregistru.enums.WeighingOperationStatus.FINALIZED)")
    boolean existsCollectedPackaging(@Param("companyId") UUID companyId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    /**
     * The two figures the dashboard shows about a month: how many movements were recorded and how
     * much they weigh together, in kilograms.
     *
     * <p>An aggregate rather than a fetch, and that is the whole point of it. The dashboard used to
     * ask for every movement of the month and add them up in the browser; since the list is paged
     * (P3.1) that would have added up one page and presented the result as the month's total.
     *
     * <p>The conversion is in the query because the unit is on the row: a company that records some
     * loads in kg and some in tonnes has no single factor to apply afterwards. Rows still waiting
     * for the recipient's weighbridge have no quantity; {@code sum} skips them, which is the same
     * thing the screen says about them — „de cântărit", not zero.
     */
    @Query("""
            select count(m) as movements,
                   coalesce(sum(case when m.unit = ro.ecoregistru.enums.Unit.TONS
                                     then m.quantity * 1000 else m.quantity end), 0) as quantityKg
            from WasteMovement m left join m.weighingOperation o
            where m.company.id = :companyId and m.deleted = false
              and m.date between :from and :to
              and (o is null or o.status = ro.ecoregistru.enums.WeighingOperationStatus.FINALIZED)
            """)
    MovementTotals summarise(@Param("companyId") UUID companyId,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to);

    /** The shape {@link #summarise} returns; the names are the aliases of its select list. */
    interface MovementTotals {
        long getMovements();

        java.math.BigDecimal getQuantityKg();
    }

    /**
     * Soldul registrului art. 48 la 1 ianuarie, pe cod de deșeu — <b>o sumă, nu rândurile</b>.
     *
     * <p>Registrul se tipărea citind {@link #findCounted}, adică <b>toate</b> mișcările firmei, din
     * toți anii și din ambele registre, ca să afle un singur lucru din anii dinainte: cât rămăsese
     * în stoc. La cinci ani × zece mii de mișcări, asta e o sesiune cu cincizeci de mii de entități
     * pe un dyno de 300 MB — aceeași formă ca BUG-017, plătită de clientul cu cel mai mult istoric.
     * Anii dinainte nu se citesc rând cu rând, se adună aici (20.09.2026).
     *
     * <p>Semnul e chiar regula soldului: preluarea adaugă, predarea și ieșirea fără cod R/D scad.
     * {@code GENERATED} nu intră — deșeul propriu e pe anexa 1, niciodată aici. Rândurile fără
     * cantitate se sar, ca peste tot: „de cântărit" nu e zero.
     */
    @Query("""
            select m.wasteCode.code as code,
                   m.wasteCode.hazardous as hazardous,
                   m.wasteCode.name as name,
                   coalesce(sum((case when m.operation = ro.ecoregistru.enums.WasteOperation.COLLECTED
                                      then 1 else -1 end)
                                * (case when m.unit = ro.ecoregistru.enums.Unit.TONS
                                        then m.quantity * 1000 else m.quantity end)), 0) as kg
            from WasteMovement m left join m.weighingOperation o
            where m.company.id = :companyId and m.deleted = false
              and m.register = ro.ecoregistru.enums.WasteRegister.ART_48
              and m.date < :before
              and m.quantity is not null
              and m.operation <> ro.ecoregistru.enums.WasteOperation.GENERATED
              and (:workPointId is null or m.workPoint.id = :workPointId)
              and (o is null or o.status = ro.ecoregistru.enums.WeighingOperationStatus.FINALIZED)
            group by m.wasteCode.code, m.wasteCode.hazardous, m.wasteCode.name
            """)
    List<Art48Opening> art48OpeningBefore(@Param("companyId") UUID companyId,
                                          @Param("workPointId") UUID workPointId,
                                          @Param("before") LocalDate before);

    /** The shape {@link #art48OpeningBefore} returns; the names are the aliases of its select list. */
    interface Art48Opening {
        String getCode();

        boolean getHazardous();

        String getName();

        java.math.BigDecimal getKg();
    }

    /**
     * The last time anything dated on or before {@code until} changed. Feeds the staleness check
     * that decides whether the cached evidence of a year still describes the movements.
     *
     * <p>Two things about the query are deliberate. It has <b>no {@code deletedFalse} filter</b>:
     * deleting a movement invalidates the cache exactly as editing one does, and a soft delete
     * bumps {@code updatedAt} on a row that stays. And it looks at every year <b>up to</b> the one
     * asked about, not only that year: stock is cumulative, so a correction on a 2024 movement
     * makes the 2026 lines wrong through the opening balance they carry.
     *
     * <p>D1.3: a line also changes <b>when its operation does</b>. Finalizing or cancelling touches
     * only the operation's head, so the line's own {@code updatedAt} stays put while what it counts
     * for flips; the later of the two timestamps is the line's last change.
     */
    @Query("select max(case when o.updatedAt is not null and o.updatedAt > m.updatedAt "
            + "then o.updatedAt else m.updatedAt end) "
            + "from WasteMovement m left join m.weighingOperation o "
            + "where m.company.id = :companyId and m.date <= :until")
    Instant findLastChangeUpTo(@Param("companyId") UUID companyId, @Param("until") LocalDate until);

    /**
     * Year of the tenant's earliest movement, deleted ones included; null when it has none. Where
     * the evidence chain starts: stock carries forward, so a rebuild begins here.
     */
    @Query("select min(extract(year from m.date)) from WasteMovement m where m.company.id = :companyId")
    Integer findFirstYear(@Param("companyId") UUID companyId);

    /**
     * AO — numele şi actul de identitate ale delegatului, şterse de pe mişcările mai vechi decât
     * termenul de păstrare (OUG 92/2021 art. 48 alin. (5): cel puţin 3 ani). Numărul maşinii rămâne.
     * Actualizare în bloc, deci fără rânduri de jurnal de audit; jobul scrie în log câte a atins.
     * Include şi mişcările şterse moale: o dată personală nu devine mai puţin personală la ştergere.
     */
    @Modifying
    @Query("update WasteMovement m set m.driverName = null, m.driverIdentification = null, "
            + "m.driverCnp = null where m.date < :cutoff and (m.driverName is not null "
            + "or m.driverIdentification is not null or m.driverCnp is not null)")
    int clearDriverDataBefore(@Param("cutoff") LocalDate cutoff);
}
