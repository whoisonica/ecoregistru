package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.VehicleRequest;
import ro.ecoregistru.controller.response.VehicleResponse;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.Vehicle;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.VehicleRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * Flota firmei (D2.1). Aceleași trepte ca la șoferi: dezactivare reversibilă, ștergere definitivă
 * doar după dezactivare. Operațiunile nu se ating la ștergere: țin numărul tipărit ca text.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VehicleService {

    VehicleRepository vehicleRepository;
    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;
    PartnerRepository partnerRepository;

    @Transactional(readOnly = true)
    public List<VehicleResponse> list() {
        return vehicleRepository.findAllForCompany(TenantContext.require()).stream()
                .map(VehicleService::toResponse).toList();
    }

    @Transactional
    public VehicleResponse create(VehicleRequest request) {
        UUID tenantId = TenantContext.require();
        Vehicle vehicle = Vehicle.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .active(true)
                .createdAt(Instant.now())
                .build();
        apply(vehicle, request, tenantId);
        vehicleRepository.save(vehicle);
        return toResponse(vehicle);
    }

    @Transactional
    public VehicleResponse update(UUID id, VehicleRequest request) {
        UUID tenantId = TenantContext.require();
        Vehicle vehicle = require(id, tenantId);
        apply(vehicle, request, tenantId);
        return toResponse(vehicle);
    }

    @Transactional
    public void deactivate(UUID id) {
        require(id, TenantContext.require()).setActive(false);
    }

    @Transactional
    public void reactivate(UUID id) {
        require(id, TenantContext.require()).setActive(true);
    }

    @Transactional
    public void delete(UUID id) {
        Vehicle vehicle = require(id, TenantContext.require());
        if (vehicle.isActive()) {
            throw new BusinessException(VEHICLE_DELETE_REQUIRES_DEACTIVATION);
        }
        vehicleRepository.delete(vehicle);
    }

    /**
     * Numărul se normalizează (majuscule, fără spații și cratime), ca unicitatea să prindă același
     * camion scris altfel. Sub 3,5 t licența se golește: rubrica nu are obiect, iar baza o refuză.
     */
    private void apply(Vehicle vehicle, VehicleRequest request, UUID tenantId) {
        String registration = normalizeRegistration(request.registration());
        if (registration == null) {
            throw new BusinessException(VEHICLE_REGISTRATION_REQUIRED);
        }
        vehicleRepository.findByCompany_IdAndRegistration(tenantId, registration)
                .filter(other -> !other.getId().equals(vehicle.getId()))
                .ifPresent(other -> {
                    throw new BusinessException(VEHICLE_REGISTRATION_TAKEN);
                });
        if (request.standardTareKg() != null && request.standardTareKg().signum() <= 0) {
            throw new BusinessException(VEHICLE_TARE_NOT_POSITIVE);
        }
        WorkPoint home = request.homeWorkPointId() == null ? null
                : workPointRepository.findByIdAndCompany_Id(request.homeWorkPointId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND));
        Partner carrier = request.partnerId() == null ? null
                : partnerRepository.findByIdAndCompany_Id(request.partnerId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(PARTNER_NOT_FOUND));
        if (carrier != null && !carrier.isCarrier()) {
            throw new BusinessException(VEHICLE_PARTNER_NOT_CARRIER);
        }

        vehicle.setRegistration(registration);
        vehicle.setKind(blankToNull(request.kind()));
        vehicle.setStandardTareKg(request.standardTareKg());
        vehicle.setHeavy(request.heavy());
        vehicle.setItpExpiry(request.itpExpiry());
        vehicle.setTransportLicenseNumber(request.heavy() ? blankToNull(request.transportLicenseNumber()) : null);
        vehicle.setTransportLicenseExpiry(request.heavy() ? request.transportLicenseExpiry() : null);
        vehicle.setHomeWorkPoint(home);
        vehicle.setPartner(carrier);
    }

    private Vehicle require(UUID id, UUID tenantId) {
        return vehicleRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(VEHICLE_NOT_FOUND));
    }

    static String normalizeRegistration(String value) {
        if (value == null) return null;
        String normalized = value.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static VehicleResponse toResponse(Vehicle v) {
        WorkPoint home = v.getHomeWorkPoint();
        Partner partner = v.getPartner();
        return new VehicleResponse(v.getId(), v.getRegistration(), v.getKind(), v.getStandardTareKg(),
                v.isHeavy(), v.getItpExpiry(), v.getTransportLicenseNumber(), v.getTransportLicenseExpiry(),
                home == null ? null : home.getId(), home == null ? null : home.getName(),
                partner == null ? null : partner.getId(), partner == null ? null : partner.getName(),
                v.isActive());
    }
}
