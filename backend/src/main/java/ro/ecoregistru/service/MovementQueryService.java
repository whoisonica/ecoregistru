package ro.ecoregistru.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.response.MovementSummaryResponse;
import ro.ecoregistru.controller.response.MovementTotalsResponse;
import ro.ecoregistru.controller.response.PageResponse;
import ro.ecoregistru.controller.response.WasteMovementResponse;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.MovementDirection;
import ro.ecoregistru.enums.PackagingFilter;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.mapper.WasteMovementMapper;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * Citirile ecranelor de mișcări: lista pe pagini, totalurile de deasupra ei și sumarul lunii de pe
 * Panou. Toate trec prin același filtru ({@link #buildFilter}), scopat pe firma din {@code TenantContext}.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovementQueryService {

    WasteMovementRepository movementRepository;
    DepotAccess depotAccess;
    WasteMovementMapper mapper;
    /** Pentru totaluri: o interogare Criteria cu agregate, peste același filtru ca lista. */
    EntityManager entityManager;

    /**
     * The movements screen, one page at a time.
     *
     * <p>Until now this returned everything the filters matched and the browser did the rest —
     * search, sort and paging all in {@code useTableView}. That held while a client had a month of
     * data and stopped holding at two years, which is the one thing in the backlog that breaks
     * without anyone touching it. The screen's own defence was the month filter it starts on; the
     * moment someone asks for a whole year, or imports two, there is nothing between the table and
     * every row the company owns.
     *
     * <p>So all three moved here. Not paging alone: a search box that only looks inside the
     * twenty-five rows on screen is worse than no search box, because it answers confidently and
     * wrongly. {@link FoldedSearch} carries the browser's rules over word for word.
     *
     * @param search   what was typed in the toolbar, or null/blank for „everything"
     * @param page     zero-based; a page past the end comes back empty rather than as an error,
     *                 because a row deleted by someone else can shorten the table under you
     * @param size     rows per page, clamped to {@link #MAX_PAGE_SIZE}
     * @param sortKey  column key from the table header, or null for the default order
     * @param ascending direction of that column
     */
    @Transactional(readOnly = true)
    public PageResponse<WasteMovementResponse> list(Integer year, Integer month, UUID workPointId,
                                                    UUID wasteCodeId, boolean leftSite,
                                                    boolean missingOperationCode, boolean incomplete,
                                                    WasteRegister register,
                                                    MovementDirection direction, PackagingFilter packaging,
                                                    String search,
                                                    int page, int size, String sortKey, boolean ascending) {
        UUID tenantId = TenantContext.require();
        LocalDate[] window = window(year, month);
        Specification<WasteMovement> filter = buildFilter(tenantId, workPointId, wasteCodeId,
                window[0], window[1], leftSite, missingOperationCode, register, direction, packaging);
        if (incomplete) {
            filter = filter.and((r, q, cb) -> incomplete(r, cb));
        }
        Specification<WasteMovement> spec = ordered(withSearch(filter, search), sortKey, ascending);
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size));

        return PageResponse.of(movementRepository.findAll(spec, pageable), mapper::toResponse);
    }

    /**
     * Cifrele de deasupra listei — aceleași filtre ca {@link #list}, adunate de bază peste toate
     * rândurile, nu peste o pagină. Ecranele „Generare", „Intrări" și „Ieșiri" (15.09.2026).
     *
     * <p>Folosește <b>același</b> {@link #buildFilter} ca lista, printr-o interogare Criteria cu
     * agregate, ca totalul să nu poată descrie alte rânduri decât cele afișate. Kilogramele se
     * normalizează în interogare (tonele × 1000), ca la {@code summarise}; rândurile fără cantitate
     * nu intră în nicio sumă, se numără. Liniile unei operațiuni de cântar nefinalizate nu se
     * socotesc — sunt ciorne, nu evidență (D1.3).
     */
    @Transactional(readOnly = true)
    public MovementTotalsResponse totals(Integer year, Integer month, UUID workPointId,
                                         UUID wasteCodeId, boolean leftSite,
                                         boolean missingOperationCode, boolean incomplete,
                                         WasteRegister register, MovementDirection direction,
                                         PackagingFilter packaging) {
        UUID tenantId = TenantContext.require();
        LocalDate[] window = window(year, month);
        // Toate filtrele listei, nu doar şase. Patru dintre ele — codul de deşeu, „a ieşit pe
        // poartă", „fără cod R/D" şi „de completat" — se trimiteau goale, deşi lista le aplică: cine
        // intra pe „2 linii fără cod R/D" de pe Acasă vedea două rânduri, iar deasupra lor totalul
        // anului întreg. Banda stă peste listă, deci descrie chiar rândurile de sub ea (20.09.2026).
        Specification<WasteMovement> filter = buildFilter(tenantId, workPointId, wasteCodeId,
                window[0], window[1], leftSite, missingOperationCode, register, direction, packaging);
        if (incomplete) {
            filter = filter.and((r, q, cb) -> incomplete(r, cb));
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<WasteMovement> root = query.from(WasteMovement.class);
        Join<WasteMovement, WeighingOperation> op = root.join("weighingOperation", JoinType.LEFT);

        Expression<BigDecimal> kg = cb.<BigDecimal>selectCase()
                .when(cb.equal(root.get("unit"), Unit.TONS),
                        cb.prod(root.<BigDecimal>get("quantity"), BigDecimal.valueOf(1000)))
                .otherwise(root.<BigDecimal>get("quantity"));
        Expression<BigDecimal> zero = cb.literal(BigDecimal.ZERO);
        Expression<Integer> one = cb.literal(1);
        Expression<Integer> none = cb.literal(0);

        query.multiselect(
                cb.count(root).alias("rows"),
                cb.coalesce(cb.sum(kg), zero).alias("quantityKg"),
                cb.sum(cb.<Integer>selectCase().when(cb.isNull(root.get("quantity")), one).otherwise(none))
                        .alias("awaitingWeighing"),
                cb.coalesce(cb.sum(cb.<BigDecimal>selectCase()
                        .when(cb.equal(root.get("operation"), WasteOperation.RECOVERED), kg).otherwise(zero)), zero)
                        .alias("recoveredKg"),
                cb.coalesce(cb.sum(cb.<BigDecimal>selectCase()
                        .when(cb.equal(root.get("operation"), WasteOperation.DISPOSED), kg).otherwise(zero)), zero)
                        .alias("disposedKg"),
                cb.sum(cb.<Integer>selectCase()
                        .when(cb.equal(root.get("operation"), WasteOperation.UNCLASSIFIED_OUT), one).otherwise(none))
                        .alias("missingOperationCode"),
                cb.sum(cb.<Integer>selectCase().when(incomplete(root, cb), one).otherwise(none))
                        .alias("incomplete"),
                cb.coalesce(cb.sum(cb.<BigDecimal>selectCase()
                        .when(cb.isNotNull(op.get("naturalPerson")), kg).otherwise(zero)), zero)
                        .alias("fromNaturalPersonsKg"));
        query.where(filter.toPredicate(root, query, cb));

        Tuple t = entityManager.createQuery(query).getSingleResult();
        return new MovementTotalsResponse(
                number(t.get("rows")),
                decimal(t.get("quantityKg")),
                number(t.get("awaitingWeighing")),
                decimal(t.get("recoveredKg")),
                decimal(t.get("disposedKg")),
                number(t.get("missingOperationCode")),
                decimal(t.get("fromNaturalPersonsKg")),
                number(t.get("incomplete")));
    }

    /**
     * Decizia 19.09.2026: o predare de pe Anexa 1 căreia îi lipsește ceva ce tipăresc rapoartele —
     * aceleași întrebări pe care {@code WasteMovementService.validateOwnWasteHandover} le pune la
     * salvare. Rândurile vechi nu se blochează, se listează ca să poată fi completate.
     */
    static Predicate incomplete(Root<WasteMovement> root, CriteriaBuilder cb) {
        Path<String> code = root.get("wasteCode").get("code");
        Predicate packagingGap = cb.and(cb.like(code, "15 01%"), cb.or(
                cb.and(cb.isNull(root.get("packagingMaterial")),
                        cb.not(code.in(PackagingMaterial.settledCodes()))),
                cb.and(cb.or(cb.isNull(root.get("packagingOnMarket")), cb.isTrue(root.get("packagingOnMarket"))),
                        cb.isNull(root.get("packagingCategory")))));
        return cb.and(
                cb.equal(root.get("register"), WasteRegister.ANEXA_1),
                cb.or(
                        cb.equal(root.get("operation"), WasteOperation.UNCLASSIFIED_OUT),
                        cb.and(root.get("operation").in(WasteOperation.RECOVERED, WasteOperation.DISPOSED),
                                cb.or(cb.isNull(root.get("physicalState")),
                                        cb.isNull(root.get("storageType")),
                                        cb.isNull(root.get("transportMeans")),
                                        cb.isNull(root.get("wasteDestination")),
                                        packagingGap))));
    }

    /** Sumele de întregi ies `Long` sau `Integer` după dialect; `null` când nu e niciun rând. */
    private static long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : (BigDecimal) value;
    }

    /**
     * Fereastra de date a unui filtru: luna, sau anul întreg — sau nimic.
     *
     * <p>Anul fără lună înseamnă anul întreg. Până acum nu însemna nimic: se cerea `?year=2026`
     * fără lună și veneau înapoi toate mișcările, din toți anii — o filtrare care se ignora în
     * tăcere, deci mai rea decât una respinsă. Treapta rămâne după ce căutarea s-a mutat pe
     * server, dar din alt motiv decât înainte: acum nu mai apără ecranul de „tot" (paginarea o
     * face), ci spune ce se caută — „luna asta" sau „anul ăsta".
     */
    private static LocalDate[] window(Integer year, Integer month) {
        LocalDate fromDate = null;
        LocalDate toDate = null;
        if (year != null) {
            if (month != null) {
                YearMonth ym = YearMonth.of(year, month);
                fromDate = ym.atDay(1);
                toDate = ym.atEndOfMonth();
            } else {
                fromDate = LocalDate.of(year, 1, 1);
                toDate = LocalDate.of(year, 12, 31);
            }
        }
        return new LocalDate[] {fromDate, toDate};
    }

    /**
     * Ce s-a înregistrat într-o lună, în două cifre — fără să aducă niciun rând.
     *
     * <p>Panoul le citea din lista de mișcări, adunând în browser tot ce venise. De când lista vine
     * pe pagini (P3.1), aceeași adunare ar fi adunat <b>o pagină</b> și ar fi scris rezultatul sub
     * titlul „luna aceasta". Aici numărul îl dă baza de date, peste luna întreagă.
     *
     * <p>Ferestrele sunt aceleași ca la {@code list}: luna calendaristică a companiei curente,
     * fără mișcările șterse.
     */
    @Transactional(readOnly = true)
    public MovementSummaryResponse summary(int year, int month) {
        UUID tenantId = TenantContext.require();
        YearMonth ym = YearMonth.of(year, month);
        var totals = movementRepository.summarise(tenantId, ym.atDay(1), ym.atEndOfMonth());
        return new MovementSummaryResponse(totals.getMovements(), totals.getQuantityKg());
    }

    /**
     * The phrase first, the words only if the phrase found nothing — the order the browser used,
     * and the reason it is two queries rather than one {@code OR}.
     *
     * <p>„15 01 02" typed in the box is a waste code, so if any row carries it as written, those
     * are the rows meant; the word search exists for „hamburger 15 01", which is nobody's phrase
     * but describes a row exactly. An {@code OR} would mix the two and bury the exact match among
     * the loose ones.
     */
    private Specification<WasteMovement> withSearch(Specification<WasteMovement> filter, String search) {
        String folded = FoldedSearch.fold(search == null ? "" : search.trim());
        if (folded.isEmpty()) {
            return filter;
        }
        Specification<WasteMovement> phrase = filter.and((root, query, cb) ->
                FoldedSearch.phrase(cb, searchableText(root, cb), folded));
        if (movementRepository.count(phrase) > 0) {
            return phrase;
        }
        List<String> tokens = FoldedSearch.tokenize(folded);
        if (tokens.isEmpty()) {
            return phrase;
        }
        return filter.and((root, query, cb) -> FoldedSearch.words(cb, searchableText(root, cb), tokens));
    }

    /**
     * The columns someone would type, in the same list as {@code MovementsPage.tsx}: the code, its
     * name, the partner, the section, the work point, the document number and the date as it is
     * printed on screen.
     *
     * <p>The joins are left joins on purpose. A movement without a partner — an internally
     * generated one — must stay findable by its own code; an inner join would have hidden exactly
     * the rows that have the least written on them.
     *
     * <p>The date goes in formatted {@code dd.MM.yyyy} rather than ISO, because that is what the
     * screen shows and therefore what a person retypes when looking for „predarea din 11.09".
     */
    private Expression<String> searchableText(Root<WasteMovement> root, CriteriaBuilder cb) {
        return FoldedSearch.haystack(cb,
                root.join("wasteCode", JoinType.LEFT).get("code"),
                root.join("wasteCode", JoinType.LEFT).get("name"),
                root.join("partner", JoinType.LEFT).get("name"),
                root.join("internalGenerator", JoinType.LEFT).get("name"),
                root.join("workPoint", JoinType.LEFT).get("name"),
                root.get("documentReference"),
                root.get("operationCode").as(String.class),
                cb.function("to_char", String.class, root.get("date"), cb.literal("DD.MM.YYYY")),
                cb.function("to_char", String.class, root.get("unloadDate"), cb.literal("DD.MM.YYYY")));
    }

    /** Nobody asks for more than a screenful; a client asking for 10.000 is asking for the old bug back. */
    private static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_PAGE_SIZE = 25;

    private int clampSize(int size) {
        return size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * Which column headers may sort, and by what.
     *
     * <p>A whitelist rather than the property name straight off the query string: {@code ?sort=}
     * reaching a JPA path is a way to sort by, and so learn about, columns the screen never shows.
     * The keys are the ones the table headers pass to {@code toggleSort}.
     *
     * <p>The two that walk into another table join <b>left</b>. Sorting by partner must not make
     * the movements without a partner disappear from the table — which is exactly what an inner
     * join would do, and it would look like rows lost to paging.
     *
     * <p>{@code handoverDate} is the odd one and the reason this maps to expressions rather than to
     * property names: the handover register shows the unload day when it is known and the movement
     * day otherwise, in one column. Sorting by the raw {@code unloadDate} would have sent to the
     * back exactly the rows that are on screen showing a date — press the header, and the rows do
     * not line up with the figures written in them.
     */
    private static final Map<String, BiFunction<Root<WasteMovement>, CriteriaBuilder, Expression<?>>> SORTABLE =
            Map.of(
                    "date", (root, cb) -> root.get("date"),
                    "handoverDate", (root, cb) -> cb.coalesce(root.get("unloadDate"), root.get("date")),
                    "wasteCode", (root, cb) -> root.join("wasteCode", JoinType.LEFT).get("code"),
                    "quantity", (root, cb) -> root.get("quantity"),
                    "partnerName", (root, cb) -> root.join("partner", JoinType.LEFT).get("name"),
                    "workPointName", (root, cb) -> root.join("workPoint", JoinType.LEFT).get("name"));

    /**
     * The order, written into the query itself rather than handed over as a {@link Sort}.
     *
     * <p>Not a preference: {@code Sort.Order.nullsLast()} is <b>silently dropped</b> by Spring Data
     * when the query is built from a {@code Specification} — {@code QueryUtils.toJpaOrder} reads
     * the direction and ignores the null handling. It compiles, it runs, and the empty quantities
     * come back first. Found by the test, which is the only place it could have been found.
     *
     * <p>Safe to set here: Spring Data clears the ordering of the count query after applying the
     * specification, precisely so that specifications may carry one.
     */
    private Specification<WasteMovement> ordered(Specification<WasteMovement> spec,
                                                 String sortKey, boolean ascending) {
        return (root, query, cb) -> {
            Predicate where = spec.toPredicate(root, query, cb);
            BiFunction<Root<WasteMovement>, CriteriaBuilder, Expression<?>> column =
                    sortKey == null ? null : SORTABLE.get(sortKey);
            List<jakarta.persistence.criteria.Order> orders = new java.util.ArrayList<>();
            if (column != null) {
                Expression<?> sorted = column.apply(root, cb);
                /*
                 * Lipsa stă la coadă în ambele sensuri. „De cântărit" nu e nici cea mai mică, nici
                 * cea mai mare cantitate: e nespusă, și n-are ce căuta printre cifre la niciun
                 * capăt. La fel o mișcare fără partener, sortată după partener. E `missingLast` din
                 * `useTableView.ts`, scris aici ca o coloană 0/1 dinaintea celei adevărate — forma
                 * portabilă a lui NULLS LAST.
                 */
                orders.add(cb.asc(cb.selectCase().when(cb.isNull(sorted), 1).otherwise(0)));
                orders.add(ascending ? cb.asc(sorted) : cb.desc(sorted));
            } else {
                // Ce vede omul la deschiderea ecranului: cele mai noi mișcări primele.
                orders.add(cb.desc(root.get("date")));
            }
            /*
             * `createdAt` închide orice sortare, și nu de dragul simetriei: două rânduri egale pe
             * coloana sortată sunt libere să-și schimbe locul între două cereri, iar atunci unul
             * apare pe pagina 1 și pe pagina 2, iar altul nu apare niciodată. Ordinea unei liste
             * paginate trebuie să fie totală, altfel paginile nu descriu aceeași listă.
             */
            orders.add(cb.desc(root.get("createdAt")));
            query.orderBy(orders);
            return where;
        };
    }

    /**
     * Dynamic, tenant-scoped filter. Building predicates only for present filters avoids
     * binding typed nulls (which Postgres rejects with "could not determine data type").
     *
     * <p>No {@code orderBy} here any more: the order lives in the {@link Pageable}. A
     * {@code Specification} is applied to the count query too, and an {@code order by} on a
     * {@code select count(*)} is at best ignored and at worst rejected.
     */
    private Specification<WasteMovement> buildFilter(UUID tenantId, UUID workPointId,
                                                     UUID wasteCodeId, LocalDate fromDate, LocalDate toDate,
                                                     boolean leftSite, boolean missingOperationCode,
                                                     WasteRegister register, MovementDirection direction,
                                                     PackagingFilter packaging) {
        java.util.Set<UUID> allowed = depotAccess.allowed();
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("company").get("id"), tenantId));
            predicates.add(cb.isFalse(root.get("deleted")));
            // Liniile unei operațiuni de cântar în lucru sau anulate sunt ciorne, nu evidență (D1.3):
            // nici lista, nici totalurile de deasupra ei nu le arată.
            Join<WasteMovement, WeighingOperation> weighing = root.join("weighingOperation", JoinType.LEFT);
            predicates.add(cb.or(cb.isNull(weighing.get("id")),
                    weighing.get("status").in(WeighingOperationStatus.FINALIZED, WeighingOperationStatus.IN_TRANSIT)));
            // D2.5 — pe firmă, transferul intern se anulează cu el însuși: nu e nici intrare, nici ieșire. Pe un
            // depozit ales se vede, ca intrare (primit) sau ieșire (trimis).
            if (workPointId == null) {
                predicates.add(cb.not(root.get("operation").in(WasteOperation.TRANSFERRED_OUT, WasteOperation.TRANSFERRED_IN)));
            }
            if (workPointId != null) {
                predicates.add(cb.equal(root.get("workPoint").get("id"), workPointId));
            }
            // D2.4 — un utilizator restrâns vede doar rândurile depozitelor lui, oricare ar fi filtrul cerut.
            if (allowed != null) {
                predicates.add(allowed.isEmpty() ? cb.disjunction() : root.get("workPoint").get("id").in(allowed));
            }
            if (wasteCodeId != null) {
                predicates.add(cb.equal(root.get("wasteCode").get("id"), wasteCodeId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), toDate));
            }
            /*
             * Ce a plecat de pe amplasament — întrebarea registrului de predări.
             *
             * <p>Cele trei nu sunt `WasteOperation.isExit()`, și diferența e voită: `isExit()`
             * răspunde la „are nevoie de cod R/D?", iar `UNCLASSIFIED_OUT` tocmai n-are unul.
             * Registrul întreabă altceva — ce a ieșit pe poartă — și rândurile fără cod sunt exact
             * cele pe care le caută cineva care vrea să le repare.
             */
            if (leftSite) {
                predicates.add(root.get("operation").in(
                        WasteOperation.RECOVERED, WasteOperation.DISPOSED, WasteOperation.UNCLASSIFIED_OUT));
            }
            // „Arată-mi doar ce blochează depunerea", trimis prin adresă de pe Panou.
            if (missingOperationCode) {
                predicates.add(cb.isNull(root.get("operationCode")));
            }
            // Ecranul „Generare" (Anexa 1) și cele de art. 48 — proprietarul, 14.09.2026.
            if (register != null) {
                predicates.add(cb.equal(root.get("register"), register));
            }
            // „Intrări" / „Ieșiri", separate (proprietarul, 15.09.2026). `OUT` e același set ca
            // `leftSite`: și rândul fără cod a ieșit pe poartă.
            if (direction == MovementDirection.IN) {
                predicates.add(root.get("operation").in(WasteOperation.COLLECTED, WasteOperation.TRANSFERRED_IN));
            } else if (direction == MovementDirection.OUT) {
                predicates.add(root.get("operation").in(WasteOperation.RECOVERED, WasteOperation.DISPOSED,
                        WasteOperation.UNCLASSIFIED_OUT, WasteOperation.TRANSFERRED_OUT));
            }
            /*
             * Tastele de ambalaje de pe „Mișcări" (18.09.2026), de când tabul „Ambalaje" nu-și mai
             * ține propriul registru. Aceleași trei întrebări pe care le punea el, puse listei.
             *
             * <p>`like '15 01%'` e chiar `PackagingMaterial.isPackagingCode`, scris în SQL: codul e
             * un șir cu spații, iar capitolul se citește din primele cinci caractere.
             */
            if (packaging != null) {
                Path<String> code = root.get("wasteCode").get("code");
                predicates.add(cb.like(code, "15 01%"));
                if (packaging != PackagingFilter.ANY) {
                    // Bifa neatinsă se comportă ca „da", ca în `WasteMovementMapper`: un rând de
                    // dinaintea întrebării intră în declarație până când cineva spune altceva.
                    predicates.add(cb.or(cb.isNull(root.get("packagingOnMarket")),
                            cb.isTrue(root.get("packagingOnMarket"))));
                }
                if (packaging == PackagingFilter.INCOMPLETE) {
                    predicates.add(cb.or(
                            cb.and(cb.isNull(root.get("packagingMaterial")),
                                    cb.not(code.in(PackagingMaterial.settledCodes()))),
                            cb.isNull(root.get("packagingCategory"))));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
