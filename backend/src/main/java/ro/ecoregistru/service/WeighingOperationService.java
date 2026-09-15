package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Driver;
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.DriverRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;

import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * Operațiunile de depozit (V46, D1.2): capul numerotat. Liniile, cântarul, stările și plata vin
 * în punctele următoare ale feliei.
 *
 * <p><b>Numerotarea</b> e pe firmă și pe tip, fără goluri la creările obișnuite: maximul + 1, sub un
 * lacăt consultativ ținut până la commit ({@link WeighingOperationRepository#lockNumbering}).
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WeighingOperationService {

    WeighingOperationRepository operationRepository;
    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;
    PartnerRepository partnerRepository;
    NaturalPersonRepository naturalPersonRepository;
    DriverRepository driverRepository;

    @Transactional
    public WeighingOperationResponse create(WeighingOperationRequest request) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        if (!company.getType().keepsArt48Register()) {
            throw new BusinessException(ART48_REGISTER_NOT_ENABLED);
        }
        WeighingOperationType type = requireAvailableType(request.type());
        if (request.date() == null) {
            throw new BusinessException(WEIGHING_OPERATION_DATE_REQUIRED);
        }
        if (request.partnerId() != null && request.naturalPersonId() != null) {
            throw new BusinessException(WEIGHING_OPERATION_ONE_COUNTERPARTY);
        }
        if (request.naturalPersonId() != null && type != WeighingOperationType.IN) {
            throw new BusinessException(WEIGHING_OPERATION_PERSON_ONLY_IN);
        }

        WorkPoint workPoint = workPointRepository.findByIdAndCompany_Id(request.workPointId(), tenantId)
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND));
        Partner partner = request.partnerId() == null ? null
                : partnerRepository.findByIdAndCompany_Id(request.partnerId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(PARTNER_NOT_FOUND));
        NaturalPerson person = request.naturalPersonId() == null ? null
                : naturalPersonRepository.findByIdAndCompany_Id(request.naturalPersonId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(NATURAL_PERSON_NOT_FOUND));
        Driver driver = request.driverId() == null ? null
                : driverRepository.findByIdAndCompany_Id(request.driverId(), tenantId)
                        .orElseThrow(() -> new NotFoundException(DRIVER_NOT_FOUND));

        operationRepository.lockNumbering(tenantId + ":" + type);
        Integer max = operationRepository.findMaxNumber(tenantId, type);

        WeighingOperation operation = WeighingOperation.builder()
                .company(company)
                .workPoint(workPoint)
                .type(type)
                .number(max == null ? 1 : max + 1)
                .date(request.date())
                .partner(partner)
                .naturalPerson(person)
                .origin(resolveOrigin(request, partner, person))
                .driver(driver)
                .driverName(firstNonBlank(request.driverName(), driver == null ? null : driver.getName()))
                .vehicleRegistration(firstNonBlank(request.vehicleRegistration(),
                        driver == null ? null : driver.getVehicleRegistration()))
                .orderNumber(blankToNull(request.orderNumber()))
                .notes(blankToNull(request.notes()))
                .status(WeighingOperationStatus.IN_PROGRESS)
                .createdBy(SecurityUtils.currentUser().getId())
                .build();
        operationRepository.save(operation);
        return toResponse(operation);
    }

    @Transactional(readOnly = true)
    public WeighingOperationResponse get(UUID id) {
        UUID tenantId = TenantContext.require();
        return toResponse(operationRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(WEIGHING_OPERATION_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public List<WeighingOperationResponse> list() {
        UUID tenantId = TenantContext.require();
        return operationRepository.findAllByCompany_IdOrderByDateDescNumberDesc(tenantId).stream()
                .map(WeighingOperationService::toResponse).toList();
    }

    private static WeighingOperationType requireAvailableType(WeighingOperationType type) {
        if (type == null) {
            throw new BusinessException(WEIGHING_OPERATION_TYPE_REQUIRED);
        }
        if (!type.isAvailable()) {
            throw new BusinessException(WEIGHING_OPERATION_TYPE_UNAVAILABLE);
        }
        return type;
    }

    /**
     * La o persoană fizică originea e mereu „populație”. La un partener, ce s-a ales pe operațiune,
     * altfel ce spune fișa lui; gol dacă nu spune nimeni — nu se ghicește o rubrică de registru.
     */
    private static PackagingOrigin resolveOrigin(WeighingOperationRequest request, Partner partner,
                                                 NaturalPerson person) {
        if (person != null) {
            return PackagingOrigin.POPULATIE;
        }
        if (request.origin() == PackagingOrigin.POPULATIE) {
            return null;
        }
        return request.origin() != null ? request.origin()
                : partner == null ? null : partner.getPackagingOrigin();
    }

    private static String firstNonBlank(String typed, String fallback) {
        String value = blankToNull(typed);
        return value != null ? value : blankToNull(fallback);
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static WeighingOperationResponse toResponse(WeighingOperation o) {
        Partner partner = o.getPartner();
        NaturalPerson person = o.getNaturalPerson();
        return new WeighingOperationResponse(o.getId(), o.getType(), o.getNumber(), o.getDate(),
                o.getWorkPoint().getId(), o.getWorkPoint().getName(),
                partner == null ? null : partner.getId(), partner == null ? null : partner.getName(),
                person == null ? null : person.getId(), person == null ? null : person.getName(),
                o.getOrigin(), o.getDriverName(), o.getVehicleRegistration(), o.getOrderNumber(),
                o.getStatus(), o.getNotes());
    }
}
