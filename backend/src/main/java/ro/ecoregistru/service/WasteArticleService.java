package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.WasteArticleRequest;
import ro.ecoregistru.controller.response.WasteArticleResponse;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.util.MetalWasteCodes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.WASTE_ARTICLE_CODE_REQUIRED;
import static ro.ecoregistru.exception.ErrorMessageEnum.WASTE_ARTICLE_NAME_REQUIRED;
import static ro.ecoregistru.exception.ErrorMessageEnum.WASTE_ARTICLE_NAME_TAKEN;
import static ro.ecoregistru.exception.ErrorMessageEnum.WASTE_ARTICLE_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.WASTE_CODE_NOT_FOUND;

/**
 * Catalogul de sortimente al firmei (D1.6): „Cupru”, „Carton balotat”, fiecare legat de un cod LER.
 *
 * <p>Sortimentul nu se șterge, doar se dezactivează: liniile de operațiune îl numesc, iar borderoul
 * se păstrează zece ani. Schimbarea codului nu atinge liniile vechi, fiindcă linia își ține codul ei
 * ({@code WasteMovement.wasteCode}), luat la cântărire.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WasteArticleService {

    WasteArticleRepository articleRepository;
    WasteCodeRepository wasteCodeRepository;
    CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<WasteArticleResponse> list() {
        return articleRepository.findAllByCompany_IdOrderByNameAsc(TenantContext.require()).stream()
                .map(WasteArticleService::toResponse).toList();
    }

    @Transactional
    public WasteArticleResponse create(WasteArticleRequest request) {
        UUID tenantId = TenantContext.require();
        String name = requireName(request);
        if (articleRepository.existsByCompany_IdAndNameIgnoreCase(tenantId, name)) {
            throw new BusinessException(WASTE_ARTICLE_NAME_TAKEN);
        }
        WasteCode code = requireCode(request);
        WasteArticle article = WasteArticle.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .wasteCode(code)
                .name(name)
                .metal(metal(request, code))
                .forbiddenFromIndividuals(request.forbiddenFromIndividuals())
                .active(true)
                .createdAt(Instant.now())
                .build();
        articleRepository.save(article);
        return toResponse(article);
    }

    @Transactional
    public WasteArticleResponse update(UUID id, WasteArticleRequest request) {
        WasteArticle article = requireOwn(id);
        String name = requireName(request);
        if (articleRepository.existsByCompany_IdAndNameIgnoreCaseAndIdNot(article.getCompany().getId(), name, id)) {
            throw new BusinessException(WASTE_ARTICLE_NAME_TAKEN);
        }
        WasteCode code = requireCode(request);
        article.setName(name);
        article.setWasteCode(code);
        article.setMetal(metal(request, code));
        article.setForbiddenFromIndividuals(request.forbiddenFromIndividuals());
        return toResponse(article);
    }

    @Transactional
    public void deactivate(UUID id) {
        requireOwn(id).setActive(false);
    }

    @Transactional
    public void reactivate(UUID id) {
        requireOwn(id).setActive(true);
    }

    private WasteArticle requireOwn(UUID id) {
        return articleRepository.findByIdAndCompany_Id(id, TenantContext.require())
                .orElseThrow(() -> new NotFoundException(WASTE_ARTICLE_NOT_FOUND));
    }

    private WasteCode requireCode(WasteArticleRequest request) {
        if (request.wasteCodeId() == null) {
            throw new BusinessException(WASTE_ARTICLE_CODE_REQUIRED);
        }
        return wasteCodeRepository.findById(request.wasteCodeId())
                .orElseThrow(() -> new NotFoundException(WASTE_CODE_NOT_FOUND));
    }

    private static boolean metal(WasteArticleRequest request, WasteCode code) {
        return request.metal() == null ? MetalWasteCodes.suggests(code.getCode()) : request.metal();
    }

    private static String requireName(WasteArticleRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException(WASTE_ARTICLE_NAME_REQUIRED);
        }
        return request.name().trim();
    }

    private static WasteArticleResponse toResponse(WasteArticle a) {
        WasteCode code = a.getWasteCode();
        return new WasteArticleResponse(a.getId(), a.getName(), code.getId(), code.getCode(), code.getName(),
                code.isHazardous(), a.isMetal(), a.isForbiddenFromIndividuals(), a.isActive());
    }
}
