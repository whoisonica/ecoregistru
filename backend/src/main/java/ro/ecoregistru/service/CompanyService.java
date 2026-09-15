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
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.exception.UnprocessableEntityException;
import ro.ecoregistru.controller.response.WasteCodeResponse;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ConsultancyRepository;
import ro.ecoregistru.repository.SubscriptionRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_CUI_ALREADY_EXISTS;
import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_HAS_OWN_SUBSCRIPTION;
import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_CUI_UNAVAILABLE;
import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.CONSULTANCY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.INVALID_CUI;

/**
 * Company (tenant) management, above the tenant rather than inside one — a deliberate exception to
 * the "everything is tenant-scoped" rule. Two callers, gated at the controller:
 *
 * <ul>
 *   <li>{@code PLATFORM_ADMIN} — every company, and the only one who moves a company between
 *       consultancies.</li>
 *   <li>{@code CONSULTANT} (P2.13) — the companies of their own consultancy, and nothing else. A
 *       company they create joins that consultancy; an id outside it is a 404, never a 403, so the
 *       answer does not confirm that the id exists somewhere.</li>
 * </ul>
 *
 * Never expose these to the tenant-scoped roles.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompanyService {

    /** Romanian fiscal code: optional "RO" prefix + 2–10 digits (e.g. RO12345678 or 12345678). */
    private static final Pattern CUI_PATTERN = Pattern.compile("^(RO)?\\d{2,10}$");

    CompanyRepository companyRepository;
    ConsultancyRepository consultancyRepository;
    SubscriptionRepository subscriptionRepository;
    WasteCodeRepository wasteCodeRepository;
    AuthenticationService authenticationService;

    @Transactional(readOnly = true)
    public List<CompanyResponse> listAll() {
        AppUser me = SecurityUtils.currentUser();
        List<Company> companies = me.getRole() == Role.CONSULTANT
                ? companyRepository.findAllByConsultancy_Id(consultancyIdOf(me))
                : companyRepository.findAll();
        return companies.stream()
                .sorted(Comparator.comparing(Company::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    /** The tenant the request is scoped to. Readable by any member, unlike the rest here. */
    @Transactional(readOnly = true)
    public CompanyResponse current() {
        UUID tenantId = TenantContext.require();
        return companyRepository.findById(tenantId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
    }

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        AppUser me = SecurityUtils.currentUser();
        String cui = normalizeCui(request.cui());
        if (companyRepository.existsByCui(cui)) {
            throw cuiTaken(me);
        }
        Company company = Company.builder()
                .name(request.name().trim())
                .cui(cui)
                .type(request.type())
                .afmObligation(request.afmObligation())
                // A consultant's new company joins their consultancy — the only way they could
                // ever select it afterwards. The platform admin creates direct clients, and
                // assigns a consultancy separately.
                .consultancy(me.getRole() == Role.CONSULTANT
                        ? consultancyRepository.getReferenceById(consultancyIdOf(me))
                        : null)
                .active(true)
                .createdAt(Instant.now())
                .build();
        applyEditableFields(company, request);
        companyRepository.save(company);
        return toResponse(company);
    }

    @Transactional
    public CompanyResponse update(UUID id, CompanyRequest request) {
        AppUser me = SecurityUtils.currentUser();
        Company company = requireManaged(id, me);
        String cui = normalizeCui(request.cui());
        if (!cui.equals(company.getCui()) && companyRepository.existsByCui(cui)) {
            throw cuiTaken(me);
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
        Company company = requireManaged(companyId, SecurityUtils.currentUser());
        AppUser user = authenticationService.inviteUser(
                company, request.email(), request.role(), request.firstName(), request.lastName());
        return CompanyUserResponse.from(user);
    }

    /**
     * P2.13 — hand a company to a consultancy, move it to another, or ({@code null}) take it back to
     * a direct client. Platform admin only: it decides who can read the company's records, so it is
     * never something a consultancy does to itself.
     */
    @Transactional
    public CompanyResponse assignConsultancy(UUID companyId, UUID consultancyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        Consultancy consultancy = consultancyId == null ? null
                : consultancyRepository.findById(consultancyId)
                        .orElseThrow(() -> new NotFoundException(CONSULTANCY_NOT_FOUND));
        // A consultancy pays for its companies; a company that also pays for itself would be billed twice.
        if (consultancy != null && subscriptionRepository.existsByCompany_Id(companyId)) {
            throw new UnprocessableEntityException(COMPANY_HAS_OWN_SUBSCRIPTION);
        }
        company.setConsultancy(consultancy);
        return toResponse(company);
    }

    /** A company the caller may manage: any for the platform admin, their portfolio for a consultant. */
    private Company requireManaged(UUID id, AppUser me) {
        return (me.getRole() == Role.CONSULTANT
                ? companyRepository.findByIdAndConsultancy_Id(id, consultancyIdOf(me))
                : companyRepository.findById(id))
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
    }

    /**
     * The platform admin is told plainly. A consultant is told something that stays true whoever
     * holds the CUI: the company may be another consultancy's client, and naming that would be the
     * one leak between consultancies this endpoint could still make.
     */
    private UnprocessableEntityException cuiTaken(AppUser me) {
        return new UnprocessableEntityException(
                me.getRole() == Role.CONSULTANT ? COMPANY_CUI_UNAVAILABLE : COMPANY_CUI_ALREADY_EXISTS);
    }

    private static UUID consultancyIdOf(AppUser consultant) {
        // V40 guarantees a consultant has one; failing closed if the guarantee ever breaks.
        if (consultant.getConsultancy() == null) {
            throw new NotFoundException(CONSULTANCY_NOT_FOUND);
        }
        return consultant.getConsultancy().getId();
    }

    private void applyEditableFields(Company company, CompanyRequest request) {
        company.setEnvironmentalAuthNumber(blankToNull(request.environmentalAuthNumber()));
        company.setEnvironmentalAuthExpiry(request.environmentalAuthExpiry());
        company.setAddress(blankToNull(request.address()));
        company.setContactName(blankToNull(request.contactName()));
        company.setContactEmail(blankToNull(request.contactEmail()));
        company.setContactPhone(blankToNull(request.contactPhone()));
        applyProfile(company, request);
    }

    /**
     * The answers from the intake form. Null is left alone rather than treated as "clear": an
     * older client of this API that does not know about the profile must not wipe it by omission.
     * An explicitly empty set does clear it — that is someone choosing "no restriction".
     */
    private void applyProfile(Company company, CompanyRequest request) {
        if (request.authorizedOperationCodes() != null) {
            company.setAuthorizedOperationCodes(new LinkedHashSet<>(request.authorizedOperationCodes()));
        }
        if (request.marketRoles() != null) {
            company.setMarketRoles(new LinkedHashSet<>(request.marketRoles()));
        }
        if (request.afmContributions() != null) {
            company.setAfmContributions(new LinkedHashSet<>(request.afmContributions()));
        }
        if (request.authorizedWasteCodeIds() != null) {
            Set<WasteCode> codes = new LinkedHashSet<>(
                    wasteCodeRepository.findAllById(request.authorizedWasteCodeIds()));
            company.setAuthorizedWasteCodes(codes);
        }
        company.setCaenCode(blankToNull(request.caenCode()));
        // null stays null on purpose: "not answered" means the Anexa 3 keeps printing the unit
        // the movement was recorded in.
        company.setAnexa3Unit(request.anexa3Unit());
        // Scalar, so null clears it — same contract as anexa3Unit above, and the same
        // trap: a partial PUT wipes it. The profile screen sends the whole object.
        company.setPackagingOperatorRole(request.packagingOperatorRole());
        company.setContactRole(blankToNull(request.contactRole()));
        company.setWasteManagerName(blankToNull(request.wasteManagerName()));
        company.setWasteManagerRole(blankToNull(request.wasteManagerRole()));
        // Boolean, not boolean: null means the question was never put, and that is different from
        // "an employee, not a third party". Only the client may turn one into the other.
        company.setWasteManagerExternal(request.wasteManagerExternal());
        company.setWasteManagerTraining(blankToNull(request.wasteManagerTraining()));
        // Same three-state contract as wasteManagerExternal: null is "nobody asked", and it is not
        // the same as a "no". The 30 April deadline reads only the TRUE.
        company.setConstructionPermitHolder(request.constructionPermitHolder());
        company.setTradeRegisterNumber(blankToNull(request.tradeRegisterNumber()));
        company.setAnexa3Series(blankToNull(request.anexa3Series()));
        company.setTransportMeans(blankToNull(request.transportMeans()));
        company.setTransportLicenseNumber(blankToNull(request.transportLicenseNumber()));
        company.setTransportLicenseExpiry(request.transportLicenseExpiry());
    }

    /**
     * Normalizes a CUI to upper-case, no spaces, and validates its shape. Package-visible: a
     * consultancy's CUI is the same kind of code ({@code ConsultancyService}).
     */
    static String normalizeCui(String raw) {
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
        Consultancy consultancy = c.getConsultancy();
        return new CompanyResponse(
                c.getId(), c.getName(), c.getCui(), c.getType(), c.isActive(), c.isAfmObligation(),
                c.getEnvironmentalAuthNumber(), c.getEnvironmentalAuthExpiry(), c.getAddress(),
                c.getContactName(), c.getContactEmail(), c.getContactPhone(),
                new LinkedHashSet<>(c.getAuthorizedOperationCodes()),
                new LinkedHashSet<>(c.getMarketRoles()),
                new LinkedHashSet<>(c.getAfmContributions()),
                c.getAuthorizedWasteCodes().stream()
                        .sorted(Comparator.comparing(WasteCode::getCode))
                        .map(w -> new WasteCodeResponse(w.getId(), w.getCode(), w.getName(), w.isHazardous(),
                                ro.ecoregistru.util.MetalWasteCodes.suggests(w.getCode())))
                        .toList(),
                c.getTransportMeans(), c.getTransportLicenseNumber(), c.getTransportLicenseExpiry(),
                c.getTradeRegisterNumber(), c.getAnexa3Series(),
                c.getPackagingOperatorRole(),
                c.getCaenCode(), c.getAnexa3Unit(), c.getContactRole(),
                c.getWasteManagerName(), c.getWasteManagerRole(),
                c.getWasteManagerExternal(), c.getWasteManagerTraining(),
                c.getConstructionPermitHolder(),
                consultancy == null ? null : consultancy.getId(),
                consultancy == null ? null : consultancy.getName());
    }
}
