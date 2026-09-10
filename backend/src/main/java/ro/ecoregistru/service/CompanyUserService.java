package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.request.InviteUserRequest;
import ro.ecoregistru.controller.response.CompanyUserResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.VerificationRecordRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.CANNOT_MANAGE_SELF;
import static ro.ecoregistru.exception.ErrorMessageEnum.INVALID_INVITE_ROLE;
import static ro.ecoregistru.exception.ErrorMessageEnum.LAST_ADMIN;
import static ro.ecoregistru.exception.ErrorMessageEnum.USER_ALREADY_DEACTIVATED;
import static ro.ecoregistru.exception.ErrorMessageEnum.USER_NOT_DEACTIVATED;
import static ro.ecoregistru.exception.ErrorMessageEnum.USER_NOT_FOUND;
import static ro.ecoregistru.exception.ErrorMessageEnum.USER_NOT_INVITATION;
import static ro.ecoregistru.exception.ErrorMessageEnum.USER_NOT_PENDING;
import static ro.ecoregistru.exception.ErrorMessageEnum.USER_STILL_PENDING;

/**
 * P1.12 — a client firm administers its own users.
 *
 * <p><b>Why this is not on {@code CompanyService}.</b> That one is platform-only and global by
 * design: the platform admin picks a company by id and acts on it. This one is <b>tenant-scoped</b>
 * like every other domain service — the company is the one the session is on, never a path
 * parameter. That is the whole point: an {@code ADMIN} of a firm can run their own users without
 * being able to name anybody else's. The platform admin reaches the same screen through the tenant
 * switcher, so there is one implementation, not two.
 *
 * <p><b>What a firm could do to itself, and does not.</b> Three guards, all of which a real client
 * hits in their first week:
 * <ul>
 *   <li><b>Yourself.</b> Deactivating or demoting your own account is refused. The old session dies
 *       on the next request, so the mistake is not undoable from inside the application — somebody
 *       would have to call us.</li>
 *   <li><b>The last admin.</b> A firm whose only enabled {@code ADMIN} is switched off or demoted
 *       has no way back either: nothing left in it can invite, promote or reactivate. Counted, not
 *       hoped for.</li>
 *   <li><b>{@code PLATFORM_ADMIN}.</b> Neither reachable nor grantable. Not reachable because that
 *       account's company is null and every lookup here is scoped by company — structural, not a
 *       check that can be forgotten. Not grantable because the role is refused on the way in, the
 *       same rule the invite has had from the start.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompanyUserService {

    AppUserRepository appUserRepository;
    CompanyRepository companyRepository;
    VerificationRecordRepository verificationRecordRepository;
    AuthenticationService authenticationService;

    /**
     * The tenant's members: active, invited and switched-off alike.
     *
     * <p>Deactivated rows stay on the list on purpose. They are the answer to "who used to have
     * access", they are what the reactivate button acts on, and hiding them would make the screen
     * look like the account was deleted when it was not.
     */
    @Transactional(readOnly = true)
    public List<CompanyUserResponse> list() {
        UUID tenantId = TenantContext.require();
        return appUserRepository.findAllByCompany_Id(tenantId).stream()
                .sorted(Comparator.comparing(AppUser::getEmail, String.CASE_INSENSITIVE_ORDER))
                .map(CompanyUserResponse::from)
                .toList();
    }

    /**
     * Invite somebody onto the tenant the session is on.
     *
     * <p>Delegates to {@code AuthenticationService.inviteUser}, which is where the account, the
     * unusable random password and the reset-link mail all come from — one invite mechanism, the
     * same one the platform admin has been using.
     */
    @Transactional
    public CompanyUserResponse invite(InviteUserRequest request) {
        UUID tenantId = TenantContext.require();
        Company company = companyRepository.getReferenceById(tenantId);
        AppUser user = authenticationService.inviteUser(
                company, request.email(), request.role(), request.firstName(), request.lastName());
        return CompanyUserResponse.from(user);
    }

    /**
     * Send the invitation again — the single most common support request there is, because the
     * first mail lands in spam or the 30-minute link expires while somebody is at lunch.
     *
     * <p>Only to a user who has never been in. An account that already has a password does not
     * need an invite, it needs "Parolă uitată", which is theirs to use and does not go through an
     * administrator. Sending one anyway would be an admin able to mint a password-reset link for a
     * colleague's live account, which is a different and worse feature than the one asked for.
     */
    @Transactional
    public void resendInvite(UUID id) {
        AppUser user = require(id);
        if (user.isEnabled() || user.getDeactivatedAt() != null) {
            throw new BusinessException(USER_NOT_PENDING);
        }
        authenticationService.resendInvite(user);
    }

    /**
     * Switch an account off.
     *
     * <p>{@code enabled = false} is what closes the open sessions — {@code JwtService} checks it on
     * every request since P0.4, so the button does not lie about "access removed now".
     *
     * <p><b>The counter bump is about the way back, not the way out.</b> Tokens live 30 days. A
     * user switched off on Monday and back on on Wednesday would otherwise find every token issued
     * before Monday working again — including the one on the laptop that was the reason for
     * switching them off. Bumping the counter here means reactivation gives back the account, not
     * the old sessions.
     */
    @Transactional
    public void deactivate(UUID id) {
        AppUser user = require(id);
        refuseSelf(user);
        if (user.getDeactivatedAt() != null) {
            throw new BusinessException(USER_ALREADY_DEACTIVATED);
        }
        // An invitation is not access, so there is nothing here to take away — and letting this
        // through would build the one state V34 exists to prevent: deactivating a pending invite
        // and then reactivating it would set enabled = true over the random password from
        // inviteUser, leaving a row that reads Active and can never sign in. Cancel it instead.
        if (!user.isEnabled()) {
            throw new BusinessException(USER_STILL_PENDING);
        }
        refuseLastAdmin(user);
        user.setEnabled(false);
        user.setDeactivatedAt(Instant.now());
        user.setTokenVersion(user.getTokenVersion() + 1);
        appUserRepository.save(user);
    }

    /**
     * Cancel an invitation that was never used — the mistyped address, the colleague who turned out
     * not to need an account.
     *
     * <p><b>This one really does delete the row</b>, unlike every other "remove" in the
     * application. It is safe here and nowhere else: an account that has never been signed into
     * cannot be the {@code createdBy} of a movement, an evidence sheet or an attachment, so there
     * is no trail to cut. What it buys is the address going free again, which is the whole point
     * after a typo — {@code inviteUser} refuses an email that already exists.
     *
     * <p>The outstanding reset links go with it. They would be orphaned otherwise, and a link that
     * resolves to a user that is gone is a 500 waiting for whoever opens their mail late.
     */
    @Transactional
    public void cancelInvite(UUID id) {
        AppUser user = require(id);
        refuseSelf(user);
        if (user.isEnabled() || user.getDeactivatedAt() != null) {
            throw new BusinessException(USER_NOT_INVITATION);
        }
        verificationRecordRepository.deleteByUser(user);
        appUserRepository.delete(user);
    }

    /**
     * Undo a deactivation.
     *
     * <p>Safe to hand the account straight back because only an enabled account can be deactivated
     * — see {@code deactivate} — so anybody in this state has been in, and has a password of their
     * own. A pending invite can never get here.
     */
    @Transactional
    public void reactivate(UUID id) {
        AppUser user = require(id);
        if (user.getDeactivatedAt() == null) {
            throw new BusinessException(USER_NOT_DEACTIVATED);
        }
        user.setEnabled(true);
        user.setDeactivatedAt(null);
        appUserRepository.save(user);
    }

    /**
     * Change what somebody may do. Takes effect on their next request — the role travels in the
     * token as a claim, but authorisation reads the user row, so nobody has to sign out and back in.
     */
    @Transactional
    public CompanyUserResponse changeRole(UUID id, Role role) {
        if (role == Role.PLATFORM_ADMIN) {
            throw new BusinessException(INVALID_INVITE_ROLE);
        }
        AppUser user = require(id);
        refuseSelf(user);
        if (user.getRole() != role) {
            // Only a move *away* from ADMIN can leave the firm without one.
            refuseLastAdmin(user);
        }
        user.setRole(role);
        appUserRepository.save(user);
        return CompanyUserResponse.from(user);
    }

    // --- guards ---

    /**
     * The tenant's member with this id, or 404.
     *
     * <p>Scoped by company, so an id belonging to another firm is indistinguishable from one that
     * does not exist — the endpoint never confirms that somebody else's user id is real. It is also
     * why {@code PLATFORM_ADMIN} rows can never be reached from here: their company is null.
     */
    private AppUser require(UUID id) {
        UUID tenantId = TenantContext.require();
        return appUserRepository.findByIdAndCompany_Id(id, tenantId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
    }

    private void refuseSelf(AppUser user) {
        if (user.getId().equals(SecurityUtils.currentUser().getId())) {
            throw new BusinessException(CANNOT_MANAGE_SELF);
        }
    }

    /**
     * Refuses the move if this user is the only enabled {@code ADMIN} the tenant has.
     *
     * <p>Counted rather than assumed, and counted among the <i>enabled</i>: an invited admin who
     * never set a password cannot let anybody back in, so they do not count as cover.
     */
    private void refuseLastAdmin(AppUser user) {
        if (user.getRole() != Role.ADMIN || !user.isEnabled()) {
            return;
        }
        long enabledAdmins = appUserRepository.countByCompany_IdAndRoleAndEnabledTrue(
                TenantContext.require(), Role.ADMIN);
        if (enabledAdmins <= 1) {
            throw new BusinessException(LAST_ADMIN);
        }
    }
}
