package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.export.Anexa3Register;
import ro.ecoregistru.service.export.Anexa3RegisterBuilder;
import ro.ecoregistru.service.export.Anexa3RegisterGenerator;

import java.time.LocalDate;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.WORK_POINT_NOT_FOUND;

/**
 * Registrul Anexa 3 (transport) al unei firme, pe an (Andreea, 29.09.2026). Read-only, so any member
 * of the tenant may print it: nothing is allocated here — the numbers were allocated when each form
 * was first printed. See {@link Anexa3Register}.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Anexa3RegisterService {

    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;
    WasteMovementRepository movementRepository;
    Anexa3RegisterBuilder builder;
    Anexa3RegisterGenerator generator;

    @Transactional(readOnly = true)
    public Anexa3Register build(int year, UUID workPointId) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        WorkPoint workPoint = workPointId == null ? null : workPointRepository.findById(workPointId)
                .filter(wp -> wp.getCompany().getId().equals(tenantId))
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND));
        return builder.build(company, workPoint, year,
                movementRepository.findCountedBetween(tenantId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)));
    }

    @Transactional(readOnly = true)
    public byte[] render(int year, UUID workPointId) {
        return generator.pdf(build(year, workPointId));
    }
}
