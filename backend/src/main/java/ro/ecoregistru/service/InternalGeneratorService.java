package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.InternalGeneratorRequest;
import ro.ecoregistru.controller.response.InternalGeneratorResponse;
import ro.ecoregistru.entity.InternalGenerator;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.InternalGeneratorRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.INTERNAL_GENERATOR_NAME_TAKEN;
import static ro.ecoregistru.exception.ErrorMessageEnum.INTERNAL_GENERATOR_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.INTERNAL_GENERATOR_WORK_POINT_IMMUTABLE;
import static ro.ecoregistru.exception.ErrorMessageEnum.WORK_POINT_NOT_FOUND;

/**
 * Tenant-scoped CRUD for internal generators — the "Secţia" of Anexa 1 cap. 2.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalGeneratorService {

    InternalGeneratorRepository internalGeneratorRepository;
    WorkPointRepository workPointRepository;
    CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<InternalGeneratorResponse> list(UUID workPointId) {
        UUID tenantId = TenantContext.require();
        List<InternalGenerator> found = workPointId == null
                ? internalGeneratorRepository.findAllByCompany_IdOrderByNameAsc(tenantId)
                : internalGeneratorRepository
                        .findAllByCompany_IdAndWorkPoint_IdOrderByNameAsc(tenantId, workPointId);
        return found.stream().map(this::toResponse).toList();
    }

    @Transactional
    public InternalGeneratorResponse create(InternalGeneratorRequest request) {
        UUID tenantId = TenantContext.require();
        WorkPoint workPoint = requireWorkPoint(request.workPointId(), tenantId);
        requireNameFree(workPoint.getId(), request.name());

        InternalGenerator generator = InternalGenerator.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .workPoint(workPoint)
                .name(request.name().trim())
                .description(request.description())
                .active(true)
                .createdAt(Instant.now())
                .build();
        internalGeneratorRepository.save(generator);
        return toResponse(generator);
    }

    @Transactional
    public InternalGeneratorResponse update(UUID id, InternalGeneratorRequest request) {
        InternalGenerator generator = require(id);

        // Moving a section to another work point would rewrite the "Secţia" column of sheets
        // already printed for the old one. Deactivate it there and add it here instead.
        if (!generator.getWorkPoint().getId().equals(request.workPointId())) {
            throw new BusinessException(INTERNAL_GENERATOR_WORK_POINT_IMMUTABLE);
        }
        if (!generator.getName().equalsIgnoreCase(request.name().trim())) {
            requireNameFree(generator.getWorkPoint().getId(), request.name());
        }

        generator.setName(request.name().trim());
        generator.setDescription(request.description());
        return toResponse(generator);
    }

    @Transactional
    public void deactivate(UUID id) {
        require(id).setActive(false);
    }

    private void requireNameFree(UUID workPointId, String name) {
        if (internalGeneratorRepository.existsByWorkPoint_IdAndNameIgnoreCase(workPointId, name.trim())) {
            throw new BusinessException(INTERNAL_GENERATOR_NAME_TAKEN);
        }
    }

    private InternalGenerator require(UUID id) {
        UUID tenantId = TenantContext.require();
        return internalGeneratorRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(INTERNAL_GENERATOR_NOT_FOUND));
    }

    private WorkPoint requireWorkPoint(UUID workPointId, UUID tenantId) {
        return workPointRepository.findByIdAndCompany_Id(workPointId, tenantId)
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND));
    }

    private InternalGeneratorResponse toResponse(InternalGenerator g) {
        return new InternalGeneratorResponse(
                g.getId(),
                g.getWorkPoint().getId(),
                g.getWorkPoint().getName(),
                g.getName(),
                g.getDescription(),
                g.isActive());
    }
}
