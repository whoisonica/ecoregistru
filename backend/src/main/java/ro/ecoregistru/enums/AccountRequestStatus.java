package ro.ecoregistru.enums;

/**
 * Where an intake request stands. A request is never deleted — it is the paper trail behind an
 * account, and the answer to "why does this client see only these five operation codes?".
 */
public enum AccountRequestStatus {

    /** Submitted, nobody has looked at it yet. */
    NEW,

    /** Turned into a company by a PLATFORM_ADMIN; the company id is on the request. */
    APPROVED,

    /** Declined. Kept, with the reason in the notes. */
    REJECTED
}
