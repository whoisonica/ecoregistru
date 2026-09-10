package ro.ecoregistru.enums;

/**
 * P1.12 — what a member of a tenant is, read off the pair ({@code enabled}, {@code deactivatedAt}).
 *
 * <p>Derived, never stored: two columns already say it, and a third would be a third thing that
 * can disagree with them. The enum exists so the screen does not have to re-derive it, and so the
 * answer is the same everywhere.
 *
 * <p>Why the split matters: before {@code V34} the first two were indistinguishable, both being
 * {@code enabled = false}. See the migration.
 */
public enum CompanyUserStatus {

    /** Signed in, or can. */
    ACTIVE,

    /** Invited, never set a password. The one state where resending the invite means something. */
    PENDING_INVITE,

    /** Switched off by an admin. Sessions closed with it (the token counter was bumped). */
    DEACTIVATED
}
