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
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.DriverRepository;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.DRIVER_BELONGS_TO_PARTNER;
import static ro.ecoregistru.exception.ErrorMessageEnum.DRIVER_NAME_REQUIRED;
import static ro.ecoregistru.exception.ErrorMessageEnum.DRIVER_NOT_FOUND;

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
                .identification(blankToNull(request.identification()))
                .vehicleRegistration(blankToNull(request.vehicleRegistration()))
                .active(true)
                .createdAt(Instant.now())
                .build();
        driverRepository.save(driver);
        return toResponse(driver);
    }

    @Transactional
    public DriverResponse update(UUID id, DriverRequest request) {
        Driver driver = requireOwn(id);
        driver.setName(requireName(request));
        driver.setIdentification(blankToNull(request.identification()));
        driver.setVehicleRegistration(blankToNull(request.vehicleRegistration()));
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
        return new DriverResponse(d.getId(),
                partner == null ? null : partner.getId(),
                partner == null ? null : partner.getName(),
                d.getName(), d.getIdentification(), d.getVehicleRegistration(), d.isActive());
    }
}
