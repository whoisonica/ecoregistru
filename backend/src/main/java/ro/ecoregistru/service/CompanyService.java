package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.CompanyRequest;
import ro.ecoregistru.controller.request.InviteUserRequest;
import ro.ecoregistru.controller.response.CompanyResponse;
import ro.ecoregistru.controller.response.CompanyUserResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.exception.UnprocessableEntityException;
import ro.ecoregistru.repository.CompanyRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_CUI_ALREADY_EXISTS;
import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.INVALID_CUI;

/**
 * Platform-level company (tenant) management. All operations here are global, NOT tenant-scoped —
 * a deliberate exception to the "everything is tenant-scoped" rule. Access is gated to
 * PLATFORM_ADMIN at the controller; never expose these to normal, tenant-scoped users.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompanyService {

    /** Romanian fiscal code: optional "RO" prefix + 2–10 digits (e.g. RO12345678 or 12345678). */
    private static final Pattern CUI_PATTERN = Pattern.compile("^(RO)?\\d{2,10}$");

    CompanyRepository companyRepository;
    AuthenticationService authenticationService;

    @Transactional(readOnly = true)
    public List<CompanyResponse> listAll() {
        return companyRepository.findAll().stream()
                .sorted(Comparator.comparing(Company::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        String cui = normalizeCui(request.cui());
        if (companyRepository.existsByCui(cui)) {
            throw new UnprocessableEntityException(COMPANY_CUI_ALREADY_EXISTS);
        }
        Company company = Company.builder()
                .name(request.name().trim())
                .cui(cui)
                .type(request.type())
                .afmObligation(request.afmObligation())
                .active(true)
                .createdAt(Instant.now())
                .build();
        applyEditableFields(company, request);
        companyRepository.save(company);
        return toResponse(company);
    }

    @Transactional
    public CompanyResponse update(UUID id, CompanyRequest request) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        String cui = normalizeCui(request.cui());
        if (!cui.equals(company.getCui()) && companyRepository.existsByCui(cui)) {
            throw new UnprocessableEntityException(COMPANY_CUI_ALREADY_EXISTS);
        }
        company.setName(request.name().trim());
        company.setCui(cui);
        company.setType(request.type());
        company.setAfmObligation(request.afmObligation());
        applyEditableFields(company, request);
        return toResponse(company);
    }

    /**
     * Invite a user onto the given company. Delegates the actual user creation + reset-password
     * email to AuthenticationService (single source of truth for the verification flow).
     */
    @Transactional
    public CompanyUserResponse inviteUser(UUID companyId, InviteUserRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        AppUser user = authenticationService.inviteUser(
                company, request.email(), request.role(), request.firstName(), request.lastName());
        return new CompanyUserResponse(
                user.getId(), user.getEmail(), user.getRole(),
                user.getFirstName(), user.getLastName(), user.isEnabled());
    }

    private void applyEditableFields(Company company, CompanyRequest request) {
        company.setEnvironmentalAuthNumber(blankToNull(request.environmentalAuthNumber()));
        company.setEnvironmentalAuthExpiry(request.environmentalAuthExpiry());
        company.setAddress(blankToNull(request.address()));
        company.setContactName(blankToNull(request.contactName()));
        company.setContactEmail(blankToNull(request.contactEmail()));
        company.setContactPhone(blankToNull(request.contactPhone()));
    }

    /** Normalizes a CUI to upper-case, no spaces, and validates its shape. */
    private String normalizeCui(String raw) {
        String cui = raw == null ? "" : raw.replaceAll("\\s", "").toUpperCase();
        if (!CUI_PATTERN.matcher(cui).matches()) {
            throw new BusinessException(INVALID_CUI);
        }
        return cui;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CompanyResponse toResponse(Company c) {
        return new CompanyResponse(
                c.getId(), c.getName(), c.getCui(), c.getType(), c.isActive(), c.isAfmObligation(),
                c.getEnvironmentalAuthNumber(), c.getEnvironmentalAuthExpiry(), c.getAddress(),
                c.getContactName(), c.getContactEmail(), c.getContactPhone());
    }
}
