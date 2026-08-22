package ro.ecoregistru.enums;

/**
 * What the company does with waste — and therefore which registers and modules apply to it.
 *
 * <p>Anexa 1 applies to every type, so there is no accessor for it: art. 1 alin. (1) HG 856/2002
 * binds anyone who generates waste, and art. 2 alin. (1) confirms an authorised operator keeps it
 * too, for the waste generated in its own activity (a sorting station's own reject included).
 * The asymmetry is on the other side — only an operator that takes waste over from third parties
 * keeps the art. 48 chronological register. See docs/surse-oficiale.md §1.1 and §2.1.
 */
public enum CompanyType {

    /** Generates its own waste only. Anexa 1, nothing else. */
    GENERATOR,

    /** Takes waste over from third parties: collection centre, sorting/treatment station, landfill. */
    COLLECTOR,

    BOTH;

    /**
     * Whether the art. 48 chronological register — and the depot module built on top of it —
     * applies to this company.
     */
    public boolean keepsArt48Register() {
        return this != GENERATOR;
    }
}
