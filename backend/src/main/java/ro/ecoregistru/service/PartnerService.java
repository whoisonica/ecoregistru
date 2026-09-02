package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.PartnerRequest;
import ro.ecoregistru.controller.response.PartnerResponse;
import ro.ecoregistru.controller.request.DriverRequest;
import ro.ecoregistru.controller.request.PartnerWorkPointRequest;
import ro.ecoregistru.controller.response.DriverResponse;
import ro.ecoregistru.controller.response.PartnerWorkPointResponse;
import ro.ecoregistru.entity.Driver;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.PartnerWorkPoint;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ro.ecoregistru.exception.BusinessException;

import static ro.ecoregistru.exception.ErrorMessageEnum.PARTNER_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.PARTNER_ROLE_REQUIRED;
import static ro.ecoregistru.exception.ErrorMessageEnum.PARTNER_TYPE_REQUIRED;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PartnerService {

    /** Authorization is considered "expiring soon" within this many days. */
    static final int EXPIRY_WARNING_DAYS = 60;

    PartnerRepository partnerRepository;
    CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public List<PartnerResponse> list() {
        UUID tenantId = TenantContext.require();
        return partnerRepository.findAllByCompany_Id(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public PartnerResponse create(PartnerRequest request) {
        UUID tenantId = TenantContext.require();
        requireCommercialRole(request);
        requireWasteRoleOrCarrier(request);
        Partner partner = Partner.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .name(request.name())
                .cui(request.cui())
                .authorizationNumber(request.authorizationNumber())
                .authorizationExpiry(request.authorizationExpiry())
                .type(request.type())
                .client(request.client())
                .supplier(request.supplier())
                .carrier(request.carrier())
                .address(request.address())
                .tradeRegisterNumber(request.tradeRegisterNumber())
                .transportLicenseNumber(request.transportLicenseNumber())
                .transportLicenseExpiry(request.transportLicenseExpiry())
                .active(true)
                .createdAt(Instant.now())
                .build();
        applyWorkPoints(partner, request);
        applyDrivers(partner, request);
        partnerRepository.save(partner);
        return toResponse(partner);
    }

    @Transactional
    public PartnerResponse update(UUID id, PartnerRequest request) {
        Partner partner = require(id);
        requireCommercialRole(request);
        requireWasteRoleOrCarrier(request);
        partner.setName(request.name());
        partner.setCui(request.cui());
        partner.setAuthorizationNumber(request.authorizationNumber());
        partner.setAuthorizationExpiry(request.authorizationExpiry());
        partner.setType(request.type());
        partner.setClient(request.client());
        partner.setSupplier(request.supplier());
        partner.setCarrier(request.carrier());
        partner.setAddress(request.address());
        applyWorkPoints(partner, request);
        applyDrivers(partner, request);
        partner.setTradeRegisterNumber(request.tradeRegisterNumber());
        partner.setTransportLicenseNumber(request.transportLicenseNumber());
        partner.setTransportLicenseExpiry(request.transportLicenseExpiry());
        return toResponse(partner);
    }

    @Transactional
    public void deactivate(UUID id) {
        require(id).setActive(false);
    }

    /**
     * A partner with neither role cannot be saved. Partners created before the split have none —
     * which way the money flows is not derivable from anything stored, so V7 did not guess — and
     * this is what makes editing one complete it, the same way V5 made editing a handover supply
     * its R/D code.
     */
    private void requireCommercialRole(PartnerRequest request) {
        if (!request.client() && !request.supplier()) {
            throw new BusinessException(PARTNER_ROLE_REQUIRED);
        }
    }

    /**
     * Either they do something with the waste, or they haul it. V28 made {@code type} nullable so a
     * pure haulage firm would not have to be filed as a collector — a guessed value on a rubric the
     * audit file prints and the Anexa 3 "Destinat:" ticks are read from. Nullable in the column,
     * not optional in the product: the rule moved here.
     */
    private void requireWasteRoleOrCarrier(PartnerRequest request) {
        if (request.type() == null && !request.carrier()) {
            throw new BusinessException(PARTNER_TYPE_REQUIRED);
        }
    }

    private Partner require(UUID id) {
        UUID tenantId = TenantContext.require();
        return partnerRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(PARTNER_NOT_FOUND));
    }

    /**
     * Replaces the partner's work points with what the form sent.
     *
     * <p>Wholesale rather than a diff, and null leaves them alone: an older client of this API that
     * does not know about the list must not wipe it by omission — the same rule the company profile
     * follows. A row already referenced by a movement keeps its id, so the Anexa 3 of a transport
     * that already happened still names the place it went to.
     */
    private void applyWorkPoints(Partner partner, PartnerRequest request) {
        if (request.workPoints() == null) {
            return;
        }
        Map<UUID, PartnerWorkPoint> existing = partner.getWorkPoints().stream()
                .collect(java.util.stream.Collectors.toMap(PartnerWorkPoint::getId, wp -> wp,
                        (a, b) -> a, java.util.LinkedHashMap::new));

        List<PartnerWorkPoint> kept = new java.util.ArrayList<>();
        for (PartnerWorkPointRequest wanted : request.workPoints()) {
            if (wanted.address() == null || wanted.address().isBlank()) {
                continue;
            }
            PartnerWorkPoint wp = wanted.id() == null ? null : existing.get(wanted.id());
            if (wp == null) {
                wp = PartnerWorkPoint.builder()
                        .partner(partner).active(true).createdAt(Instant.now()).build();
            }
            wp.setName(blankToNull(wanted.name()));
            wp.setAddress(wanted.address().trim());
            kept.add(wp);
        }
        // orphanRemoval deletes what is no longer in the list; clearing in place keeps the
        // collection Hibernate is tracking rather than swapping it for a new one.
        partner.getWorkPoints().clear();
        partner.getWorkPoints().addAll(kept);
    }

    /**
     * Replaces this carrier's drivers with what the form sent, on the same terms as the work
     * points: wholesale, and null leaves them alone.
     *
     * <p>Unticking "carrier" does <em>not</em> delete them. The tick is often taken off for a
     * month and put back — the collector who usually hauls but this season does not — and a list of
     * people typed in by hand should not evaporate on a checkbox.
     */
    private void applyDrivers(Partner partner, PartnerRequest request) {
        if (request.drivers() == null) {
            return;
        }
        Map<UUID, Driver> existing = partner.getDrivers().stream()
                .collect(java.util.stream.Collectors.toMap(Driver::getId, d -> d,
                        (a, b) -> a, java.util.LinkedHashMap::new));

        List<Driver> kept = new java.util.ArrayList<>();
        for (DriverRequest wanted : request.drivers()) {
            if (wanted.name() == null || wanted.name().isBlank()) {
                continue;
            }
            Driver driver = wanted.id() == null ? null : existing.get(wanted.id());
            if (driver == null) {
                driver = Driver.builder()
                        .partner(partner).company(partner.getCompany())
                        .active(true).createdAt(Instant.now()).build();
            }
            driver.setName(wanted.name().trim());
            driver.setIdentification(blankToNull(wanted.identification()));
            driver.setVehicleRegistration(blankToNull(wanted.vehicleRegistration()));
            kept.add(driver);
        }
        partner.getDrivers().clear();
        partner.getDrivers().addAll(kept);
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private PartnerResponse toResponse(Partner p) {
        boolean expiringSoon = p.getAuthorizationExpiry() != null
                && !p.getAuthorizationExpiry().isAfter(LocalDate.now().plusDays(EXPIRY_WARNING_DAYS));
        return new PartnerResponse(
                p.getId(), p.getName(), p.getCui(), p.getAuthorizationNumber(),
                p.getAuthorizationExpiry(), p.getType(), p.isClient(), p.isSupplier(),
                p.isCarrier(), p.isActive(), expiringSoon,
                p.getAddress(),
                p.getWorkPoints().stream()
                        .map(wp -> new PartnerWorkPointResponse(wp.getId(), wp.getName(),
                                wp.getAddress()))
                        .toList(),
                p.getTradeRegisterNumber(),
                p.getTransportLicenseNumber(), p.getTransportLicenseExpiry(),
                p.getDrivers().stream()
                        .map(d -> new DriverResponse(d.getId(), p.getId(), p.getName(), d.getName(),
                                d.getIdentification(), d.getVehicleRegistration(), d.isActive()))
                        .toList());
    }
}
