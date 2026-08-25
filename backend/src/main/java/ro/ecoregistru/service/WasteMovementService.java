package ro.ecoregistru.service;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ro.ecoregistru.controller.request.RecordWeightRequest;
import ro.ecoregistru.controller.request.WasteMovementRequest;
import ro.ecoregistru.controller.response.AttachmentResponse;
import ro.ecoregistru.controller.response.WasteMovementResponse;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.PackagingMaterial;
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
    PartnerWorkPointRepository partnerWorkPointRepository;
    InternalGeneratorRepository internalGeneratorRepository;
    AttachmentRepository attachmentRepository;
    CloudinaryStorageService storageService;
    ro.ecoregistru.service.export.Anexa3FormGenerator anexa3FormGenerator;
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
                // Ambalaje: cele trei rubrici ale tabelului 1 călătoresc pe mişcare, dar numai pe
                // un cod 15 01 xx. Pe orice alt cod se ignoră, ca să nu rămână un răspuns agăţat
                // de o mişcare pe care declaraţia n-o citeşte niciodată.
                .packagingMaterial(packagingOnly(wasteCode, request.packagingMaterial()))
                .packagingCategory(packagingOnly(wasteCode, request.packagingCategory()))
                .packagingReusable(packagingOnly(wasteCode, request.packagingReusable()))
                .packagingHazardousContent(
                        packagingOnly(wasteCode, request.packagingHazardousContent()))
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
        movement.setPackagingMaterial(packagingOnly(wasteCode, request.packagingMaterial()));
        movement.setPackagingCategory(packagingOnly(wasteCode, request.packagingCategory()));
        movement.setPackagingReusable(packagingOnly(wasteCode, request.packagingReusable()));
        movement.setPackagingHazardousContent(
                packagingOnly(wasteCode, request.packagingHazardousContent()));
        movement.setTransportPartner(carrier);
        movement.setDriverName(request.driverName());
        movement.setDriverIdentification(request.driverIdentification());
        movement.setVehicleRegistration(request.vehicleRegistration());
        movement.setTransportDestinations(request.transportDestinations() == null
                ? new java.util.LinkedHashSet<>()
                : new java.util.LinkedHashSet<>(request.transportDestinations()));
        movement.setDocumentReference(request.documentReference());
        movement.setNotes(request.notes());

        return mapper.toResponse(movement);
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

    /**
     * Renders Anexa 3 la HG 1061/2008 for a movement that is already recorded, allocating the
     * form's number the first time so a reprint stays the same document.
     *
     * <p>Two refusals, both legal rather than technical. The form covers a handover — it names an
     * expeditor and a destinatar — so a movement with no partner has nobody to print on the right
     * half. And its title says <em>nepericuloase</em>: a hazardous code belongs on the expedition
     * form of anexa 2, which is a different document we do not produce yet, so we say that instead
     * of printing the wrong one.
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
}
