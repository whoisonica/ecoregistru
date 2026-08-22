package ro.ecoregistru.service;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ro.ecoregistru.controller.request.WasteMovementRequest;
import ro.ecoregistru.controller.response.AttachmentResponse;
import ro.ecoregistru.controller.response.WasteMovementResponse;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;
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
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * Tenant-scoped CRUD for waste movements. Every query is filtered by the current
 * tenant (from TenantContext) so cross-tenant access is impossible.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WasteMovementService {

    WasteMovementRepository movementRepository;
    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;
    WasteCodeRepository wasteCodeRepository;
    PartnerRepository partnerRepository;
    AttachmentRepository attachmentRepository;
    CloudinaryStorageService storageService;
    WasteMovementMapper mapper;

    @Transactional
    public WasteMovementResponse create(WasteMovementRequest request) {
        UUID tenantId = TenantContext.require();

        // Idempotency: if the client already created this movement, return it unchanged.
        if (request.clientGeneratedId() != null) {
            var existing = movementRepository
                    .findByCompany_IdAndClientGeneratedId(tenantId, request.clientGeneratedId());
            if (existing.isPresent()) {
                return mapper.toResponse(existing.get());
            }
        }

        Company company = requireCompany(tenantId);
        WorkPoint workPoint = requireWorkPoint(request.workPointId(), tenantId);
        WasteCode wasteCode = requireWasteCode(request.wasteCodeId());
        Partner partner = resolvePartner(request, tenantId);
        validateOperationCode(request);
        WasteRegister register = resolveRegister(request, company);

        WasteMovement movement = WasteMovement.builder()
                .company(company)
                .workPoint(workPoint)
                .date(request.date())
                .wasteCode(wasteCode)
                .quantity(request.quantity())
                .unit(request.unit())
                .operation(request.operation())
                .register(register)
                .physicalState(request.physicalState())
                .operationCode(request.operationCode())
                .partner(partner)
                .documentReference(request.documentReference())
                .notes(request.notes())
                .clientGeneratedId(request.clientGeneratedId())
                .deleted(false)
                .createdBy(SecurityUtils.currentUser().getId())
                .build();

        movementRepository.save(movement);
        return mapper.toResponse(movement);
    }

    @Transactional
    public WasteMovementResponse update(UUID id, WasteMovementRequest request) {
        UUID tenantId = TenantContext.require();
        WasteMovement movement = requireMovement(id, tenantId);

        Company company = requireCompany(tenantId);
        WorkPoint workPoint = requireWorkPoint(request.workPointId(), tenantId);
        WasteCode wasteCode = requireWasteCode(request.wasteCodeId());
        Partner partner = resolvePartner(request, tenantId);
        validateOperationCode(request);
        WasteRegister register = resolveRegister(request, company);

        movement.setWorkPoint(workPoint);
        movement.setDate(request.date());
        movement.setWasteCode(wasteCode);
        movement.setQuantity(request.quantity());
        movement.setUnit(request.unit());
        movement.setOperation(request.operation());
        movement.setRegister(register);
        movement.setPhysicalState(request.physicalState());
        movement.setOperationCode(request.operationCode());
        movement.setPartner(partner);
        movement.setDocumentReference(request.documentReference());
        movement.setNotes(request.notes());

        return mapper.toResponse(movement);
    }

    @Transactional(readOnly = true)
    public List<WasteMovementResponse> list(Integer year, Integer month, UUID workPointId, UUID wasteCodeId) {
        UUID tenantId = TenantContext.require();
        LocalDate fromDate = null;
        LocalDate toDate = null;
        if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            fromDate = ym.atDay(1);
            toDate = ym.atEndOfMonth();
        }
        return movementRepository.findAll(buildFilter(tenantId, workPointId, wasteCodeId, fromDate, toDate))
                .stream().map(mapper::toResponse).toList();
    }

    /**
     * Dynamic, tenant-scoped filter. Building predicates only for present filters avoids
     * binding typed nulls (which Postgres rejects with "could not determine data type").
     */
    private Specification<WasteMovement> buildFilter(UUID tenantId, UUID workPointId,
                                                     UUID wasteCodeId, LocalDate fromDate, LocalDate toDate) {
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
            query.orderBy(cb.desc(root.get("date")), cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public WasteMovementResponse get(UUID id) {
        UUID tenantId = TenantContext.require();
        return mapper.toResponse(requireMovement(id, tenantId));
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

        var stored = storageService.upload(file, "movements/" + movementId);
        Attachment attachment = Attachment.builder()
                .movement(movement)
                .url(stored.url())
                .publicId(stored.publicId())
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
        storageService.delete(attachment.getPublicId());
        attachmentRepository.delete(attachment);
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

    private Partner resolvePartner(WasteMovementRequest request, UUID tenantId) {
        if (request.operation() == WasteOperation.HANDED_OVER && request.partnerId() == null) {
            throw new BusinessException(PARTNER_REQUIRED_FOR_HANDOVER);
        }
        if (request.partnerId() == null) {
            return null;
        }
        return partnerRepository.findByIdAndCompany_Id(request.partnerId(), tenantId)
                .orElseThrow(() -> new NotFoundException(PARTNER_NOT_FOUND));
    }

    /**
     * Enforces the R/D operation code rule. Every movement that takes waste off the site carries
     * one, because Anexa 1 cap. 3 and cap. 4 report the quantity together with "Operaţia de
     * valorificare"/"de eliminare" and the operator performing it — a quantity cannot be placed on
     * those chapters without its code (docs/surse-oficiale.md §1.2).
     *
     * <p>The family is pinned where the operation already names it: an R code for RECOVERED, a D
     * code for DISPOSED. A handover takes either, because what happens to the waste is decided by
     * the partner receiving it. GENERATED and COLLECTED take none — nothing has happened yet.
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
            case HANDED_OVER -> {
                if (code == null) {
                    throw new BusinessException(OPERATION_CODE_REQUIRED_HANDOVER);
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
     * Decides which legal register the quantity lands in. The caller may say, because one case is
     * genuinely ambiguous — handing over, recovering or disposing of goods taken from third parties
     * belongs to the art. 48 register, not to Anexa 1 — but the two ends are fixed by law and are
     * enforced rather than trusted: waste generated in the company's own activity is always Anexa 1
     * (art. 1 alin. (1) HG 856/2002), and a takeover is never Anexa 1 (art. 2 alin. (1)).
     */
    private WasteRegister resolveRegister(WasteMovementRequest request, Company company) {
        boolean takeover = request.operation() == WasteOperation.COLLECTED;
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
}
