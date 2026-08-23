package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.PartnerRequest;
import ro.ecoregistru.controller.response.PartnerResponse;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import ro.ecoregistru.exception.BusinessException;

import static ro.ecoregistru.exception.ErrorMessageEnum.PARTNER_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.PARTNER_ROLE_REQUIRED;

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
        Partner partner = Partner.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .name(request.name())
                .cui(request.cui())
                .authorizationNumber(request.authorizationNumber())
                .authorizationExpiry(request.authorizationExpiry())
                .type(request.type())
                .client(request.client())
                .supplier(request.supplier())
                .address(request.address())
                .workPointAddress(request.workPointAddress())
                .tradeRegisterNumber(request.tradeRegisterNumber())
                .transportLicenseNumber(request.transportLicenseNumber())
                .transportLicenseExpiry(request.transportLicenseExpiry())
                .active(true)
                .createdAt(Instant.now())
                .build();
        partnerRepository.save(partner);
        return toResponse(partner);
    }

    @Transactional
    public PartnerResponse update(UUID id, PartnerRequest request) {
        Partner partner = require(id);
        requireCommercialRole(request);
        partner.setName(request.name());
        partner.setCui(request.cui());
        partner.setAuthorizationNumber(request.authorizationNumber());
        partner.setAuthorizationExpiry(request.authorizationExpiry());
        partner.setType(request.type());
        partner.setClient(request.client());
        partner.setSupplier(request.supplier());
        partner.setAddress(request.address());
        partner.setWorkPointAddress(request.workPointAddress());
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

    private Partner require(UUID id) {
        UUID tenantId = TenantContext.require();
        return partnerRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(PARTNER_NOT_FOUND));
    }

    private PartnerResponse toResponse(Partner p) {
        boolean expiringSoon = p.getAuthorizationExpiry() != null
                && !p.getAuthorizationExpiry().isAfter(LocalDate.now().plusDays(EXPIRY_WARNING_DAYS));
        return new PartnerResponse(
                p.getId(), p.getName(), p.getCui(), p.getAuthorizationNumber(),
                p.getAuthorizationExpiry(), p.getType(), p.isClient(), p.isSupplier(),
                p.isActive(), expiringSoon,
                p.getAddress(), p.getWorkPointAddress(), p.getTradeRegisterNumber(),
                p.getTransportLicenseNumber(), p.getTransportLicenseExpiry());
    }
}
