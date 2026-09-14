package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.export.Art48Register;
import ro.ecoregistru.service.export.Art48RegisterBuilder;
import ro.ecoregistru.service.export.Art48RegisterGenerator;
import ro.ecoregistru.service.export.ExportFormat;

import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.ART48_REGISTER_COLLECTORS_ONLY;
import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.EXPORT_FORMAT_UNSUPPORTED;
import static ro.ecoregistru.exception.ErrorMessageEnum.WORK_POINT_NOT_FOUND;

/**
 * The art. 48 chronological record of a company that takes waste over from third parties.
 * Read-only, so any member of the tenant may print it. See {@link Art48Register}.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Art48RegisterService {

    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;
    WasteMovementRepository movementRepository;
    Art48RegisterBuilder builder;
    Art48RegisterGenerator generator;

    @Transactional(readOnly = true)
    public Art48Register build(int year, UUID workPointId) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        // A generator has no register to print: its own record is the anexa 1 sheet.
        if (!company.getType().keepsArt48Register()) {
            throw new BusinessException(ART48_REGISTER_COLLECTORS_ONLY);
        }
        WorkPoint workPoint = workPointId == null ? null : workPointRepository.findById(workPointId)
                .filter(wp -> wp.getCompany().getId().equals(tenantId))
                .orElseThrow(() -> new NotFoundException(WORK_POINT_NOT_FOUND));
        // Every year up to this one, not just this one: the opening stock is what the earlier years left.
        return builder.build(company, workPoint, year,
                movementRepository.findAllByCompany_IdAndDeletedFalse(tenantId));
    }

    @Transactional(readOnly = true)
    public byte[] render(int year, UUID workPointId, ExportFormat format) {
        Art48Register register = build(year, workPointId);
        return switch (format) {
            case XLSX -> generator.xlsx(register);
            case PDF -> generator.pdf(register);
            case XLS -> throw new BadRequestException(EXPORT_FORMAT_UNSUPPORTED);
        };
    }
}
