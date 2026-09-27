package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.StockOpeningRequest;
import ro.ecoregistru.controller.response.StockOpeningResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.StockOpening;
import ro.ecoregistru.entity.StockOpeningLine;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.StockOpeningStatus;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.MonthlyEvidenceRepository;
import ro.ecoregistru.repository.StockOpeningRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * D3.5 — nota de preluare a soldurilor: stocul care exista pe un depozit înainte ca firma să-și țină evidența în
 * aplicație. Nu e inventar (Legea 82/1991 art. 7 alin. (1); Normele OMFP 2861/2009 pct. 2–3, 35 alin. (1)): cantitățile
 * vin din fișele de magazie sau din analiticul contabil la data de tăiere, iar gestionarul și contabilul confirmă
 * reconcilierea (OMFP 2634/2015 anexa 1 pct. 61). Confirmarea scrie liniile {@link WasteOperation#OPENING_BALANCE};
 * după ea nota nu se mai schimbă — o corectură se face printr-un inventar.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockOpeningService {

    StockOpeningRepository openingRepository;
    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;
    WasteArticleRepository articleRepository;
    WasteCodeRepository wasteCodeRepository;
    WasteMovementRepository movementRepository;
    WeighingOperationRepository operationRepository;
    MonthlyEvidenceRepository evidenceRepository;
    DepotAccess depotAccess;

    @Transactional(readOnly = true)
    public List<StockOpeningResponse> list(UUID workPointId) {
        UUID tenantId = TenantContext.require();
        return openingRepository.findAllByCompanyIdOrderByCreatedAtDesc(tenantId).stream()
                .filter(o -> workPointId == null || o.getWorkPoint().getId().equals(workPointId))
                .filter(o -> depotAccess.allows(o.getWorkPoint().getId()))
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StockOpeningResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional
    public StockOpeningResponse create(StockOpeningRequest request) {
        UUID tenantId = TenantContext.require();
        requireArt48(tenantId);
        StockOpening opening = StockOpening.builder()
                .companyId(tenantId)
                .status(StockOpeningStatus.DRAFT)
                .build();
        apply(opening, request, tenantId);
        return toResponse(openingRepository.save(opening));
    }

    @Transactional
    public StockOpeningResponse update(UUID id, StockOpeningRequest request) {
        StockOpening opening = requireDraft(id);
        apply(opening, request, opening.getCompanyId());
        return toResponse(openingRepository.save(opening));
    }

    @Transactional
    public void delete(UUID id) {
        openingRepository.delete(requireDraft(id));
    }

    @Transactional
    public StockOpeningResponse confirm(UUID id) {
        StockOpening opening = requireDraft(id);
        UUID tenantId = opening.getCompanyId();
        evidenceRepository.lockForRebuild(tenantId); // BUG-048: nu scrie în mijlocul unei refaceri
        if (blank(opening.getKeeperName()) || blank(opening.getAccountantName())) {
            throw new BusinessException(STOCK_OPENING_SIGNATURES_REQUIRED);
        }
        if (openingRepository.existsByWorkPoint_IdAndStatus(opening.getWorkPoint().getId(), StockOpeningStatus.CONFIRMED)) {
            throw new BusinessException(STOCK_OPENING_ALREADY_CONFIRMED);
        }
        LocalDate first = movementRepository.firstCountedDate(tenantId, opening.getWorkPoint().getId());
        if (first != null && opening.getCutOffDate().isAfter(first)) {
            throw new BusinessException(STOCK_OPENING_AFTER_FIRST_MOVEMENT);
        }
        operationRepository.lockNumbering(tenantId + ":preluare");
        opening.setNumber(openingRepository.maxNumber(tenantId) + 1);
        opening.setStatus(StockOpeningStatus.CONFIRMED);
        opening.setConfirmedOn(DeadlineService.today());
        Company company = companyRepository.getReferenceById(tenantId);
        UUID userId = SecurityUtils.currentUser().getId();
        for (StockOpeningLine line : opening.getLines()) {
            movementRepository.save(WasteMovement.builder()
                    .company(company)
                    .workPoint(opening.getWorkPoint())
                    .date(opening.getCutOffDate())
                    .wasteCode(line.getWasteCode())
                    .article(line.getArticle())
                    .quantity(line.getKg())
                    .unit(Unit.KG)
                    .operation(WasteOperation.OPENING_BALANCE)
                    .register(WasteRegister.ART_48)
                    .stockOpening(opening)
                    .deleted(false)
                    .createdBy(userId)
                    .build());
        }
        return toResponse(openingRepository.save(opening));
    }

    /** Pentru nota tipărită: nota, după aceleași reguli de acces, cu rândurile încărcate. */
    @Transactional(readOnly = true)
    public StockOpening requireForDocuments(UUID id) {
        StockOpening opening = require(id);
        opening.getLines().forEach(l -> {
            l.getWasteCode().getCode();
            if (l.getArticle() != null) l.getArticle().getName();
        });
        opening.getWorkPoint().getName();
        return opening;
    }

    // --- helpers ---

    private void apply(StockOpening opening, StockOpeningRequest request, UUID tenantId) {
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new BusinessException(STOCK_OPENING_LINES_REQUIRED);
        }
        WorkPoint depot = depotAccess.require(workPointRepository.findByIdAndCompany_Id(request.workPointId(), tenantId)
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND)));
        opening.setWorkPoint(depot);
        opening.setCutOffDate(request.cutOffDate() == null ? DeadlineService.today() : request.cutOffDate());
        opening.setSource(request.source() == null ? ro.ecoregistru.enums.StockOpeningSource.STOCK_CARDS : request.source());
        opening.setKeeperName(trim(request.keeperName()));
        opening.setAccountantName(trim(request.accountantName()));
        opening.setNotes(trim(request.notes()));
        opening.getLines().clear();
        int no = 1;
        for (StockOpeningRequest.Line l : request.lines()) {
            if (l.kg() == null || l.kg().signum() <= 0) {
                throw new BusinessException(STOCK_OPENING_KG_POSITIVE);
            }
            WasteArticle article = l.articleId() == null ? null : articleRepository.findById(l.articleId())
                    .filter(a -> a.getCompany().getId().equals(tenantId))
                    .orElseThrow(() -> new NotFoundException(WASTE_ARTICLE_NOT_FOUND));
            WasteCode code = article != null ? article.getWasteCode() : wasteCodeRepository.findById(l.wasteCodeId())
                    .orElseThrow(() -> new NotFoundException(WASTE_CODE_NOT_FOUND));
            opening.getLines().add(StockOpeningLine.builder()
                    .opening(opening).lineNo(no++).article(article).wasteCode(code).kg(l.kg()).build());
        }
    }

    private StockOpening require(UUID id) {
        return openingRepository.findByIdAndCompanyId(id, TenantContext.require())
                .filter(o -> depotAccess.allows(o.getWorkPoint().getId()))
                .orElseThrow(() -> new NotFoundException(STOCK_OPENING_NOT_FOUND));
    }

    private StockOpening requireDraft(UUID id) {
        StockOpening opening = require(id);
        if (opening.getStatus() != StockOpeningStatus.DRAFT) {
            throw new BusinessException(STOCK_OPENING_CONFIRMED_IMMUTABLE);
        }
        return opening;
    }

    private void requireArt48(UUID tenantId) {
        Company company = companyRepository.findById(tenantId).orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        if (!company.getType().keepsArt48Register()) {
            throw new BusinessException(ART48_REGISTER_NOT_ENABLED);
        }
    }

    private StockOpeningResponse toResponse(StockOpening o) {
        return new StockOpeningResponse(o.getId(), o.getWorkPoint().getId(), o.getWorkPoint().getName(), o.getNumber(),
                o.getCutOffDate(), o.getSource(), o.getKeeperName(), o.getAccountantName(), o.getNotes(), o.getStatus(),
                o.getConfirmedOn(), movementRepository.firstCountedDate(o.getCompanyId(), o.getWorkPoint().getId()),
                o.getLines().stream().map(l -> new StockOpeningResponse.Line(l.getId(),
                        l.getArticle() == null ? null : l.getArticle().getId(),
                        l.getArticle() == null ? null : l.getArticle().getName(),
                        l.getWasteCode().getId(), l.getWasteCode().getCode(), l.getWasteCode().getName(),
                        l.getWasteCode().isHazardous(), l.getKg())).toList());
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String trim(String s) {
        return blank(s) ? null : s.trim();
    }
}
