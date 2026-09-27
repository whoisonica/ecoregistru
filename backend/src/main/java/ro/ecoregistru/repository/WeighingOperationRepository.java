package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.enums.WeighingOperationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface WeighingOperationRepository extends JpaRepository<WeighingOperation, UUID> {

    Optional<WeighingOperation> findByIdAndCompany_Id(UUID id, UUID companyId);

    boolean existsByNaturalPerson_Id(UUID naturalPersonId);

    boolean existsByScale_Id(UUID scaleId);

    /**
     * Lista ecranului (D1.15), cu partenerul, persoana și depozitul aduse odată: altfel fiecare rând
     * ar cere încă trei interogări (BUG-016). {@code type} lipsă înseamnă amândouă direcțiile.
     */
    @Query("""
            select o from WeighingOperation o
              left join fetch o.partner
              left join fetch o.naturalPerson
              join fetch o.workPoint
              left join fetch o.scale
            where o.company.id = :companyId
              and o.date between :from and :to
              and (:type is null or o.type = :type)
            order by o.date desc, o.number desc
            """)
    List<WeighingOperation> findForScreen(@Param("companyId") UUID companyId,
                                          @Param("type") WeighingOperationType type,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);

    /**
     * F6a — recepțiile neconfirmate în SIATD care au măcar o linie pe un cod din SIATD (ambalaje, anvelope, DEEE,
     * baterii, capitolul 20). Filtrul exact pe modul și pe înrolare rămâne în Java ({@code SiatdDeadlines}).
     */
    @Query("""
            select o from WeighingOperation o
              left join fetch o.partner
              left join fetch o.naturalPerson
              join fetch o.workPoint
            where o.company.id = :companyId
              and o.type = ro.ecoregistru.enums.WeighingOperationType.IN
              and o.status in (ro.ecoregistru.enums.WeighingOperationStatus.IN_PROGRESS,
                               ro.ecoregistru.enums.WeighingOperationStatus.FINALIZED)
              and o.siatdConfirmedAt is null
              and exists (select 1 from WasteMovement m where m.weighingOperation = o
                          and (m.wasteCode.code like '15 01%' or m.wasteCode.code like '16 01 03%'
                               or m.wasteCode.code like '16 02%' or m.wasteCode.code like '16 06%'
                               or m.wasteCode.code like '20 %'))
            order by o.date, o.number
            """)
    List<WeighingOperation> findSiatdCandidates(@Param("companyId") UUID companyId);

    /** F6a — recepțiile confirmate în SIATD de la {@code since} încoace, cele mai noi primele. */
    @Query("""
            select o from WeighingOperation o
              left join fetch o.partner
              left join fetch o.naturalPerson
              join fetch o.workPoint
            where o.company.id = :companyId
              and o.siatdConfirmedAt >= :since
            order by o.siatdConfirmedAt desc, o.number desc
            """)
    List<WeighingOperation> findSiatdConfirmedSince(@Param("companyId") UUID companyId,
                                                    @Param("since") java.time.Instant since);

    /** Persoanele fizice ale firmei care apar pe cel puțin o operațiune, într-o singură interogare pentru listă. */
    @Query("select distinct o.naturalPerson.id from WeighingOperation o "
            + "where o.company.id = :companyId and o.naturalPerson is not null")
    Set<UUID> findNaturalPersonIdsWithOperations(@Param("companyId") UUID companyId);


    /**
     * D1.9 și D1.10 — ce s-a reținut la sursă într-o perioadă: bazele și sumele păstrate pe
     * operațiunile <b>finalizate</b> de intrare. Anulatele nu se declară, iar ieșirile nu rețin nimic
     * (acolo cei 2% îi reține cumpărătorul). Luna e cea a <b>datei operațiunii</b>, nu a introducerii ei.
     */
    @Query("""
            select coalesce(sum(o.afmBase), 0) as afmBase,
                   coalesce(sum(o.afmContribution), 0) as afm,
                   coalesce(sum(o.incomeTaxBase), 0) as incomeTaxBase,
                   coalesce(sum(o.incomeTax), 0) as incomeTax,
                   count(o) as operations
            from WeighingOperation o
            where o.company.id = :companyId
              and o.type = ro.ecoregistru.enums.WeighingOperationType.IN
              and o.status = ro.ecoregistru.enums.WeighingOperationStatus.FINALIZED
              and o.date between :from and :to
            """)
    RetentionTotals sumRetentions(@Param("companyId") UUID companyId,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);

    /** Forma pe care o întoarce {@link #sumRetentions}; numele sunt aliasurile din select. */
    interface RetentionTotals {
        BigDecimal getAfmBase();

        BigDecimal getAfm();

        BigDecimal getIncomeTaxBase();

        BigDecimal getIncomeTax();

        long getOperations();
    }

    /**
     * Declarația pe fiecare beneficiar de venit (Codul fiscal art. 132 alin. (2), D205): persoanele
     * fizice de la care s-a reținut impozit în perioadă, cu venitul brut și impozitul. Persoanele
     * fără impozit — hârtie, plastic, acumulatori — nu apar: n-au venit impozabil de declarat.
     */
    @Query("""
            select p.id as personId, p.name as name, p.cnp as cnp,
                   coalesce(sum(o.incomeTaxBase), 0) as base,
                   coalesce(sum(o.incomeTax), 0) as tax
            from WeighingOperation o join o.naturalPerson p
            where o.company.id = :companyId
              and o.type = ro.ecoregistru.enums.WeighingOperationType.IN
              and o.status = ro.ecoregistru.enums.WeighingOperationStatus.FINALIZED
              and o.date between :from and :to
              and o.incomeTax > 0
            group by p.id, p.name, p.cnp
            order by p.name
            """)
    List<TaxedBeneficiary> findTaxedBeneficiaries(@Param("companyId") UUID companyId,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);

    /** Forma pe care o întoarce {@link #findTaxedBeneficiaries}. CNP-ul se maschează în serviciu. */
    interface TaxedBeneficiary {
        UUID getPersonId();

        String getName();

        String getCnp();

        BigDecimal getBase();

        BigDecimal getTax();
    }

    @Query("select max(o.number) from WeighingOperation o where o.company.id = :companyId and o.type = :type")
    Integer findMaxNumber(@Param("companyId") UUID companyId, @Param("type") WeighingOperationType type);

    /** D1.11 — cel mai mare număr de borderou al firmei; regim intern de numerotare (OUG 31/2011 art. 1 alin. (1^3)). */
    @Query("select max(o.borderouNumber) from WeighingOperation o where o.company.id = :companyId")
    Integer findMaxBorderouNumber(@Param("companyId") UUID companyId);

    /** D1.17a — cel mai mare număr de NIR al firmei (OMFP 2634/2015 pct. 24: seria fără goluri). */
    @Query("select max(o.receptionNoteNumber) from WeighingOperation o where o.company.id = :companyId")
    Integer findMaxReceptionNoteNumber(@Param("companyId") UUID companyId);

    /** D4.7 — numerele date vreodată, pe fiecare serie a firmei: golurile din registrul documentelor emise. */
    @Query("select o.anexa3Number from WeighingOperation o where o.company.id = :companyId and o.anexa3Number is not null")
    List<Integer> findAllAnexa3Numbers(@Param("companyId") UUID companyId);

    @Query("select o.borderouNumber from WeighingOperation o where o.company.id = :companyId and o.borderouNumber is not null")
    List<Integer> findAllBorderouNumbers(@Param("companyId") UUID companyId);

    @Query("select o.receptionNoteNumber from WeighingOperation o "
            + "where o.company.id = :companyId and o.receptionNoteNumber is not null")
    List<Integer> findAllReceptionNoteNumbers(@Param("companyId") UUID companyId);

    /** D1.11 — plățile în numerar către aceeași persoană în aceeași zi, cu operațiunea care se verifică. */
    @Query("""
            select o from WeighingOperation o
            where o.company.id = :companyId and o.naturalPerson.id = :personId and o.date = :date
              and o.paymentMethod = ro.ecoregistru.enums.PaymentMethod.NUMERAR
              and o.status <> ro.ecoregistru.enums.WeighingOperationStatus.CANCELLED
            """)
    List<WeighingOperation> findCashToPersonOnDay(@Param("companyId") UUID companyId,
                                                  @Param("personId") UUID personId,
                                                  @Param("date") LocalDate date);

    /** D1.13 — cel mai mare număr de Anexa 3 dat unei operațiuni; seria e comună cu a mișcărilor. */
    @Query("select max(o.anexa3Number) from WeighingOperation o where o.company.id = :companyId")
    Integer findMaxAnexa3Number(@Param("companyId") UUID companyId);

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

    /** D3.5 — ultimele operațiuni ale unui depozit dinaintea unei date: declarația gestionarului (pct. 8 lit. a)). */
    List<WeighingOperation> findTop1ByCompany_IdAndWorkPoint_IdAndTypeInAndStatusInAndDateBeforeOrderByDateDescNumberDesc(
            UUID companyId, UUID workPointId, java.util.Collection<ro.ecoregistru.enums.WeighingOperationType> types,
            java.util.Collection<ro.ecoregistru.enums.WeighingOperationStatus> statuses, java.time.LocalDate before);

    /** D3.5 — operațiunile unui depozit datate în perioada inventarului (pct. 9). */
    List<WeighingOperation> findAllByCompany_IdAndWorkPoint_IdAndStatusInAndDateBetweenOrderByDateAscNumberAsc(
            UUID companyId, UUID workPointId, java.util.Collection<ro.ecoregistru.enums.WeighingOperationStatus> statuses,
            java.time.LocalDate from, java.time.LocalDate to);
}
