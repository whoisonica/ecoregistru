package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.ConsultancyRequest;
import ro.ecoregistru.controller.request.InviteConsultantRequest;
import ro.ecoregistru.controller.response.CompanyUserResponse;
import ro.ecoregistru.controller.response.ConsultancyResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.exception.UnprocessableEntityException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ConsultancyRepository;
import ro.ecoregistru.repository.VerificationRecordRepository;
import ro.ecoregistru.security.SecurityUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.CANNOT_MANAGE_SELF;
import static ro.ecoregistru.exception.ErrorMessageEnum.CONSULTANCY_CUI_ALREADY_EXISTS;
import static ro.ecoregistru.exception.ErrorMessageEnum.CONSULTANCY_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.USER_ALREADY_DEACTIVATED;
import static ro.ecoregistru.exception.ErrorMessageEnum.USER_NOT_DEACTIVATED;
import static ro.ecoregistru.exception.ErrorMessageEnum.USER_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.USER_NOT_INVITATION;
import static ro.ecoregistru.exception.ErrorMessageEnum.USER_NOT_PENDING;
import static ro.ecoregistru.exception.ErrorMessageEnum.USER_STILL_PENDING;

/**
 * P2.13 — consultancies and their consultants.
 *
 * <p><b>Two halves, two callers.</b> The platform admin creates a consultancy and invites its first
 * consultant. From then on the consultancy runs its own team: every consultant may list, invite,
 * deactivate and reactivate colleagues — all of them equal, no head of office (decided 14.09.2026).
 *
 * <p><b>The team half is scoped the way {@code CompanyUserService} is scoped by tenant:</b> the
 * consultancy is the caller's own, never a path parameter, so a consultant can name nobody outside
 * it, and a colleague id from another consultancy is a 404.
 *
 * <p>The guards are the ones of P1.12 that still mean something here. Yourself — refused, the
 * session would die on the next request. An unused invitation is not deactivated (see {@code V34}).
 * There is no last-admin guard: you cannot switch yourself off, so a consultancy always keeps the
 * person asking.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsultancyService {

    ConsultancyRepository consultancyRepository;
    CompanyRepository companyRepository;
    AppUserRepository appUserRepository;
    VerificationRecordRepository verificationRecordRepository;
    AuthenticationService authenticationService;
    DeviceSessionService deviceSessionService;

    // --- platform admin ---

    @Transactional(readOnly = true)
    public List<ConsultancyResponse> list() {
        return consultancyRepository.findAll().stream()
                .sorted(Comparator.comparing(Consultancy::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ConsultancyResponse create(ConsultancyRequest request) {
        String cui = CompanyService.normalizeCui(request.cui());
        if (consultancyRepository.existsByCuiDigits(ro.ecoregistru.util.Cui.digits(cui))) {
            throw new UnprocessableEntityException(CONSULTANCY_CUI_ALREADY_EXISTS);
        }
        Consultancy consultancy = consultancyRepository.save(Consultancy.builder()
                .name(request.name().trim())
                .cui(cui)
                .createdAt(Instant.now())
                .build());
        return toResponse(consultancy);
    }

    @Transactional
    public CompanyUserResponse inviteConsultant(UUID consultancyId, InviteConsultantRequest request) {
        Consultancy consultancy = consultancyRepository.findById(consultancyId)
                .orElseThrow(() -> new NotFoundException(CONSULTANCY_NOT_FOUND));
        return CompanyUserResponse.invited(authenticationService.inviteConsultant(
                consultancy, request.email(), request.firstName(), request.lastName()));
    }

    // --- the consultancy's own team ---

    @Transactional(readOnly = true)
    public List<CompanyUserResponse> listTeam() {
        return appUserRepository.findAllByConsultancy_Id(myConsultancy().getId()).stream()
                .sorted(Comparator.comparing(AppUser::getEmail, String.CASE_INSENSITIVE_ORDER))
                .map(CompanyUserResponse::from)
                .toList();
    }

    @Transactional
    public CompanyUserResponse inviteColleague(InviteConsultantRequest request) {
        Consultancy consultancy = consultancyRepository.getReferenceById(myConsultancy().getId());
        return CompanyUserResponse.invited(authenticationService.inviteConsultant(
                consultancy, request.email(), request.firstName(), request.lastName()));
    }

    /** Same contract as {@code CompanyUserService.deactivate}: the counter bump keeps old tokens dead. */
    @Transactional
    public void deactivateColleague(UUID id) {
        AppUser colleague = requireColleague(id);
        refuseSelf(colleague);
        if (colleague.getDeactivatedAt() != null) {
            throw new BusinessException(USER_ALREADY_DEACTIVATED);
        }
        if (!colleague.isEnabled()) {
            throw new BusinessException(USER_STILL_PENDING);
        }
        colleague.setEnabled(false);
        colleague.setDeactivatedAt(Instant.now());
        colleague.setTokenVersion(colleague.getTokenVersion() + 1);
        // G1 — și telefoanele. `rotate` le-ar refuza oricum pe contul ăsta, dar o sesiune oprită
        // trebuie să fie oprită și în listă, nu abia când telefonul se întoarce să întrebe.
        deviceSessionService.revokeAllOf(colleague);
        appUserRepository.save(colleague);
    }

    @Transactional
    public void reactivateColleague(UUID id) {
        AppUser colleague = requireColleague(id);
        if (colleague.getDeactivatedAt() == null) {
            throw new BusinessException(USER_NOT_DEACTIVATED);
        }
        colleague.setEnabled(true);
        colleague.setDeactivatedAt(null);
        appUserRepository.save(colleague);
    }

    /**
     * P2.13, felia 2 — invitația trimisă din nou, ca la utilizatorii firmei ({@code CompanyUserService.resendInvite}):
     * doar unui coleg care n-a intrat niciodată. Unul cu parolă are „Parolă uitată".
     */
    @Transactional
    public void resendColleagueInvite(UUID id) {
        AppUser colleague = requireColleague(id);
        if (colleague.isEnabled() || colleague.getDeactivatedAt() != null) {
            throw new BusinessException(USER_NOT_PENDING);
        }
        authenticationService.resendInvite(colleague);
    }

    /**
     * P2.13, felia 2 — invitația nefolosită se anulează și rândul se șterge, ca adresa greșită să se
     * elibereze. Sigur din același motiv ca la firmă ({@code CompanyUserService.cancelInvite}): un cont
     * în care nu s-a intrat nu e autorul a nimic.
     */
    @Transactional
    public void cancelColleagueInvite(UUID id) {
        AppUser colleague = requireColleague(id);
        refuseSelf(colleague);
        if (colleague.isEnabled() || colleague.getDeactivatedAt() != null) {
            throw new BusinessException(USER_NOT_INVITATION);
        }
        verificationRecordRepository.deleteByUser(colleague);
        appUserRepository.delete(colleague);
    }

    // --- guards ---

    private Consultancy myConsultancy() {
        Consultancy consultancy = SecurityUtils.currentUser().getConsultancy();
        if (consultancy == null) {
            throw new NotFoundException(CONSULTANCY_NOT_FOUND);
        }
        return consultancy;
    }

    private AppUser requireColleague(UUID id) {
        return appUserRepository.findByIdAndConsultancy_Id(id, myConsultancy().getId())
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
    }

    private void refuseSelf(AppUser user) {
        if (user.getId().equals(SecurityUtils.currentUser().getId())) {
            throw new BusinessException(CANNOT_MANAGE_SELF);
        }
    }

    private ConsultancyResponse toResponse(Consultancy c) {
        return new ConsultancyResponse(c.getId(), c.getName(), c.getCui(),
                companyRepository.countByConsultancy_Id(c.getId()),
                appUserRepository.countByConsultancy_Id(c.getId()),
                c.getCreatedAt());
    }
}
