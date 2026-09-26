package ro.ecoregistru.controller.response;

import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.enums.CompanyUserStatus;
import ro.ecoregistru.enums.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A user belonging to a tenant company. Returned by the users screen (P1.12) and by the
 * platform-admin invite.
 *
 * <p>{@code enabled} is kept alongside {@code status} even though the second implies the first:
 * it is what every client of this API read before P1.12, and dropping it would be a breaking
 * change for no gain. New screens read {@code status}, which is the one that can tell an invited
 * user from a switched-off one.
 *
 * <p>No password field, ever — not even a hashed one. The row goes to the browser.
 */
public record CompanyUserResponse(
        UUID id,
        String email,
        Role role,
        String firstName,
        String lastName,
        boolean enabled,
        CompanyUserStatus status,
        Instant createdAt,
        Instant deactivatedAt,
        /** BUG-038 — only on the answer to an invite: false when the mail did not go out. */
        Boolean inviteEmailSent,
        /** D2.4 — true: every depot, including later ones. False: only {@link #workPointIds}. */
        boolean allWorkPoints,
        List<UUID> workPointIds
) {

    /** The one place the three states are read off the two columns. */
    public static CompanyUserResponse from(AppUser user) {
        return from(user, List.of());
    }

    /** With the depots of a restricted user (D2.4); the list is empty when {@code allWorkPoints}. */
    public static CompanyUserResponse from(AppUser user, List<UUID> workPointIds) {
        CompanyUserStatus status;
        if (user.isEnabled()) {
            status = CompanyUserStatus.ACTIVE;
        } else if (user.getDeactivatedAt() != null) {
            status = CompanyUserStatus.DEACTIVATED;
        } else {
            status = CompanyUserStatus.PENDING_INVITE;
        }
        return new CompanyUserResponse(
                user.getId(), user.getEmail(), user.getRole(),
                user.getFirstName(), user.getLastName(), user.isEnabled(),
                status, user.getCreatedAt(), user.getDeactivatedAt(), null,
                user.isAllWorkPoints(), user.isAllWorkPoints() ? List.of() : List.copyOf(workPointIds));
    }

    /** The answer to an invite: the same row, plus whether the mail went out. */
    public static CompanyUserResponse invited(AppUser user) {
        CompanyUserResponse r = from(user);
        return new CompanyUserResponse(r.id, r.email, r.role, r.firstName, r.lastName, r.enabled,
                r.status, r.createdAt, r.deactivatedAt, !user.isInviteEmailFailed(), r.allWorkPoints, r.workPointIds);
    }
}
