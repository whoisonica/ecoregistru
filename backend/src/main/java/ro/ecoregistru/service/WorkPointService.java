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
    DepotAccess depotAccess;

    /** Verbatim what the filled sheets carry in the "Secţia" column: "birouri", "productie". */
    private static final List<String> DEFAULT_SECTIONS = List.of("Birouri", "Producţie");

    @Transactional(readOnly = true)
    public List<WorkPointResponse> list() {
        UUID tenantId = TenantContext.require();
        // D2.4 — un utilizator restrâns își vede doar depozitele; de aici le iau toate selectoarele.
        return depotAccess.filter(workPointRepository.findAllByCompany_Id(tenantId), WorkPoint::getId).stream()
                .map(this::toResponse).toList();
    }

    /** D2.5 — un depozit la care poate pleca un transfer: numai ce trebuie ca să-l alegi. */
    public record TransferTarget(UUID id, String name) {
    }

    @Transactional(readOnly = true)
    public List<TransferTarget> transferTargets() {
        return workPointRepository.findAllByCompany_Id(TenantContext.require()).stream()
                .filter(WorkPoint::isActive)
                .map(w -> new TransferTarget(w.getId(), w.getName()))
                .toList();
    }

    @Transactional
    public WorkPointResponse create(WorkPointRequest request) {
        UUID tenantId = TenantContext.require();
        WorkPoint workPoint = WorkPoint.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .name(request.name())
                .address(request.address())
                .environmentalAuthNumber(blankToNull(request.environmentalAuthNumber()))
                .environmentalAuthExpiry(request.environmentalAuthExpiry())
                .receivedFormsSeries(blankToNull(request.receivedFormsSeries()))
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
     *
     * <p>Public because a work point is also born outside this service: the approval of an account
     * request creates the first one, and the two sections have to be there from the account's first
     * day (proprietarul, 16.09.2026).
     */
    public void seedDefaultSections(WorkPoint workPoint) {
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
        workPoint.setEnvironmentalAuthNumber(blankToNull(request.environmentalAuthNumber()));
        workPoint.setEnvironmentalAuthExpiry(request.environmentalAuthExpiry());
        workPoint.setReceivedFormsSeries(blankToNull(request.receivedFormsSeries()));
        return toResponse(workPoint);
    }

    @Transactional
    public void deactivate(UUID id) {
        require(id).setActive(false);
    }

    /**
     * Desface dezactivarea.
     *
     * <p>Nu exista: prima greșeală era definitivă. Un punct de lucru dezactivat din greșeală, un
     * partener scos la curățenie și regăsit peste o lună — amândouă rămâneau pe ecran, cu badge-ul
     * „Inactiv", și nu se mai putea face nimic cu ele. Or dezactivarea e dinadins reversibilă: nu
     * șterge nimic, doar scoate rândul din listele de ales.
     *
     * <p>Simetrică pe față cu {@code deactivate}: aceleași verificări de tenant, același răspuns
     * gol. Nicio unicitate nu se poate strica, fiindcă cea care există — numele secției într-un
     * punct de lucru — numără și rândurile inactive, deci un nume liber azi n-a fost al nimănui.
     */
    @Transactional
    public void reactivate(UUID id) {
        require(id).setActive(true);
    }

    private WorkPoint require(UUID id) {
        UUID tenantId = TenantContext.require();
        return workPointRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND));
    }

    private WorkPointResponse toResponse(WorkPoint w) {
        return new WorkPointResponse(w.getId(), w.getName(), w.getAddress(), w.isActive(),
                w.getEnvironmentalAuthNumber(), w.getEnvironmentalAuthExpiry(), w.getReceivedFormsSeries());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
