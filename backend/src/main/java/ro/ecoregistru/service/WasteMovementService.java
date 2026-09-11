package ro.ecoregistru.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ro.ecoregistru.controller.request.RecordWeightRequest;
import ro.ecoregistru.controller.request.WasteMovementRequest;
import ro.ecoregistru.controller.response.AttachmentResponse;
import ro.ecoregistru.controller.response.MovementSummaryResponse;
import ro.ecoregistru.controller.response.PageResponse;
import ro.ecoregistru.controller.response.WasteMovementResponse;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.mapper.WasteMovementMapper;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.Set;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * Tenant-scoped CRUD for waste movements. Every query is filtered by the current
 * tenant (from TenantContext) so cross-tenant access is impossible.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WasteMovementService {

    /**
     * Cât are voie să aibă un atașament. Nu e un prag ales de noi: contul Cloudinary e pe planul
     * Free, unde limita e <b>10 MB per asset</b> — și pentru {@code image}, unde ajung PDF-urile,
     * și pentru {@code raw}, unde ajung Word și Excel. Peste ea uploadul cade <i>la furnizor</i>,
     * după ce a trecut de tot ce e al nostru, iar omul primește o eroare care nu spune nimic.
     *
     * <p>Verificarea stă aici, nu doar în dropzone. Până acum singurul gardian era JavaScript-ul
     * din browser — {@code file-dropzone.tsx}, și acela la 15 MB, adică <i>peste</i> zidul de la
     * Cloudinary — deci un {@code curl} direct pe API trecea cu orice încăpea în limita de
     * multipart, iar un fișier de 12 MB trecea chiar și prin interfață ca să cadă la capăt.
     */
    public static final long MAX_ATTACHMENT_BYTES = 10L * 1024 * 1024;

    WasteMovementRepository movementRepository;
    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;
    WasteCodeRepository wasteCodeRepository;
    PartnerRepository partnerRepository;
    PartnerWorkPointRepository partnerWorkPointRepository;
    InternalGeneratorRepository internalGeneratorRepository;
    AttachmentRepository attachmentRepository;
    AnalysisBulletinRepository bulletinRepository;
    CloudinaryStorageService storageService;
    ro.ecoregistru.service.export.Anexa3FormGenerator anexa3FormGenerator;
    ro.ecoregistru.service.export.Anexa2FormGenerator anexa2FormGenerator;
    Anexa2ThresholdCalculator anexa2ThresholdCalculator;
    WasteMovementMapper mapper;

    @Transactional
    public WasteMovementResponse create(WasteMovementRequest request) {
        UUID tenantId = TenantContext.require();

        // Idempotency: if the client already created this movement, return it unchanged.
        if (request.clientGeneratedId() != null) {
            var existing = movementRepository
                    .findByCompany_IdAndClientGeneratedId(tenantId, request.clientGeneratedId());
            if (existing.isPresent()) {
                return mapper.toResponse(existing.get(), codesWithBulletin(tenantId));
            }
        }

        Company company = requireCompany(tenantId);
        WorkPoint workPoint = requireWorkPoint(request.workPointId(), tenantId);
        WasteCode wasteCode = requireWasteCode(request.wasteCodeId());
        Partner partner = resolvePartner(request, tenantId);
        InternalGenerator internalGenerator = resolveInternalGenerator(request, tenantId, workPoint);
        validateOperation(request, company);
        validateOperationCode(request);
        validateAgainstProfile(request, company);
        validateQuantity(request);
        Partner carrier = resolveCarrier(request, tenantId);
        WasteRegister register = resolveRegister(request, company);

        WasteMovement movement = WasteMovement.builder()
                .company(company)
                .workPoint(workPoint)
                .date(request.date())
                .wasteCode(wasteCode)
                .quantity(request.quantity())
                .weighedAtUnloading(request.weighedAtUnloading())
                .volumeM3(request.volumeM3())
                .unit(request.unit())
                .operation(request.operation())
                .register(register)
                .physicalState(request.physicalState())
                .storageType(request.storageType())
                .treatmentMethod(request.treatmentMethod())
                .transportMeans(request.transportMeans())
                .wasteDestination(request.wasteDestination())
                .operationCode(request.operationCode())
                .partner(partner)
                .internalGenerator(internalGenerator)
                .unloadDate(request.unloadDate())
                .partnerWorkPoint(resolvePartnerWorkPoint(request, tenantId, partner))
                .anexa3Unit(request.anexa3Unit())
                .anexa2Number(request.anexa2Number())
                .anexa2ApprovalNumber(request.anexa2ApprovalNumber())
                .anexa2Packaging(request.anexa2Packaging())
                .anexa2BelowOneTon(request.anexa2BelowOneTon())
                // Ambalaje: cele trei rubrici ale tabelului 1 călătoresc pe mişcare, dar numai pe
                // un cod 15 01 xx. Pe orice alt cod se ignoră, ca să nu rămână un răspuns agăţat
                // de o mişcare pe care declaraţia n-o citeşte niciodată.
                .packagingOnMarket(packagingOnly(wasteCode, request.packagingOnMarket()))
                .packagingMaterial(packagingOnly(wasteCode, request.packagingMaterial()))
                .packagingCategory(packagingOnly(wasteCode, request.packagingCategory()))
                .packagingReusable(packagingOnly(wasteCode, request.packagingReusable()))
                .packagingHazardousContent(
                        packagingOnly(wasteCode, request.packagingHazardousContent()))
                .packagingOrigin(packagingOnly(wasteCode, request.packagingOrigin()))
                .transportPartner(carrier)
                .driverName(request.driverName())
                .driverIdentification(request.driverIdentification())
                .vehicleRegistration(request.vehicleRegistration())
                .transportDestinations(request.transportDestinations() == null
                        ? new java.util.LinkedHashSet<>()
                        : new java.util.LinkedHashSet<>(request.transportDestinations()))
                .documentReference(request.documentReference())
                .notes(request.notes())
                .clientGeneratedId(request.clientGeneratedId())
                .deleted(false)
                .createdBy(SecurityUtils.currentUser().getId())
                .build();

        movementRepository.save(movement);
        return mapper.toResponse(movement, codesWithBulletin(tenantId));
    }

    @Transactional
    public WasteMovementResponse update(UUID id, WasteMovementRequest request) {
        UUID tenantId = TenantContext.require();
        WasteMovement movement = requireMovement(id, tenantId);

        Company company = requireCompany(tenantId);
        WorkPoint workPoint = requireWorkPoint(request.workPointId(), tenantId);
        WasteCode wasteCode = requireWasteCode(request.wasteCodeId());
        Partner partner = resolvePartner(request, tenantId);
        InternalGenerator internalGenerator = resolveInternalGenerator(request, tenantId, workPoint);
        validateOperation(request, company);
        validateOperationCode(request);
        validateAgainstProfile(request, company);
        validateQuantity(request);
        Partner carrier = resolveCarrier(request, tenantId);
        WasteRegister register = resolveRegister(request, company);

        movement.setWorkPoint(workPoint);
        movement.setDate(request.date());
        movement.setWasteCode(wasteCode);
        movement.setQuantity(request.quantity());
        movement.setWeighedAtUnloading(request.weighedAtUnloading());
        movement.setVolumeM3(request.volumeM3());
        movement.setUnit(request.unit());
        movement.setOperation(request.operation());
        movement.setRegister(register);
        movement.setPhysicalState(request.physicalState());
        movement.setStorageType(request.storageType());
        movement.setTreatmentMethod(request.treatmentMethod());
        movement.setTransportMeans(request.transportMeans());
        movement.setWasteDestination(request.wasteDestination());
        movement.setOperationCode(request.operationCode());
        movement.setPartner(partner);
        movement.setInternalGenerator(internalGenerator);
        movement.setUnloadDate(request.unloadDate());
        movement.setPartnerWorkPoint(resolvePartnerWorkPoint(request, tenantId, partner));
        movement.setAnexa3Unit(request.anexa3Unit());
        movement.setAnexa2Number(request.anexa2Number());
        movement.setAnexa2ApprovalNumber(request.anexa2ApprovalNumber());
        movement.setAnexa2Packaging(request.anexa2Packaging());
        movement.setAnexa2BelowOneTon(request.anexa2BelowOneTon());
        movement.setPackagingOnMarket(packagingOnly(wasteCode, request.packagingOnMarket()));
        movement.setPackagingMaterial(packagingOnly(wasteCode, request.packagingMaterial()));
        movement.setPackagingCategory(packagingOnly(wasteCode, request.packagingCategory()));
        movement.setPackagingReusable(packagingOnly(wasteCode, request.packagingReusable()));
        movement.setPackagingHazardousContent(
                packagingOnly(wasteCode, request.packagingHazardousContent()));
        movement.setPackagingOrigin(packagingOnly(wasteCode, request.packagingOrigin()));
        movement.setTransportPartner(carrier);
        movement.setDriverName(request.driverName());
        movement.setDriverIdentification(request.driverIdentification());
        movement.setVehicleRegistration(request.vehicleRegistration());
        movement.setTransportDestinations(request.transportDestinations() == null
                ? new java.util.LinkedHashSet<>()
                : new java.util.LinkedHashSet<>(request.transportDestinations()));
        movement.setDocumentReference(request.documentReference());
        movement.setNotes(request.notes());

        return mapper.toResponse(movement, codesWithBulletin(tenantId));
    }

    /**
     * Fills in the weight the recipient sent back, for a load that left without one.
     *
     * <p>Deliberately narrow: it touches the quantity and, if the figure came back in another
     * unit, the unit. Everything else stays, {@code weighedAtUnloading} included — that flag says
     * how this load was weighed, and it is still true once the number arrives. The evidence line
     * stops being provisional because {@code EvidenceCalculator} reads the quantity, not the flag.
     *
     * <p>Refused when there is already a quantity: changing a figure that is on a printed Anexa 3
     * is an edit, and edits go through the form where the whole movement is visible.
     */
    @Transactional
    public WasteMovementResponse recordWeight(UUID id, RecordWeightRequest request) {
        UUID tenantId = TenantContext.require();
        WasteMovement movement = requireMovement(id, tenantId);

        if (movement.getQuantity() != null) {
            throw new BusinessException(NOT_AWAITING_WEIGHING);
        }
        if (request.quantity() == null || request.quantity().signum() <= 0) {
            throw new BusinessException(INVALID_QUANTITY);
        }
        movement.setQuantity(request.quantity());
        if (request.unit() != null) {
            movement.setUnit(request.unit());
        }
        return mapper.toResponse(movement, codesWithBulletin(tenantId));
    }

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
                                                    boolean missingOperationCode, String search,
                                                    int page, int size, String sortKey, boolean ascending) {
        UUID tenantId = TenantContext.require();
        LocalDate fromDate = null;
        LocalDate toDate = null;
        /*
         * Anul fără lună înseamnă anul întreg. Până acum nu însemna nimic: se cerea `?year=2026`
         * fără lună și veneau înapoi toate mișcările, din toți anii — o filtrare care se ignora în
         * tăcere, deci mai rea decât una respinsă.
         *
         * Treapta rămâne după ce căutarea s-a mutat pe server, dar din alt motiv decât înainte:
         * acum nu mai apără ecranul de „tot" (paginarea o face), ci spune ce se caută — „luna asta"
         * sau „anul ăsta". Cine caută o predare de acum trei luni lărgește filtrul la an, nu
         * nimerește luna din prima.
         */
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

        Specification<WasteMovement> filter = buildFilter(tenantId, workPointId, wasteCodeId,
                fromDate, toDate, leftSite, missingOperationCode);
        Specification<WasteMovement> spec = ordered(withSearch(filter, search), sortKey, ascending);
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size));

        // Read once for the whole page, not per row: the mirror-code badge asks whether this
        // tenant holds an analysis bulletin for the code, and a query per row would be an N+1 on
        // the most-opened screen in the application.
        Set<String> covered = codesWithBulletin(tenantId);
        return PageResponse.of(movementRepository.findAll(spec, pageable), m -> mapper.toResponse(m, covered));
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
                                                     boolean leftSite, boolean missingOperationCode) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("company").get("id"), tenantId));
            predicates.add(cb.isFalse(root.get("deleted")));
            if (workPointId != null) {
                predicates.add(cb.equal(root.get("workPoint").get("id"), workPointId));
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
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public WasteMovementResponse get(UUID id) {
        UUID tenantId = TenantContext.require();
        return mapper.toResponse(requireMovement(id, tenantId), codesWithBulletin(tenantId));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.require();
        WasteMovement movement = requireMovement(id, tenantId);
        movement.setDeleted(true);
        movement.setDeletedAt(Instant.now());
        movement.setDeletedBy(SecurityUtils.currentUser().getId());
    }

    @Transactional
    public AttachmentResponse addAttachment(UUID movementId, MultipartFile file) {
        UUID tenantId = TenantContext.require();
        WasteMovement movement = requireMovement(movementId, tenantId);
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new BadRequestException(ATTACHMENT_TOO_LARGE);
        }

        var stored = storageService.upload(file, "movements/" + movementId);
        Attachment attachment = Attachment.builder()
                .movement(movement)
                .url(stored.url())
                .publicId(stored.publicId())
                .resourceType(stored.resourceType())
                .deliveryType(stored.deliveryType())
                .format(stored.format())
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .createdAt(Instant.now())
                .build();
        movement.getAttachments().add(attachment);
        attachmentRepository.save(attachment);
        return mapper.toAttachmentResponse(attachment);
    }

    @Transactional
    public void deleteAttachment(UUID movementId, UUID attachmentId) {
        UUID tenantId = TenantContext.require();
        requireMovement(movementId, tenantId); // enforces tenant ownership
        Attachment attachment = attachmentRepository.findByIdAndMovement_Id(attachmentId, movementId)
                .orElseThrow(() -> new NotFoundException(MOVEMENT_NOT_FOUND));
        storageService.delete(attachment.getPublicId(), attachment.getResourceType(),
                attachment.getDeliveryType());
        attachmentRepository.delete(attachment);
    }

    /**
     * The bytes of an attachment, for a caller who has already proved they may see the movement.
     *
     * <p>This is the whole of 11-bis. Before it, {@code AttachmentResponse} carried Cloudinary's
     * {@code secure_url} and the browser fetched the file straight from the CDN — no session, no
     * tenant check, forever, on an application through which handover notes, contracts and
     * drivers' identity documents pass, and through which CNPs will pass at Etapa 9. The tenant
     * check now happens here, in the same place and the same way as every other read: {@link
     * #requireMovement} scopes by {@code TenantContext}, so another company's attachment is a 404
     * and not a file.
     */
    @Transactional(readOnly = true)
    public AttachmentContent attachmentContent(UUID movementId, UUID attachmentId) {
        UUID tenantId = TenantContext.require();
        requireMovement(movementId, tenantId); // enforces tenant ownership
        Attachment attachment = attachmentRepository.findByIdAndMovement_Id(attachmentId, movementId)
                .orElseThrow(() -> new NotFoundException(ATTACHMENT_NOT_FOUND));
        try {
            return new AttachmentContent(
                    storageService.fetch(deliveryUrl(attachment)),
                    attachment.getContentType(),
                    attachment.getFileName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ATTACHMENT_FETCH_FAILED.getMessage(), e);
        } catch (Exception e) {
            log.warn("Attachment fetch failed for id={}", attachmentId, e);
            throw new IllegalStateException(ATTACHMENT_FETCH_FAILED.getMessage(), e);
        }
    }

    /**
     * Where to read the file from. Rows uploaded since 11-bis are {@code authenticated} and get a
     * freshly signed URL; older rows have null coordinates and keep their stored URL, which is
     * public — that is the migration's own note, and the reason those files are worth re-uploading
     * rather than left alone.
     */
    private String deliveryUrl(Attachment a) {
        if (a.getDeliveryType() == null) {
            return a.getUrl();
        }
        return storageService.signedUrl(a.getPublicId(), a.getResourceType(),
                a.getDeliveryType(), a.getFormat());
    }

    public record AttachmentContent(byte[] bytes, String contentType, String fileName) {}

    /**
     * Renders Anexa 3 la HG 1061/2008 for a movement that is already recorded, allocating the
     * form's number the first time so a reprint stays the same document.
     *
     * <p>Two refusals, both legal rather than technical. The form covers a handover — it names an
     * expeditor and a destinatar — so a movement with no partner has nobody to print on the right
     * half. And its title says <em>nepericuloase</em>: a hazardous code belongs on the expedition
     * form of anexa 2, which is a different document we do not produce yet, so we say that instead
     * of printing the wrong one.
     *
     * <p><b>And one thing that is deliberately not a third refusal</b> (audit point 5, decided
     * 04.09.2026): an expired recipient authorization. Both refusals above say "this is the wrong
     * document"; a lapsed authorization does not make Anexa 3 the wrong document, because the
     * handover really did happen and the client still has to be able to reconstruct an old file.
     * Refusing would mean the application declines to document reality. The warning is carried
     * instead on {@code WasteMovementResponse#recipientAuthorizationExpired} and shown on screen.
     *
     * <p><b>It must not be printed on the form.</b> The paper goes to the recipient and, at an
     * inspection, to the authority — a note of ours in its margin would be our own accusation
     * filed in the client's own dossier. On screen it is information; on paper it would be
     * evidence against the person we built it for.
     */
    @Transactional
    public byte[] renderAnexa3(UUID id) {
        UUID tenantId = TenantContext.require();
        WasteMovement movement = requireMovement(id, tenantId);
        Company company = requireCompany(tenantId);

        if (!movement.getOperation().isExit() || movement.getPartner() == null) {
            throw new BusinessException(ANEXA3_REQUIRES_HANDOVER);
        }
        if (movement.getWasteCode().isHazardous()) {
            throw new BusinessException(ANEXA3_HAZARDOUS_NOT_ALLOWED);
        }
        if (movement.getAnexa3Number() == null) {
            Integer max = movementRepository.findMaxAnexa3Number(tenantId);
            movement.setAnexa3Number(max == null ? 1 : max + 1);
            movement.setAnexa3Series(company.getAnexa3Series());
        }
        return anexa3FormGenerator.render(movement, company);
    }

    /**
     * Anexa 2 la HG 1061/2008, the hazardous-waste consignment form, as a PDF.
     *
     * <p>The mirror image of {@link #renderAnexa3(UUID)}, refusal for refusal — and deliberately
     * so, because the two forms are the two halves of the same question and a client who lands on
     * the wrong one should be told which is right, not told "no".
     *
     * <p><b>Three refusals, and each one says "this is the wrong document":</b>
     * <ul>
     *   <li><b>No handover.</b> The form names an expeditor and a destinatar and records a
     *       consignment; without a recipient there is nothing to consign.</li>
     *   <li><b>A non-hazardous code.</b> The title says <em>periculoase</em>. This is the same
     *       refusal Anexa 3 makes in the other direction, and as of this slice both of them can
     *       finally name a form that exists.</li>
     *   <li><b>Medical waste.</b> Art. 24 is not a variant of this flow, it is a different one: the
     *       <em>carrier</em> draws the forms up — "chiar dacă acesta este şi destinatar" — on the
     *       cumulated quantity of one round through an area, with a schedule of the individual
     *       expeditors attached. A clinic that printed this form as expeditor would be holding a
     *       document nobody asked it for. We do not produce the art. 24 one either: it belongs to
     *       the carrier and is built from a route we do not record.</li>
     * </ul>
     *
     * <p><b>Nothing is allocated here</b>, which is the one structural difference from Anexa 3.
     * There is no number to hand out: the model reserves it for the county agency. So this method
     * writes nothing to the movement, and a reprint is simply the same PDF again.
     */
    @Transactional
    public byte[] renderAnexa2(UUID id) {
        UUID tenantId = TenantContext.require();
        WasteMovement movement = requireMovement(id, tenantId);
        Company company = requireCompany(tenantId);

        if (!movement.getOperation().isExit() || movement.getPartner() == null) {
            throw new BusinessException(ANEXA2_REQUIRES_HANDOVER);
        }
        if (!movement.getWasteCode().isHazardous()) {
            throw new BusinessException(ANEXA2_NOT_HAZARDOUS);
        }
        if (isMedicalWaste(movement)) {
            throw new BusinessException(ANEXA2_MEDICAL_WASTE);
        }
        return anexa2FormGenerator.render(movement, company,
                anexa2ThresholdCalculator.belowOneTon(movement));
    }

    /**
     * What the "&lt; 1t/an" tick is proposed from, for the screen to show beside it.
     *
     * <p>Read-only and computed on demand rather than stored: the total moves whenever a movement
     * on the same code is recorded, edited or deleted, so a figure frozen onto this row would go
     * quietly stale — and stale in the direction that costs, since a total can only grow through
     * the threshold, never back.
     */
    @Transactional
    public ro.ecoregistru.controller.response.Anexa2ThresholdResponse anexa2Threshold(UUID id) {
        UUID tenantId = TenantContext.require();
        WasteMovement movement = requireMovement(id, tenantId);
        return anexa2ThresholdCalculator.forMovement(movement);
    }

    /**
     * Whether this is hazardous waste from medical activity, which art. 24 routes away from this
     * form altogether.
     *
     * <p>Chapter 18 of the nomenclator is "deşeuri rezultate din activităţi de îngrijire a
     * sănătăţii umane sau veterinare şi/sau din cercetări conexe", so a hazardous code in it is
     * exactly the subject of art. 24. Matching on the chapter rather than on a list of codes is
     * deliberate: a list would have to be maintained against the nomenclator, and forgetting one
     * would print the wrong document for a clinic.
     */
    private boolean isMedicalWaste(WasteMovement movement) {
        String code = movement.getWasteCode().getCode();
        return code != null && code.startsWith("18");
    }

    // --- helpers ---

    private Company requireCompany(UUID tenantId) {
        return companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(TENANT_NOT_FOUND));
    }

    private WasteMovement requireMovement(UUID id, UUID tenantId) {
        return movementRepository.findByIdAndCompany_IdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new NotFoundException(MOVEMENT_NOT_FOUND));
    }

    private WorkPoint requireWorkPoint(UUID id, UUID tenantId) {
        return workPointRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND));
    }

    private WasteCode requireWasteCode(UUID id) {
        return wasteCodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(WASTE_CODE_NOT_FOUND));
    }

    /**
     * Keeps the three Anexa 1 Ambalaje answers only where they mean something — on a
     * {@code 15 01 xx} code. Moving a movement off a packaging code therefore clears them, instead
     * of leaving an answer behind that no document reads and nobody would think to correct.
     */
    private <T> T packagingOnly(WasteCode wasteCode, T value) {
        return PackagingMaterial.isPackagingCode(wasteCode.getCode()) ? value : null;
    }

    /**
     * The partner is optional on every operation: it names "agentul economic care efectueaza
     * operatia" of Anexa 1 cap. 3 / cap. 4 when that is not this company. Handing waste over is a
     * RECOVERED or DISPOSED with a partner named, which is why nothing requires one any more.
     */
    private Partner resolvePartner(WasteMovementRequest request, UUID tenantId) {
        if (request.partnerId() == null) {
            return null;
        }
        return partnerRepository.findByIdAndCompany_Id(request.partnerId(), tenantId)
                .orElseThrow(() -> new NotFoundException(PARTNER_NOT_FOUND));
    }

    /**
     * The quantity is required, unless the recipient is the one who weighs the load.
     *
     * <p>A shop that hands its cardboard to a collector has no weighbridge: the collector weighs it
     * at the depot and the figure comes back afterwards. The filled Anexa 3 model works exactly
     * that way — the quantity on it is written in by hand. Recording a zero, or an estimate, would
     * put a made-up number both on an official transport form and in the Anexa 1 stock, so the
     * quantity simply stays empty and the evidence line says it is provisional.
     */
    private void validateQuantity(WasteMovementRequest request) {
        if (request.weighedAtUnloading()) {
            // Somebody has to do the weighing, and it is the party taking the waste over.
            if (request.partnerId() == null) {
                throw new BusinessException(WEIGHING_NEEDS_RECIPIENT);
            }
            return;
        }
        if (request.quantity() == null) {
            throw new BusinessException(QUANTITY_REQUIRED);
        }
    }

    /**
     * The recipient's work point that took the load, if one was picked.
     *
     * <p>Refused when it belongs to a different partner than the one receiving the waste: an Anexa
     * 3 naming one company and another company's depot is a form nobody can follow back.
     */
    private PartnerWorkPoint resolvePartnerWorkPoint(WasteMovementRequest request, UUID tenantId,
                                                     Partner partner) {
        if (request.partnerWorkPointId() == null) {
            return null;
        }
        PartnerWorkPoint workPoint = partnerWorkPointRepository
                .findByIdAndPartner_Company_Id(request.partnerWorkPointId(), tenantId)
                .orElseThrow(() -> new NotFoundException(PARTNER_NOT_FOUND));
        if (partner == null || !workPoint.getPartner().getId().equals(partner.getId())) {
            throw new BusinessException(PARTNER_WORK_POINT_MISMATCH);
        }
        return workPoint;
    }

    /** The carrier named on the transport form; null means we haul it ourselves. */
    private Partner resolveCarrier(WasteMovementRequest request, UUID tenantId) {
        if (request.transportPartnerId() == null) {
            return null;
        }
        return partnerRepository.findByIdAndCompany_Id(request.transportPartnerId(), tenantId)
                .orElseThrow(() -> new NotFoundException(PARTNER_NOT_FOUND));
    }

    /**
     * Resolves the section the waste came from, and refuses one belonging to another work point:
     * "Sectia" is printed on the Anexa 1 sheet of a work point, so a section from elsewhere would
     * put a source on the form that never produced the waste.
     */
    private InternalGenerator resolveInternalGenerator(WasteMovementRequest request, UUID tenantId,
                                                       WorkPoint workPoint) {
        if (request.internalGeneratorId() == null) {
            return null;
        }
        InternalGenerator generator = internalGeneratorRepository
                .findByIdAndCompany_Id(request.internalGeneratorId(), tenantId)
                .orElseThrow(() -> new NotFoundException(INTERNAL_GENERATOR_NOT_FOUND));
        if (!generator.getWorkPoint().getId().equals(workPoint.getId())) {
            throw new BusinessException(INTERNAL_GENERATOR_WRONG_WORK_POINT);
        }
        return generator;
    }

    /**
     * Keeps the operation within what this kind of company may record. The screen already offers
     * only those, so this is the server-side half of the same rule: a generator has no art. 48
     * register and therefore no takeovers to record, and UNCLASSIFIED_OUT is a migration state
     * rather than a choice.
     */
    private void validateOperation(WasteMovementRequest request, Company company) {
        WasteOperation operation = request.operation();
        if (!operation.isSelectable()) {
            throw new BusinessException(OPERATION_NOT_SELECTABLE);
        }
        if (!company.getType().allowedOperations().contains(operation)) {
            // COLLECTED is the only type-gated operation today, and there is already a message
            // that names the fix ("switch the company to Colector or Ambele"). Prefer it; the
            // generic one is here for whatever the set gains later.
            throw new BusinessException(operation == WasteOperation.COLLECTED
                    ? ART48_REGISTER_NOT_ENABLED
                    : OPERATION_NOT_ALLOWED_FOR_COMPANY_TYPE);
        }
    }

    /**
     * Enforces the R/D operation code rule. Every movement that takes waste off the site carries
     * one, because Anexa 1 cap. 3 and cap. 4 report the quantity together with "Operaţia de
     * valorificare"/"de eliminare" and the operator performing it — a quantity cannot be placed on
     * those chapters without its code (docs/surse-oficiale.md §1.2).
     *
     * <p>The family is pinned by the operation: an R code for RECOVERED, a D code for DISPOSED —
     * including when a partner performs it, which is how a handover is recorded. GENERATED and
     * COLLECTED take none: nothing has happened to the waste yet.
     */
    private void validateOperationCode(WasteMovementRequest request) {
        var code = request.operationCode();
        switch (request.operation()) {
            case RECOVERED -> {
                if (code == null || !code.isRecovery()) {
                    throw new BusinessException(OPERATION_CODE_REQUIRED_RECOVERY);
                }
            }
            case DISPOSED -> {
                if (code == null || !code.isDisposal()) {
                    throw new BusinessException(OPERATION_CODE_REQUIRED_DISPOSAL);
                }
            }
            default -> {
                if (code != null) {
                    throw new BusinessException(OPERATION_CODE_NOT_ALLOWED);
                }
            }
        }
    }

    /**
     * Keeps the R/D code within the operations this account said it works with, on its intake
     * form. The screen offers only those, so this is the server-side half of the same rule.
     *
     * <p>An empty profile means the form has not been answered, not that nothing is allowed:
     * every account that existed before the profile did has one, and refusing their movements
     * would break accounts that are working today.
     */
    private void validateAgainstProfile(WasteMovementRequest request, Company company) {
        var allowed = company.getAuthorizedOperationCodes();
        if (request.operationCode() == null || allowed == null || allowed.isEmpty()) {
            return;
        }
        if (!allowed.contains(request.operationCode())) {
            throw new BusinessException(OPERATION_CODE_NOT_IN_PROFILE);
        }
    }

    /**
     * Decides which legal register the quantity lands in. The caller may say, because one case is
     * genuinely ambiguous — handing over, recovering or disposing of goods taken from third parties
     * belongs to the art. 48 register, not to Anexa 1 — but the two ends are fixed by law and are
     * enforced rather than trusted: waste generated in the company's own activity is always Anexa 1
     * (art. 1 alin. (1) HG 856/2002), and a takeover is never Anexa 1 (art. 2 alin. (1)).
     */
    private WasteRegister resolveRegister(WasteMovementRequest request, Company company) {
        boolean takeover = request.operation() == WasteOperation.COLLECTED;

        // Ieşirea e singurul loc unde implicitul minţea. Un colector care valorifică marfă preluată
        // înregistra RECOVERED, nimeni nu-l întreba nimic, iar cantitatea cădea pe Anexa 1 — adică
        // se declara drept ambalaj pus pe piaţă de el. Proba, 25.08.2026: 1000 kg de 15 01 01 luaţi
        // de la un magazin şi valorificaţi apăreau în tabelul 1 al Anexei 1 Ambalaje, iar generarea
        // dedusă din V24 îi mai spunea şi „generate de tine". Deci se întreabă, nu se presupune —
        // şi numai la firmele care chiar pot prelua, ca generatorul pur să nu vadă o întrebare
        // fără sens.
        if (request.operation().isExit()
                && company.getType().keepsArt48Register()
                && request.register() == null) {
            throw new BusinessException(REGISTER_REQUIRED_ON_EXIT);
        }

        WasteRegister register = request.register() != null
                ? request.register()
                : (takeover ? WasteRegister.ART_48 : WasteRegister.ANEXA_1);

        if (takeover && register != WasteRegister.ART_48) {
            throw new BusinessException(REGISTER_INVALID_FOR_OPERATION);
        }
        if (request.operation() == WasteOperation.GENERATED && register != WasteRegister.ANEXA_1) {
            throw new BusinessException(REGISTER_INVALID_FOR_OPERATION);
        }
        if (register == WasteRegister.ART_48 && !company.getType().keepsArt48Register()) {
            throw new BusinessException(ART48_REGISTER_NOT_ENABLED);
        }
        return register;
    }

    /**
     * The waste codes this tenant holds an analysis bulletin for — the clean source of the
     * mirror-code check since G-7 (OUG 92/2021 art. 8 alin. (2) and alin. (4)).
     *
     * <p>Returned as a set and read once per request, never per row. On the single-movement paths
     * it is one small indexed query; on {@link #list} it is one for the whole page.
     */
    private Set<String> codesWithBulletin(UUID tenantId) {
        return Set.copyOf(bulletinRepository.findCoveredWasteCodes(tenantId));
    }

}
