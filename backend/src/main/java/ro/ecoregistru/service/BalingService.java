package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.BalingRequest;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.TreatmentMethod;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.MonthlyEvidenceRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * F5 — balotarea (proprietarul, 27.09.2026): „intră vrac, îl balotează”. Operatorul scrie doar câți baloți a făcut;
 * greutatea standard a sortimentului balotat dă kilogramele, care ies din sortimentul vrac și intră în cel balotat, cu
 * același cod de deșeu (regula ANPM 2022), sub R12 și „tratare mecanică” (surse-oficiale §18.6).
 *
 * <p><b>Se salvează finalizată.</b> N-are preț, plată sau document de transport, deci nimic de aprobat; o greșeală se
 * anulează cu motiv de cine aprobă ({@link WeighingOperationService#cancel}), iar stocul revine. Nu se editează.
 *
 * <p><b>Pierderea reală nu se vede aici:</b> baloții nu se cântăresc unul câte unul. Diferența dintre greutatea standard
 * și cea reală iese la cântărirea de la vânzare și la inventar.
 *
 * <p>Avertismente, nu refuzuri: stocul vrac care iese negativ (ca la ieșiri, D3.2) și R12 lipsă din codurile firmei.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BalingService {

    WeighingOperationRepository operationRepository;
    WasteMovementRepository movementRepository;
    WasteArticleRepository articleRepository;
    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;
    MonthlyEvidenceRepository evidenceRepository;
    DepotAccess depotAccess;
    StockPeriodLock stockLock;
    WeighingOperationService operations;

    @Transactional
    public WeighingOperationResponse create(BalingRequest request) {
        UUID tenantId = TenantContext.require();
        evidenceRepository.lockForRebuild(tenantId); // BUG-048: nu scrie în mijlocul unei refaceri
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        if (!company.getType().keepsArt48Register()) {
            throw new BusinessException(ART48_REGISTER_NOT_ENABLED);
        }
        if (request.date() == null) {
            throw new BusinessException(WEIGHING_OPERATION_DATE_REQUIRED);
        }
        if (request.baleCount() == null || request.baleCount() <= 0) {
            throw new BusinessException(BALING_COUNT_POSITIVE);
        }
        WorkPoint workPoint = depotAccess.require(workPointRepository.findByIdAndCompany_Id(request.workPointId(), tenantId)
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND)));
        stockLock.require(workPoint.getId(), request.date());
        WasteArticle baled = request.articleId() == null ? null
                : articleRepository.findByIdAndCompany_Id(request.articleId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(WASTE_ARTICLE_NOT_FOUND));
        if (baled == null || !baled.isActive() || !baled.isBaled()) {
            throw new BusinessException(BALING_ARTICLE_NOT_BALED);
        }
        BigDecimal kg = baled.getBaleWeightKg().multiply(BigDecimal.valueOf(request.baleCount()));
        UUID userId = SecurityUtils.currentUser().getId();

        operationRepository.lockNumbering(tenantId + ":" + WeighingOperationType.PROCESSING);
        Integer max = operationRepository.findMaxNumber(tenantId, WeighingOperationType.PROCESSING);
        WeighingOperation operation = operationRepository.save(WeighingOperation.builder()
                .company(company)
                .workPoint(workPoint)
                .type(WeighingOperationType.PROCESSING)
                .number(max == null ? 1 : max + 1)
                .date(request.date())
                .baleCount(request.baleCount())
                .baleWeightKg(baled.getBaleWeightKg())
                .notes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim())
                .status(WeighingOperationStatus.FINALIZED)
                .finalizedAt(Instant.now())
                .finalizedBy(userId)
                .createdBy(userId)
                .build());
        List<WasteMovement> lines = movementRepository.saveAll(List.of(
                leg(operation, baled.getSourceArticle(), kg, WasteOperation.PROCESSING_INPUT, 1, userId),
                leg(operation, baled, kg, WasteOperation.PROCESSING_OUTPUT, 2, userId)));
        return operations.withStockWarnings(operation, lines);
    }

    private static WasteMovement leg(WeighingOperation operation, WasteArticle article, BigDecimal kg,
                                     WasteOperation kind, int lineNo, UUID userId) {
        return WasteMovement.builder()
                .company(operation.getCompany())
                .workPoint(operation.getWorkPoint())
                .date(operation.getDate())
                .wasteCode(article.getWasteCode())
                .article(article)
                .quantity(kg)
                .unit(Unit.KG)
                .operation(kind)
                .register(WasteRegister.ART_48)
                .operationCode(WasteOperationCode.R12)
                .treatmentMethod(TreatmentMethod.TM)
                .weighingOperation(operation)
                .lineNo(lineNo)
                .deleted(false)
                .createdBy(userId)
                .build();
    }
}
