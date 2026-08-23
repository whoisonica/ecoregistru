package ro.ecoregistru.enums;

import java.util.EnumSet;
import java.util.Set;

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

    /**
     * The movement operations this kind of company may record — what the screen offers and what
     * the service accepts.
     *
     * <p>The only member that varies is {@link WasteOperation#COLLECTED}, and it varies for the
     * same reason the art. 48 register does: a company that does not take waste over from third
     * parties has nothing to record there. {@link WasteOperation#GENERATED} is offered to every
     * type on purpose — art. 2 alin. (1) obliges an authorised collector to keep Anexa 1 too, for
     * the waste of its own activity, a sorting station's own reject included — so a COLLECTOR is
     * never stripped of it. {@link WasteOperation#UNCLASSIFIED_OUT} is in no set: it is written by
     * a migration, never chosen.
     */
    public Set<WasteOperation> allowedOperations() {
        return keepsArt48Register()
                ? EnumSet.of(WasteOperation.GENERATED, WasteOperation.COLLECTED,
                             WasteOperation.RECOVERED, WasteOperation.DISPOSED)
                : EnumSet.of(WasteOperation.GENERATED,
                             WasteOperation.RECOVERED, WasteOperation.DISPOSED);
    }
}
