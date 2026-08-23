package ro.ecoregistru.enums;

/**
 * The kind of waste movement recorded.
 *
 * <p>There is deliberately no "handed over" member. HG 856/2002 anexa nr. 1 cap. 1 has four
 * quantity columns — generate · valorificată · eliminată final · rămasă în stoc — and none of them
 * is "predat"; cap. 3 and cap. 4 report a quantity together with the R/D operation performed on it
 * <em>and</em> "agentul economic care efectuează operaţia". Handing waste to a recycler is
 * therefore a {@link #RECOVERED} performed by a partner, and handing it to a landfill a
 * {@link #DISPOSED} performed by a partner — the partner is who did it, not what happened.
 * Keeping a separate HANDED_OVER member meant the same physical exit could be recorded two ways.
 * Verbatim source: docs/surse-oficiale.md §1.2.
 *
 * <p>Which members a company may use follows from {@link CompanyType#allowedOperations()}.
 */
public enum WasteOperation {

    /** Waste produced in the company's own activity. Anexa 1 only (art. 1 alin. (1)). */
    GENERATED,

    /** Waste taken over from a third party. Art. 48 register only (art. 2 alin. (1)). */
    COLLECTED,

    /** An R1–R13 operation, performed by this company or — when a partner is named — by it. */
    RECOVERED,

    /** A D1–D15 operation, performed by this company or — when a partner is named — by it. */
    DISPOSED,

    /**
     * Waste that left the site with no R/D operation recorded. Not offered when recording a
     * movement: it exists only for rows written before the code became mandatory, which cannot be
     * classified retroactively without inventing an operation for an official form. The quantity
     * leaves the stock and enters neither official column, and the line is reported incomplete
     * until someone completes it.
     */
    UNCLASSIFIED_OUT;

    /** Whether this operation takes waste off the site, and therefore needs an R/D code. */
    public boolean isExit() {
        return this == RECOVERED || this == DISPOSED;
    }

    /** Whether an operator may choose this when recording a movement. */
    public boolean isSelectable() {
        return this != UNCLASSIFIED_OUT;
    }
}
