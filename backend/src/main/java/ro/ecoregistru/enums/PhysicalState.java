package ro.ecoregistru.enums;

/**
 * Physical state of the waste, an identification field on the HG 856/2002 Anexa 1 sheet.
 * Constant names are English (code convention); Romanian labels live in the frontend.
 * NOTE: the exact value set is still to be confirmed with the environmental expert
 * (see docs/legislatie.md §4 Q6) — treat these as a working set, not a final list.
 */
public enum PhysicalState {
    SOLID,
    LIQUID,
    SLUDGE,
    PASTY,
    POWDER,
    GASEOUS
}
