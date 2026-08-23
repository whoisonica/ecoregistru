package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.AccountRequestSubmission;
import ro.ecoregistru.controller.request.CompanyRequest;
import ro.ecoregistru.controller.request.RejectAccountRequest;
import ro.ecoregistru.controller.response.AccountRequestResponse;
import ro.ecoregistru.controller.response.CompanyResponse;
import ro.ecoregistru.entity.AccountRequest;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.AccountRequestStatus;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AccountRequestRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.SecurityUtils;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.ACCOUNT_REQUEST_ALREADY_HANDLED;
import static ro.ecoregistru.exception.ErrorMessageEnum.ACCOUNT_REQUEST_NOT_FOUND;

/**
 * The intake form, and what support does with it.
 *
 * <p>Submitting is public and creates nothing but a request: no user, no company, no token. That is
 * what keeps the register closed while still giving a client a way in — the alternative, a
 * self-registration endpoint left disabled by a flag, is one configuration change away from open.
 *
 * <p>Approving copies the answers onto a real company, profile included, so the new account opens
 * with its screens already narrowed to what it said it does. Inviting the user stays a separate
 * act: creating an account and giving someone access are two decisions, not one.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountRequestService {

    AccountRequestRepository accountRequestRepository;
    CompanyRepository companyRepository;
    WorkPointRepository workPointRepository;
    CompanyService companyService;

    /** Public. Returns nothing about what it wrote, so it cannot be used to probe for companies. */
    @Transactional
    public void submit(AccountRequestSubmission submission) {
        AccountRequest request = AccountRequest.builder()
                .companyName(submission.companyName().trim())
                .cui(normalizeCui(submission.cui()))
                .companyType(submission.companyType())
                .companyAddress(blankToNull(submission.companyAddress()))
                .workPointName(blankToNull(submission.workPointName()))
                .workPointAddress(blankToNull(submission.workPointAddress()))
                .contactName(blankToNull(submission.contactName()))
                .contactEmail(submission.contactEmail().trim().toLowerCase())
                .contactPhone(blankToNull(submission.contactPhone()))
                .environmentalAuthNumber(blankToNull(submission.environmentalAuthNumber()))
                .environmentalAuthExpiry(submission.environmentalAuthExpiry())
                .transportMeans(blankToNull(submission.transportMeans()))
                .transportLicenseNumber(blankToNull(submission.transportLicenseNumber()))
                .transportLicenseExpiry(submission.transportLicenseExpiry())
                .marketRoles(submission.marketRoles() == null
                        ? new LinkedHashSet<>()
                        : new LinkedHashSet<>(submission.marketRoles()))
                .operationCodes(submission.operationCodes() == null
                        ? new LinkedHashSet<>()
                        : new LinkedHashSet<>(submission.operationCodes()))
                .wasteCodesText(blankToNull(submission.wasteCodesText()))
                .notes(blankToNull(submission.notes()))
                .status(AccountRequestStatus.NEW)
                .createdAt(Instant.now())
                .build();
        accountRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<AccountRequestResponse> list(AccountRequestStatus status) {
        List<AccountRequest> found = status == null
                ? accountRequestRepository.findAllByOrderByCreatedAtDesc()
                : accountRequestRepository.findAllByStatusOrderByCreatedAtDesc(status);
        return found.stream().map(this::toResponse).toList();
    }

    /**
     * Turns the request into a company, carrying the profile across. The work point is created
     * here too when the form named one: it is the address the legal records are actually kept for,
     * and asking for it twice is how it ends up different in the two places.
     */
    @Transactional
    public CompanyResponse approve(UUID id) {
        AccountRequest request = require(id);
        requirePending(request);

        CompanyResponse created = companyService.create(new CompanyRequest(
                request.getCompanyName(),
                request.getCui(),
                request.getCompanyType(),
                false, // the AFM obligation is a determination, not a form answer
                request.getEnvironmentalAuthNumber(),
                request.getEnvironmentalAuthExpiry(),
                request.getCompanyAddress(),
                request.getContactName(),
                request.getContactEmail(),
                request.getContactPhone(),
                request.getOperationCodes(),
                request.getMarketRoles(), // producător / importator / comerciant, as answered
                Set.of(), // the free-text waste codes are mapped by hand; nothing is guessed here
                request.getTransportMeans(),
                request.getTransportLicenseNumber(),
                request.getTransportLicenseExpiry(),
                null,   // trade register number: not asked on the intake form
                null)); // Anexa 3 series: set later, when the client has a form pad

        Company company = companyRepository.getReferenceById(created.id());
        if (request.getWorkPointName() != null || request.getWorkPointAddress() != null) {
            workPointRepository.save(WorkPoint.builder()
                    .company(company)
                    .name(request.getWorkPointName() != null
                            ? request.getWorkPointName()
                            : request.getCompanyName())
                    .address(request.getWorkPointAddress())
                    .active(true)
                    .createdAt(Instant.now())
                    .build());
        }

        request.setStatus(AccountRequestStatus.APPROVED);
        request.setCreatedCompany(company);
        stamp(request);
        return created;
    }

    @Transactional
    public void reject(UUID id, RejectAccountRequest body) {
        AccountRequest request = require(id);
        requirePending(request);
        request.setStatus(AccountRequestStatus.REJECTED);
        String reason = body == null ? null : blankToNull(body.reason());
        if (reason != null) {
            String existing = request.getNotes() == null ? "" : request.getNotes() + "\n";
            request.setNotes(existing + "Respins: " + reason);
        }
        stamp(request);
    }

    private void stamp(AccountRequest request) {
        request.setHandledBy(SecurityUtils.currentUser().getId());
        request.setHandledAt(Instant.now());
    }

    private void requirePending(AccountRequest request) {
        if (request.getStatus() != AccountRequestStatus.NEW) {
            throw new BusinessException(ACCOUNT_REQUEST_ALREADY_HANDLED);
        }
    }

    private AccountRequest require(UUID id) {
        return accountRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ACCOUNT_REQUEST_NOT_FOUND));
    }

    private AccountRequestResponse toResponse(AccountRequest r) {
        return new AccountRequestResponse(
                r.getId(), r.getCompanyName(), r.getCui(), r.getCompanyType(),
                r.getCompanyAddress(), r.getWorkPointName(), r.getWorkPointAddress(),
                r.getContactName(), r.getContactEmail(), r.getContactPhone(),
                r.getEnvironmentalAuthNumber(), r.getEnvironmentalAuthExpiry(),
                r.getTransportMeans(), r.getTransportLicenseNumber(), r.getTransportLicenseExpiry(),
                new LinkedHashSet<>(r.getMarketRoles()),
                new LinkedHashSet<>(r.getOperationCodes()), r.getWasteCodesText(), r.getNotes(),
                r.getStatus(),
                r.getCreatedCompany() != null ? r.getCreatedCompany().getId() : null,
                r.getHandledAt(), r.getCreatedAt());
    }

    /** Same shape CompanyService validates; normalized here so the request stores what it sent. */
    private String normalizeCui(String raw) {
        return raw.replaceAll("\\s", "").toUpperCase();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
