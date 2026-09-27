package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.InventoryDeclarationRequest;
import ro.ecoregistru.controller.request.InventoryHeaderRequest;
import ro.ecoregistru.controller.request.InventoryLinesRequest;
import ro.ecoregistru.controller.request.InventoryPvRequest;
import ro.ecoregistru.controller.response.InventoryResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Inventory;
import ro.ecoregistru.entity.InventoryCommissionMember;
import ro.ecoregistru.entity.InventoryLine;
import ro.ecoregistru.entity.KeeperDeclaration;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CountMethod;
import ro.ecoregistru.enums.InventoryKind;
import ro.ecoregistru.enums.InventoryStatus;
import ro.ecoregistru.enums.StockOpeningStatus;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.InventoryRepository;
import ro.ecoregistru.repository.MonthlyEvidenceRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * D3.5 — inventarul unui depozit, după Normele OMFP 2861/2009. Scripticul se fotografiază la deschidere la începutul
 * zilei de început (pct. 1 alin. (1), 8 lit. d)): ce intră sau iese în perioada inventarului ține de „zona tampon”
 * (pct. 9), nu de ce s-a numărat. Operațiunile nu se blochează; în schimb, închiderea și aprobarea recalculează
 * scripticul și refuză dacă s-a schimbat (o operațiune finalizată retroactiv). Aprobarea scrie liniile
 * {@code INVENTORY_SURPLUS/SHORTAGE} datate la data de referință (OMFP 1802/2014 pct. 95 alin. (2)) și inventarul nu se
 * mai schimbă: o corectură e un inventar nou (OMFP 2634/2015 anexa 1 pct. 16).
 *
 * <p>Tot ce scrie aici îl face cine administrează firma; comisia și gestionarul sunt nume tastate, nu conturi.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryService {

    static final Set<WeighingOperationStatus> COUNTED = EnumSet.of(WeighingOperationStatus.FINALIZED,
            WeighingOperationStatus.IN_TRANSIT);
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);
    /** Pct. 43: procesul-verbal ajunge la administrator în cel mult 7 zile lucrătoare de la terminarea inventarului. */
    static final int PV_WORKING_DAYS = 7;

    InventoryRepository inventoryRepository;
    ro.ecoregistru.repository.StockOpeningRepository openingRepository;
    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;
    WasteArticleRepository articleRepository;
    WasteCodeRepository wasteCodeRepository;
    WasteMovementRepository movementRepository;
    WeighingOperationRepository operationRepository;
    MonthlyEvidenceRepository evidenceRepository;
    DepotAccess depotAccess;

    @Transactional(readOnly = true)
    public List<InventoryResponse> list(UUID workPointId) {
        UUID tenantId = TenantContext.require();
        return inventoryRepository.findAllByCompanyIdOrderByNumberDesc(tenantId).stream()
                .filter(i -> workPointId == null || i.getWorkPoint().getId().equals(workPointId))
                .filter(i -> depotAccess.allows(i.getWorkPoint().getId()))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InventoryResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public InventoryResponse open(InventoryHeaderRequest request) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId).orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        if (!company.getType().keepsArt48Register()) {
            throw new BusinessException(ART48_REGISTER_NOT_ENABLED);
        }
        Inventory inventory = Inventory.builder().companyId(tenantId).status(InventoryStatus.OPEN).build();
        applyHeader(inventory, request, tenantId);
        if (inventoryRepository.existsByWorkPoint_IdAndStatusIn(inventory.getWorkPoint().getId(),
                EnumSet.of(InventoryStatus.OPEN, InventoryStatus.CLOSED))) {
            throw new BusinessException(INVENTORY_ALREADY_ACTIVE);
        }
        UUID depotId = inventory.getWorkPoint().getId();
        LocalDate approved = inventoryRepository.latestApprovedStart(depotId);
        if (approved != null && inventory.getStartsOn().isBefore(approved)) {
            throw new BusinessException(INVENTORY_BEFORE_APPROVED);
        }
        openingRepository.findFirstByWorkPoint_IdAndStatus(depotId, StockOpeningStatus.CONFIRMED)
                .filter(o -> inventory.getStartsOn().isBefore(o.getCutOffDate()))
                .ifPresent(o -> {
                    throw new BusinessException(INVENTORY_BEFORE_OPENING);
                });
        operationRepository.lockNumbering(tenantId + ":inventar");
        inventory.setNumber(inventoryRepository.maxNumber(tenantId) + 1);
        int no = 1;
        for (var b : book(inventory).values()) {
            inventory.getLines().add(InventoryLine.builder().inventory(inventory).lineNo(no++)
                    .article(b.article()).wasteCode(b.code()).bookKg(b.kg()).build());
        }
        inventory.setLastEntryDoc(lastDocument(inventory, EnumSet.of(WeighingOperationType.IN)));
        inventory.setLastExitDoc(lastDocument(inventory, EnumSet.of(WeighingOperationType.OUT, WeighingOperationType.TRANSFER)));
        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse updateHeader(UUID id, InventoryHeaderRequest request) {
        Inventory inventory = requireOpen(id);
        if (!inventory.getWorkPoint().getId().equals(request.workPointId())
                || !inventory.getStartsOn().equals(request.startsOn())) {
            // Depozitul și data de referință fixează fotografia: se schimbă printr-un inventar nou.
            throw new BusinessException(INVENTORY_NOT_OPEN);
        }
        applyHeader(inventory, request, inventory.getCompanyId());
        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse saveDeclaration(UUID id, InventoryDeclarationRequest request) {
        Inventory inventory = requireOpen(id);
        if (request.answers() == null || request.answers().size() != KeeperDeclaration.QUESTIONS.size()) {
            throw new BusinessException(INVENTORY_DECLARATION_INCOMPLETE);
        }
        inventory.setDeclaration(new KeeperDeclaration(request.answers().stream()
                .map(a -> new KeeperDeclaration.Answer(a.yes(), a.yes() ? trim(a.detail()) : null)).toList()));
        inventory.setLastEntryDoc(trim(request.lastEntryDoc()));
        inventory.setLastExitDoc(trim(request.lastExitDoc()));
        inventory.setDeclarationDate(request.declarationDate() == null ? inventory.getStartsOn() : request.declarationDate());
        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse saveLines(UUID id, InventoryLinesRequest request) {
        Inventory inventory = requireOpen(id);
        List<InventoryLinesRequest.Line> rows = request.lines() == null ? List.of() : request.lines();
        if (inventory.getDeclaration() == null && rows.stream().anyMatch(r -> r.countedKg() != null)) {
            throw new BusinessException(INVENTORY_DECLARATION_REQUIRED);
        }
        Map<UUID, InventoryLine> byId = new HashMap<>();
        inventory.getLines().forEach(l -> byId.put(l.getId(), l));
        Set<UUID> kept = new HashSet<>();
        int next = inventory.getLines().stream().mapToInt(InventoryLine::getLineNo).max().orElse(0) + 1;
        for (InventoryLinesRequest.Line r : rows) {
            InventoryLine line;
            if (r.id() != null) {
                line = byId.get(r.id());
                if (line == null) {
                    throw new NotFoundException(INVENTORY_NOT_FOUND);
                }
            } else {
                WasteArticle article = r.articleId() == null ? null : articleRepository.findById(r.articleId())
                        .filter(a -> a.getCompany().getId().equals(inventory.getCompanyId()))
                        .orElseThrow(() -> new NotFoundException(WASTE_ARTICLE_NOT_FOUND));
                WasteCode code = article != null ? article.getWasteCode() : wasteCodeRepository.findById(r.wasteCodeId())
                        .orElseThrow(() -> new NotFoundException(WASTE_CODE_NOT_FOUND));
                line = InventoryLine.builder().inventory(inventory).lineNo(next++).article(article).wasteCode(code)
                        .bookKg(BigDecimal.ZERO).addedManually(true).build();
                inventory.getLines().add(line);
            }
            if (r.countedKg() != null && r.countedKg().signum() < 0) {
                throw new BusinessException(INVENTORY_COUNT_MISSING);
            }
            line.setCountedKg(r.countedKg());
            line.setCountMethod(r.countMethod());
            line.setTechnicalData(trim(r.technicalData()));
            line.setExplanation(trim(r.explanation()));
            line.setShortageNature(r.shortageNature());
            line.setResponsiblePerson(trim(r.responsiblePerson()));
            line.setSlowMoving(r.slowMoving());
            kept.add(line.getId());
        }
        // Un rând adăugat de mână și scos din tabel dispare; cele fotografiate rămân (scripticul lor e fapt).
        inventory.getLines().removeIf(l -> l.isAddedManually() && l.getId() != null && !kept.contains(l.getId()));
        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse savePv(UUID id, InventoryPvRequest request) {
        Inventory inventory = requireOpen(id);
        inventory.setPvDate(request.pvDate());
        inventory.setPvCauses(trim(request.causes()));
        inventory.setPvMeasures(trim(request.measures()));
        inventory.setPvSlowStock(trim(request.slowStock()));
        inventory.setPvStorageFindings(trim(request.storageFindings()));
        inventory.setPvOther(trim(request.other()));
        inventory.setKeeperObjections(trim(request.keeperObjections()));
        inventory.setCommissionConclusions(trim(request.commissionConclusions()));
        return toResponse(inventoryRepository.save(inventory));
    }

    /** Procesul-verbal se încheie: tot ce cer Normele e completat și scripticul e cel fotografiat. */
    @Transactional
    public InventoryResponse close(UUID id) {
        Inventory inventory = requireOpen(id);
        if (inventory.getDeclaration() == null) {
            throw new BusinessException(INVENTORY_DECLARATION_REQUIRED);
        }
        for (InventoryLine l : inventory.getLines()) {
            if (l.getCountedKg() == null) {
                throw new BusinessException(INVENTORY_COUNT_MISSING);
            }
            if (l.getCountMethod() == null) {
                throw new BusinessException(INVENTORY_METHOD_REQUIRED);
            }
            if (l.getCountMethod() == CountMethod.TECHNICAL && l.getTechnicalData() == null) {
                throw new BusinessException(INVENTORY_TECHNICAL_DATA_REQUIRED);
            }
            if (l.difference().signum() != 0 && l.getExplanation() == null) {
                throw new BusinessException(INVENTORY_EXPLANATION_REQUIRED);
            }
            if (l.difference().signum() < 0 && l.getShortageNature() == null) {
                throw new BusinessException(INVENTORY_NATURE_REQUIRED);
            }
        }
        requireBookUnchanged(inventory);
        inventory.setStatus(InventoryStatus.CLOSED);
        inventory.setClosedOn(DeadlineService.today());
        if (inventory.getPvDate() == null) {
            inventory.setPvDate(inventory.getClosedOn());
        }
        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse reopen(UUID id) {
        Inventory inventory = require(id);
        requireNotApproved(inventory);
        if (inventory.getStatus() != InventoryStatus.CLOSED) {
            throw new BusinessException(INVENTORY_NOT_CLOSED);
        }
        inventory.setStatus(InventoryStatus.OPEN);
        inventory.setClosedOn(null);
        return toResponse(inventoryRepository.save(inventory));
    }

    /** Reface fotografia scripticului: rândurile schimbate își pierd explicația, perechile noi apar. */
    @Transactional
    public InventoryResponse recalculate(UUID id) {
        Inventory inventory = requireOpen(id);
        Map<String, Book> book = book(inventory);
        for (InventoryLine l : inventory.getLines()) {
            Book b = book.remove(key(l.getArticle(), l.getWasteCode()));
            BigDecimal now = b == null ? BigDecimal.ZERO : b.kg();
            if (now.compareTo(l.getBookKg()) != 0) {
                l.setBookKg(now);
                l.setExplanation(null);
                l.setShortageNature(null);
            }
        }
        int no = inventory.getLines().stream().mapToInt(InventoryLine::getLineNo).max().orElse(0) + 1;
        for (Book b : book.values()) {
            inventory.getLines().add(InventoryLine.builder().inventory(inventory).lineNo(no++)
                    .article(b.article()).wasteCode(b.code()).bookKg(b.kg()).build());
        }
        return toResponse(inventoryRepository.save(inventory));
    }

    /** Administratorul aprobă PV-ul: se scriu ajustările, datate la data de referință. */
    @Transactional
    public InventoryResponse approve(UUID id) {
        Inventory inventory = require(id);
        requireNotApproved(inventory);
        if (inventory.getStatus() != InventoryStatus.CLOSED) {
            throw new BusinessException(INVENTORY_NOT_CLOSED);
        }
        evidenceRepository.lockForRebuild(inventory.getCompanyId()); // BUG-048
        requireBookUnchanged(inventory);
        Company company = companyRepository.getReferenceById(inventory.getCompanyId());
        UUID userId = SecurityUtils.currentUser().getId();
        for (InventoryLine l : inventory.getLines()) {
            BigDecimal diff = l.difference();
            if (diff.signum() == 0) {
                continue;
            }
            movementRepository.save(WasteMovement.builder()
                    .company(company)
                    .workPoint(inventory.getWorkPoint())
                    .date(inventory.getStartsOn())
                    .wasteCode(l.getWasteCode())
                    .article(l.getArticle())
                    .quantity(diff.abs())
                    .unit(Unit.KG)
                    .operation(diff.signum() > 0 ? WasteOperation.INVENTORY_SURPLUS : WasteOperation.INVENTORY_SHORTAGE)
                    .register(WasteRegister.ART_48)
                    .inventory(inventory)
                    .deleted(false)
                    .createdBy(userId)
                    .build());
        }
        inventory.setStatus(InventoryStatus.APPROVED);
        inventory.setApprovedOn(DeadlineService.today());
        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse cancel(UUID id, String reason) {
        Inventory inventory = require(id);
        requireNotApproved(inventory);
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(INVENTORY_CANCEL_REASON_REQUIRED);
        }
        inventory.setStatus(InventoryStatus.CANCELLED);
        inventory.setCancelReason(reason.trim());
        return toResponse(inventoryRepository.save(inventory));
    }

    /** Pct. 9 — operațiunile depozitului datate între începutul și sfârșitul inventarului, de arătat comisiei. */
    @Transactional(readOnly = true)
    public List<InventoryResponse.DuringOperation> operationsDuring(UUID id) {
        Inventory inventory = require(id);
        return during(inventory).stream().map(o -> new InventoryResponse.DuringOperation(o.getId(), o.getType().name(),
                o.getNumber(), o.getDate(), counterparty(o))).toList();
    }

    public List<WeighingOperation> during(Inventory inventory) {
        return operationRepository.findAllByCompany_IdAndWorkPoint_IdAndStatusInAndDateBetweenOrderByDateAscNumberAsc(
                inventory.getCompanyId(), inventory.getWorkPoint().getId(), COUNTED, inventory.getStartsOn(),
                inventory.getEndsOn());
    }

    /** Pentru documentele tipărite: inventarul, după aceleași reguli de acces. */
    @Transactional(readOnly = true)
    public Inventory requireForDocuments(UUID id) {
        Inventory inventory = require(id);
        inventory.getLines().forEach(l -> {
            l.getWasteCode().getCode();
            if (l.getArticle() != null) l.getArticle().getName();
        });
        inventory.getCommission().size();
        inventory.getWorkPoint().getName();
        return inventory;
    }

    // --- helpers ---

    private void applyHeader(Inventory inventory, InventoryHeaderRequest r, UUID tenantId) {
        WorkPoint depot = depotAccess.require(workPointRepository.findByIdAndCompany_Id(r.workPointId(), tenantId)
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND)));
        if (r.keeperName() == null || r.keeperName().isBlank()) {
            throw new BusinessException(INVENTORY_KEEPER_REQUIRED);
        }
        List<InventoryHeaderRequest.Member> members = r.commission() == null ? List.of() : r.commission().stream()
                .filter(m -> m.name() != null && !m.name().isBlank()).toList();
        if (members.stream().filter(InventoryHeaderRequest.Member::president).count() != 1) {
            throw new BusinessException(INVENTORY_PRESIDENT_REQUIRED);
        }
        InventoryKind kind = r.kind() == null ? InventoryKind.ANNUAL : r.kind();
        String receiving = trim(r.receivingKeeperName());
        if (receiving != null && kind != InventoryKind.HANDOVER) {
            throw new BusinessException(INVENTORY_RECEIVING_KEEPER_ONLY_ON_HANDOVER);
        }
        LocalDate startsOn = r.startsOn() == null ? DeadlineService.today() : r.startsOn();
        LocalDate endsOn = r.endsOn() == null ? startsOn : r.endsOn();
        if (endsOn.isBefore(startsOn)) {
            throw new BusinessException(INVENTORY_PERIOD_INVALID);
        }
        inventory.setWorkPoint(depot);
        inventory.setDecisionNumber(trim(r.decisionNumber()));
        inventory.setDecisionDate(r.decisionDate());
        inventory.setKind(kind);
        inventory.setCountsAsAnnual(kind != InventoryKind.ANNUAL && r.countsAsAnnual());
        inventory.setMode(trim(r.mode()));
        inventory.setMethod(trim(r.method()));
        inventory.setStartsOn(startsOn);
        inventory.setEndsOn(endsOn);
        inventory.getCommission().clear();
        members.forEach(m -> inventory.getCommission().add(InventoryCommissionMember.builder()
                .name(m.name().trim()).role(trim(m.role())).president(m.president()).build()));
        inventory.setKeeperName(r.keeperName().trim());
        inventory.setReceivingKeeperName(receiving);
        inventory.setKeeperRepresentative(trim(r.keeperRepresentative()));
    }

    private record Book(WasteArticle article, WasteCode code, BigDecimal kg) {
    }

    /**
     * Scripticul la începutul zilei de început (pct. 1, 8 lit. d), 9): tot ce contează până în ziua dinainte, plus
     * liniile de stoc datate chiar în ziua de început — soldul preluat și ajustările unui inventar aprobat din aceeași zi
     * sunt stocul dimineții, nu mișcări ale zilei (altfel s-ar număra de două ori).
     */
    private Map<String, Book> book(Inventory inventory) {
        Map<String, Book> book = new java.util.LinkedHashMap<>();
        for (var s : movementRepository.stockAt(inventory.getCompanyId(), inventory.getWorkPoint().getId(),
                inventory.getStartsOn().minusDays(1))) {
            WasteArticle article = s.getArticleId() == null ? null : articleRepository.getReferenceById(s.getArticleId());
            WasteCode code = wasteCodeRepository.getReferenceById(s.getWasteCodeId());
            book.put(key(s.getArticleId(), s.getWasteCodeId()), new Book(article, code, s.getKg()));
        }
        for (WasteMovement m : movementRepository.findStockOnlyBetween(inventory.getCompanyId(),
                inventory.getWorkPoint().getId(), inventory.getStartsOn(), inventory.getStartsOn())) {
            if (inventory.getId() != null && m.getInventory() != null && inventory.getId().equals(m.getInventory().getId())) {
                continue; // ajustările lui însuși, după aprobare
            }
            BigDecimal kg = m.getUnit() == Unit.TONS ? m.getQuantity().multiply(BigDecimal.valueOf(1000)) : m.getQuantity();
            BigDecimal signed = m.getOperation() == WasteOperation.INVENTORY_SHORTAGE ? kg.negate() : kg;
            book.merge(key(m.getArticle(), m.getWasteCode()), new Book(m.getArticle(), m.getWasteCode(), signed),
                    (a, b) -> new Book(a.article(), a.code(), a.kg().add(b.kg())));
        }
        book.values().removeIf(b -> b.kg().signum() == 0);
        return book;
    }

    private static String key(WasteArticle article, WasteCode code) {
        return key(article == null ? null : article.getId(), code.getId());
    }

    private static String key(UUID articleId, UUID codeId) {
        return articleId + "|" + codeId;
    }

    /** Rândurile al căror scriptic nu mai e cel fotografiat, plus perechile apărute între timp. */
    private List<UUID> changedLines(Inventory inventory) {
        Map<String, Book> book = book(inventory);
        List<UUID> changed = new ArrayList<>();
        for (InventoryLine l : inventory.getLines()) {
            Book b = book.remove(key(l.getArticle(), l.getWasteCode()));
            BigDecimal now = b == null ? BigDecimal.ZERO : b.kg();
            if (now.compareTo(l.getBookKg()) != 0) {
                changed.add(l.getId());
            }
        }
        if (!book.isEmpty()) {
            changed.add(null);
        }
        return changed;
    }

    private void requireBookUnchanged(Inventory inventory) {
        if (!changedLines(inventory).isEmpty()) {
            throw new BusinessException(INVENTORY_BOOK_CHANGED);
        }
    }

    private String lastDocument(Inventory inventory, Set<WeighingOperationType> types) {
        return operationRepository
                .findTop1ByCompany_IdAndWorkPoint_IdAndTypeInAndStatusInAndDateBeforeOrderByDateDescNumberDesc(
                        inventory.getCompanyId(), inventory.getWorkPoint().getId(), types, COUNTED,
                        inventory.getStartsOn())
                .stream().findFirst()
                .map(o -> typeLabel(o.getType()) + " nr. " + o.getNumber() + " din " + o.getDate().format(DATE))
                .orElse(null);
    }

    static String typeLabel(WeighingOperationType type) {
        return switch (type) {
            case IN -> "Intrare";
            case OUT -> "Ieșire";
            case TRANSFER -> "Transfer";
            default -> type.name();
        };
    }

    private static String counterparty(WeighingOperation o) {
        if (o.getPartner() != null) return o.getPartner().getName();
        if (o.getNaturalPerson() != null) return o.getNaturalPerson().getName();
        if (o.getTargetWorkPoint() != null) return o.getTargetWorkPoint().getName();
        return null;
    }

    private Inventory require(UUID id) {
        return inventoryRepository.findByIdAndCompanyId(id, TenantContext.require())
                .filter(i -> depotAccess.allows(i.getWorkPoint().getId()))
                .orElseThrow(() -> new NotFoundException(INVENTORY_NOT_FOUND));
    }

    private Inventory requireOpen(UUID id) {
        Inventory inventory = require(id);
        requireNotApproved(inventory);
        if (inventory.getStatus() != InventoryStatus.OPEN) {
            throw new BusinessException(INVENTORY_NOT_OPEN);
        }
        return inventory;
    }

    private static void requireNotApproved(Inventory inventory) {
        if (inventory.getStatus() == InventoryStatus.APPROVED) {
            throw new BusinessException(INVENTORY_APPROVED_FINAL);
        }
        if (inventory.getStatus() == InventoryStatus.CANCELLED) {
            throw new BusinessException(INVENTORY_NOT_OPEN);
        }
    }

    private InventoryResponse toResponse(Inventory i) {
        List<String> warnings = new ArrayList<>();
        String keeper = normalize(i.getKeeperName());
        if (i.getCommission().stream().anyMatch(m -> Objects.equals(normalize(m.getName()), keeper))) {
            warnings.add("KEEPER_IN_COMMISSION");
        }
        if (i.getDeclaration() != null && i.getDeclaration().answers().size() > KeeperDeclaration.THIRD_PARTY_GOODS
                && i.getDeclaration().answers().get(KeeperDeclaration.THIRD_PARTY_GOODS).yes()) {
            warnings.add("THIRD_PARTY_GOODS");
        }
        List<UUID> changed = List.of();
        boolean active = i.getStatus() == InventoryStatus.OPEN || i.getStatus() == InventoryStatus.CLOSED;
        if (active) {
            if (DeadlineService.today().isAfter(addWorkingDays(i.getEndsOn(), PV_WORKING_DAYS))) {
                warnings.add("PV_DEADLINE");
            }
            changed = changedLines(i);
            if (!changed.isEmpty()) {
                warnings.add("BOOK_CHANGED");
            }
        }
        BigDecimal surplus = BigDecimal.ZERO;
        BigDecimal shortage = BigDecimal.ZERO;
        for (InventoryLine l : i.getLines()) {
            BigDecimal d = l.difference();
            if (d == null) continue;
            if (d.signum() > 0) surplus = surplus.add(d);
            else shortage = shortage.add(d.negate());
        }
        return new InventoryResponse(i.getId(), i.getWorkPoint().getId(), i.getWorkPoint().getName(), i.getNumber(),
                i.getDecisionNumber(), i.getDecisionDate(), i.getKind(), i.isCountsAsAnnual(), i.getMode(), i.getMethod(),
                i.getStartsOn(), i.getEndsOn(),
                i.getCommission().stream().map(m -> new InventoryResponse.Member(m.getName(), m.getRole(), m.isPresident())).toList(),
                i.getKeeperName(), i.getReceivingKeeperName(), i.getKeeperRepresentative(),
                i.getDeclaration() == null ? null : i.getDeclaration().answers(), i.getDeclarationDate(),
                i.getLastEntryDoc(), i.getLastExitDoc(), i.getPvDate(), i.getPvCauses(), i.getPvMeasures(),
                i.getPvSlowStock(), i.getPvStorageFindings(), i.getPvOther(), i.getKeeperObjections(),
                i.getCommissionConclusions(), i.getStatus(), i.getClosedOn(), i.getApprovedOn(), i.getCancelReason(),
                i.getLines().stream().map(l -> new InventoryResponse.Line(l.getId(),
                        l.getArticle() == null ? null : l.getArticle().getId(),
                        l.getArticle() == null ? null : l.getArticle().getName(),
                        l.getWasteCode().getId(), l.getWasteCode().getCode(), l.getWasteCode().getName(),
                        l.getWasteCode().isHazardous(), l.getBookKg(), l.getCountedKg(), l.difference(),
                        l.getCountMethod(), l.getTechnicalData(), l.getExplanation(), l.getShortageNature(),
                        l.getResponsiblePerson(), l.isSlowMoving(), l.isAddedManually())).toList(),
                surplus, shortage, warnings, changed.stream().filter(Objects::nonNull).toList());
    }

    /** Luni–vineri; sărbătorile legale nu sunt socotite (avertismentul poate veni cu o zi-două mai devreme). */
    static LocalDate addWorkingDays(LocalDate from, int days) {
        LocalDate d = from;
        int added = 0;
        while (added < days) {
            d = d.plusDays(1);
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return d;
    }

    private static String normalize(String s) {
        return s == null ? null : s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
