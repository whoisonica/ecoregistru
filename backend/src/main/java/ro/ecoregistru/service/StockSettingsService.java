package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.AuthorizedLimitRequest;
import ro.ecoregistru.controller.request.StockThresholdRequest;
import ro.ecoregistru.entity.AuthorizedLimit;
import ro.ecoregistru.entity.StockThreshold;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AuthorizedLimitRepository;
import ro.ecoregistru.repository.StockThresholdRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * F3 — pragurile de stoc (D3.3) și limitele din autorizație (D3.4) ale unui depozit. Le scrie cine administrează firma:
 * limitele vin din autorizația ei, pragurile sunt politica ei. Le citește oricine vede depozitul.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockSettingsService {

    static final Set<String> KINDS = Set.of("STORED", "TREATED", "OUTPUT");
    static final Set<String> UNITS = Set.of("T", "KG", "M3");
    static final Set<String> PERIODS = Set.of("AT_ONCE", "MONTH", "YEAR");

    StockThresholdRepository thresholdRepository;
    AuthorizedLimitRepository limitRepository;
    WorkPointRepository workPointRepository;
    WasteArticleRepository articleRepository;
    WasteCodeRepository wasteCodeRepository;
    DepotAccess depotAccess;

    public record ThresholdView(UUID id, UUID workPointId, UUID articleId, String articleName, BigDecimal minKg,
                                BigDecimal maxKg) {
    }

    public record LimitView(UUID id, UUID workPointId, String kind, UUID wasteCodeId, String wasteCode,
                            BigDecimal quantity, String unit, String period, Integer maxStorageDays,
                            boolean approximate, String note) {
    }

    @Transactional(readOnly = true)
    public List<ThresholdView> thresholds(UUID workPointId) {
        UUID tenantId = TenantContext.require();
        requireDepot(workPointId, tenantId);
        return thresholdRepository.findAllByCompanyIdAndWorkPoint_Id(tenantId, workPointId).stream()
                .map(StockSettingsService::view).toList();
    }

    /** Un prag pe sortiment × depozit: salvarea îl înlocuiește pe cel vechi. */
    @Transactional
    public ThresholdView saveThreshold(StockThresholdRequest request) {
        UUID tenantId = TenantContext.require();
        WorkPoint depot = requireDepot(request.workPointId(), tenantId);
        var article = articleRepository.findByIdAndCompany_Id(request.articleId(), tenantId)
                .orElseThrow(() -> new NotFoundException(WASTE_ARTICLE_NOT_FOUND));
        if (request.minKg() == null && request.maxKg() == null) {
            throw new BusinessException(STOCK_THRESHOLD_EMPTY);
        }
        if ((request.minKg() != null && request.minKg().signum() < 0)
                || (request.maxKg() != null && request.maxKg().signum() <= 0)
                || (request.minKg() != null && request.maxKg() != null && request.minKg().compareTo(request.maxKg()) > 0)) {
            throw new BusinessException(STOCK_THRESHOLD_INVALID);
        }
        StockThreshold threshold = thresholdRepository.findByWorkPoint_IdAndArticle_Id(depot.getId(), article.getId())
                .orElseGet(() -> StockThreshold.builder().companyId(tenantId).workPoint(depot).article(article)
                        .createdAt(Instant.now()).build());
        threshold.setMinKg(request.minKg());
        threshold.setMaxKg(request.maxKg());
        return view(thresholdRepository.save(threshold));
    }

    @Transactional
    public void deleteThreshold(UUID id) {
        UUID tenantId = TenantContext.require();
        StockThreshold threshold = thresholdRepository.findByIdAndCompanyId(id, tenantId)
                .filter(t -> depotAccess.allows(t.getWorkPoint().getId()))
                .orElseThrow(() -> new NotFoundException(STOCK_SETTING_NOT_FOUND));
        thresholdRepository.delete(threshold);
    }

    @Transactional(readOnly = true)
    public List<LimitView> limits(UUID workPointId) {
        UUID tenantId = TenantContext.require();
        requireDepot(workPointId, tenantId);
        return limitRepository.findAllByCompanyIdAndWorkPoint_IdOrderByCreatedAtAsc(tenantId, workPointId).stream()
                .map(StockSettingsService::view).toList();
    }

    @Transactional
    public LimitView addLimit(AuthorizedLimitRequest request) {
        UUID tenantId = TenantContext.require();
        WorkPoint depot = requireDepot(request.workPointId(), tenantId);
        AuthorizedLimit limit = AuthorizedLimit.builder().companyId(tenantId).workPoint(depot)
                .createdAt(Instant.now()).build();
        apply(limit, request);
        return view(limitRepository.save(limit));
    }

    @Transactional
    public LimitView updateLimit(UUID id, AuthorizedLimitRequest request) {
        AuthorizedLimit limit = requireLimit(id);
        apply(limit, request);
        return view(limitRepository.save(limit));
    }

    @Transactional
    public void deleteLimit(UUID id) {
        limitRepository.delete(requireLimit(id));
    }

    private void apply(AuthorizedLimit limit, AuthorizedLimitRequest request) {
        if (!KINDS.contains(request.kind()) || !UNITS.contains(request.unit()) || !PERIODS.contains(request.period())) {
            throw new BusinessException(AUTHORIZED_LIMIT_INVALID);
        }
        if (request.quantity() == null || request.quantity().signum() <= 0) {
            throw new BusinessException(AUTHORIZED_LIMIT_QUANTITY);
        }
        if (request.maxStorageDays() != null && request.maxStorageDays() <= 0) {
            throw new BusinessException(AUTHORIZED_LIMIT_INVALID);
        }
        limit.setKind(request.kind());
        limit.setWasteCode(request.wasteCodeId() == null ? null : wasteCodeRepository.findById(request.wasteCodeId())
                .orElseThrow(() -> new NotFoundException(WASTE_CODE_NOT_FOUND)));
        limit.setQuantity(request.quantity());
        limit.setUnit(request.unit());
        limit.setPeriod(request.period());
        limit.setMaxStorageDays(request.maxStorageDays());
        limit.setApproximate(request.approximate());
        limit.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
    }

    private AuthorizedLimit requireLimit(UUID id) {
        return limitRepository.findByIdAndCompanyId(id, TenantContext.require())
                .filter(l -> depotAccess.allows(l.getWorkPoint().getId()))
                .orElseThrow(() -> new NotFoundException(STOCK_SETTING_NOT_FOUND));
    }

    private WorkPoint requireDepot(UUID workPointId, UUID tenantId) {
        return depotAccess.require(workPointRepository.findByIdAndCompany_Id(workPointId, tenantId)
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND)));
    }

    private static ThresholdView view(StockThreshold t) {
        return new ThresholdView(t.getId(), t.getWorkPoint().getId(), t.getArticle().getId(), t.getArticle().getName(),
                t.getMinKg(), t.getMaxKg());
    }

    private static LimitView view(AuthorizedLimit l) {
        return new LimitView(l.getId(), l.getWorkPoint().getId(), l.getKind(),
                l.getWasteCode() == null ? null : l.getWasteCode().getId(),
                l.getWasteCode() == null ? null : l.getWasteCode().getCode(), l.getQuantity(), l.getUnit(),
                l.getPeriod(), l.getMaxStorageDays(), l.isApproximate(), l.getNote());
    }
}
