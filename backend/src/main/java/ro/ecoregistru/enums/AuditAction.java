package ro.ecoregistru.enums;

/**
 * Ce s-a întâmplat cu un rând — verbe, nu diferenţe de câmpuri.
 *
 * <p>Trei dintre ele ies dintr-un UPDATE în baza de date şi totuşi nu sunt „modificare":
 * ştergerea e moale peste tot în aplicaţia asta (mişcările vechi citează rândul, deci nimic nu
 * dispare), iar dezactivarea e chiar fapta pe care cineva o caută în jurnal. Scrise ca
 * {@code deleted: false → true}, ar fi fost adevărate şi de negăsit.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    DEACTIVATE,
    REACTIVATE,
    /** Recalcularea evidenţei lunare — o faptă asupra unui an întreg, nu asupra unui rând. */
    REGENERATE
}
