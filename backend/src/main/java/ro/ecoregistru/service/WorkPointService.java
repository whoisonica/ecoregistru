package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.WorkPointRequest;
import ro.ecoregistru.controller.response.WorkPointResponse;
import ro.ecoregistru.entity.InternalGenerator;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.InternalGeneratorRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.WORK_POINT_NOT_FOUND;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkPointService {

    WorkPointRepository workPointRepository;
    InternalGeneratorRepository internalGeneratorRepository;
    CompanyRepository companyRepository;

    /** Verbatim what the filled sheets carry in the "Secţia" column: "birouri", "productie". */
    private static final List<String> DEFAULT_SECTIONS = List.of("Birouri", "Producţie");

    @Transactional(readOnly = true)
    public List<WorkPointResponse> list() {
        UUID tenantId = TenantContext.require();
        return workPointRepository.findAllByCompany_Id(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public WorkPointResponse create(WorkPointRequest request) {
        UUID tenantId = TenantContext.require();
        WorkPoint workPoint = WorkPoint.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .name(request.name())
                .address(request.address())
                .active(true)
                .createdAt(Instant.now())
                .build();
        workPointRepository.save(workPoint);
        seedDefaultSections(workPoint);
        return toResponse(workPoint);
    }

    /**
     * Every new work point starts with the two sections nearly every site has: <b>Birouri</b> and
     * <b>Producţie</b>.
     *
     * <p>They are what Anexa 1 cap. 2 prints under "Secţia", and asking a client to invent the
     * column from scratch is how it ends up empty on a filed form — which is exactly what the
     * specialist saw on 25.08.2026 in her own account. Predefined, not imposed: they can be
     * renamed or removed, and a movement still says which one the waste came from.
     */
    private void seedDefaultSections(WorkPoint workPoint) {
        for (String name : DEFAULT_SECTIONS) {
            internalGeneratorRepository.save(InternalGenerator.builder()
                    .company(workPoint.getCompany())
                    .workPoint(workPoint)
                    .name(name)
                    .active(true)
                    .createdAt(Instant.now())
                    .build());
        }
    }

    @Transactional
    public WorkPointResponse update(UUID id, WorkPointRequest request) {
        WorkPoint workPoint = require(id);
        workPoint.setName(request.name());
        workPoint.setAddress(request.address());
        return toResponse(workPoint);
    }

    @Transactional
    public void deactivate(UUID id) {
        require(id).setActive(false);
    }

    private WorkPoint require(UUID id) {
        UUID tenantId = TenantContext.require();
        return workPointRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND));
    }

    private WorkPointResponse toResponse(WorkPoint w) {
        return new WorkPointResponse(w.getId(), w.getName(), w.getAddress(), w.isActive());
    }
}
