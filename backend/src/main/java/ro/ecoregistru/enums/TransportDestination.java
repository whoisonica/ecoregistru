package ro.ecoregistru.enums;

import lombok.Getter;

/**
 * The "Destinat:" box of Anexa 3 la HG 1061/2008 — what the waste is being taken away for.
 *
 * <p>More than one may be ticked. The filled model received from the specialist (series HMB 180)
 * has an X on both <em>Colectării</em> and <em>Valorificării</em>: the collector picks the waste up
 * and it ends up recycled, and the form says both. That is why this is a set on the movement and
 * not a single choice.
 *
 * <p>It is asked rather than derived, even though the movement already carries an R/D code. The
 * code says what finally happens to the waste; this box says what this particular transport is
 * for. Handing cardboard to a collector who stores it (R13) and then sells it on is "colectării"
 * on the form and R13 in Anexa 1 — two different questions with two different answers.
 */
@Getter
public enum TransportDestination {

    COLECTARE("Colectării"),
    STOCARE_TEMPORARA("Stocării temporare"),
    TRATARE("Tratării"),
    VALORIFICARE("Valorificării"),
    ELIMINARE("Eliminării");

    /** The wording printed on the form, verbatim. */
    private final String officialLabel;

    TransportDestination(String officialLabel) {
        this.officialLabel = officialLabel;
    }
}
