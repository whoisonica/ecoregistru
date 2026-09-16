package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.DriverRequest;
import ro.ecoregistru.controller.response.DriverResponse;
import ro.ecoregistru.entity.Driver;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.DriverRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.DRIVER_BELONGS_TO_PARTNER;
import static ro.ecoregistru.exception.ErrorMessageEnum.DRIVER_DELETE_REQUIRES_DEACTIVATION;
import static ro.ecoregistru.exception.ErrorMessageEnum.DRIVER_NAME_REQUIRED;
import static ro.ecoregistru.exception.ErrorMessageEnum.DRIVER_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.WORK_POINT_NOT_FOUND;

/**
 * Our <em>own</em> drivers — the rows of {@code drivers} with no partner, which is the
 * "— transportăm noi —" case of the movement form.
 *
 * <p>A carrier's drivers are deliberately <b>not</b> writable here: they are edited nested in the
 * partner form, like its work points, and that list is replaced wholesale on save. Two write paths
 * into the same rows would mean a driver added through this endpoint disappears the next time
 * somebody opens and saves the partner. So the write methods refuse a driver that belongs to a
 * partner, and only {@link #list()} sees both — the movement form needs every driver in one call.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DriverService {

    DriverRepository driverRepository;
    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;

    /** Every driver of the tenant, ours and the carriers'. The movement form filters client-side. */
    @Transactional(readOnly = true)
    public List<DriverResponse> list() {
        UUID tenantId = TenantContext.require();
        return driverRepository.findAllByCompany_IdOrderByNameAsc(tenantId).stream()
                .map(DriverService::toResponse).toList();
    }

    @Transactional
    public DriverResponse create(DriverRequest request) {
        UUID tenantId = TenantContext.require();
        String name = requireName(request);
        Driver driver = Driver.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .name(name)
                .active(true)
                .createdAt(Instant.now())
                .build();
        apply(driver, request, tenantId);
        driverRepository.save(driver);
        return toResponse(driver);
    }

    @Transactional
    public DriverResponse update(UUID id, DriverRequest request) {
        Driver driver = requireOwn(id);
        driver.setName(requireName(request));
        apply(driver, request, TenantContext.require());
        return toResponse(driver);
    }

    /**
     * Deactivates rather than deletes: the man may have driven twenty transports, and although the
     * movement keeps his details as text, the list is also the record of who we have on the road.
     */
    @Transactional
    public void deactivate(UUID id) {
        requireOwn(id).setActive(false);
    }

    /**
     * Şterge definitiv fişa unui şofer al nostru — AO, 14.09.2026: specialista a lăsat decizia la
     * noi, „cu atenţie la GDPR". Numai după dezactivare, ca un clic greşit să nu fie ireversibil.
     *
     * <p>Mişcările nu se ating: ţin instantaneul lor ca text, nu o cheie spre fişă, iar Anexa 3
     * trebuie să iasă la fel cât se păstrează evidenţa. De pe ele datele şoferului pleacă separat, la
     * termen — {@link DriverDataRetentionScheduler}.
     */
    @Transactional
    public void delete(UUID id) {
        Driver driver = requireOwn(id);
        if (driver.isActive()) {
            throw new BusinessException(DRIVER_DELETE_REQUIRES_DEACTIVATION);
        }
        driverRepository.delete(driver);
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
        requireOwn(id).setActive(true);
    }

    /** D2.2 — depozitul implicit trebuie să fie al firmei; atestatul e liber, data lui e opțională. */
    private void apply(Driver driver, DriverRequest request, UUID tenantId) {
        WorkPoint home = request.homeWorkPointId() == null ? null
                : workPointRepository.findByIdAndCompany_Id(request.homeWorkPointId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND));
        driver.setIdentification(blankToNull(request.identification()));
        driver.setCnp(blankToNull(request.cnp()));
        driver.setVehicleRegistration(blankToNull(request.vehicleRegistration()));
        driver.setHomeWorkPoint(home);
        driver.setAttestationNumber(blankToNull(request.attestationNumber()));
        driver.setAttestationExpiry(request.attestationExpiry());
    }

    private Driver requireOwn(UUID id) {
        UUID tenantId = TenantContext.require();
        Driver driver = driverRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(DRIVER_NOT_FOUND));
        if (driver.getPartner() != null) {
            throw new BusinessException(DRIVER_BELONGS_TO_PARTNER);
        }
        return driver;
    }

    private static String requireName(DriverRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException(DRIVER_NAME_REQUIRED);
        }
        return request.name().trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static DriverResponse toResponse(Driver d) {
        Partner partner = d.getPartner();
        WorkPoint home = d.getHomeWorkPoint();
        return new DriverResponse(d.getId(),
                partner == null ? null : partner.getId(),
                partner == null ? null : partner.getName(),
                d.getName(), d.getIdentification(), d.getCnp(), d.getVehicleRegistration(),
                home == null ? null : home.getId(), home == null ? null : home.getName(),
                d.getAttestationNumber(), d.getAttestationExpiry(),
                d.isActive());
    }
}
