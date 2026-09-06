package ro.ecoregistru.enums;

import lombok.Getter;

/**
 * Where packaging waste taken over from a third party came from — column "Provenienţa" of both
 * tables of Anexa 3 la Ordinul 794/2012.
 *
 * <p><b>Verbatim from nota 2</b>, identical under tabelul 1 and tabelul 2: <i>"Se menţionează, după
 * caz, «populaţie», «generator persoană juridică», «colector», «comerciant», în funcţie de
 * persoanele juridice sau fizice de la care provin deşeurile de ambalaje preluate."</i> Four
 * values, not three — the {@code .ods} template received from the specialist has only the first
 * three, and it is a locally modified copy, the same way its "tone" heading contradicts art. 8
 * alin. (1) lit. a). Where the model and the primary source disagree on a question of law, the
 * source wins (regula de lucru 2).
 *
 * <p><b>Why it is a property of the partner, and only then of the movement.</b> The note says "de
 * la care provin" — it describes the <em>source</em>, not the transport. A collector one buys from
 * is a collector on every load they bring, so answering it once on the partner is answering it for
 * good, and typing it per movement would be typing the same fact a hundred times with a hundred
 * chances to differ. {@code POPULATIE} is the exception that makes the per-movement override
 * necessary rather than merely convenient: a natural person is not a partner and never will be
 * one, so without the override that row of the form could never be filled at all.
 *
 * <p>This closes the half of question <b>AA</b> that was ours to decide. The other half — that
 * there are four values — was closed by the act itself on 02.09.2026.
 */
@Getter
public enum PackagingOrigin {

    /** Natural persons. Has no partner by construction — chosen on the movement itself. */
    POPULATIE("populaţie"),

    /** A legal person that generated the packaging waste in its own activity. */
    GENERATOR_PJ("generator persoană juridică"),

    /** Another collector, passing on what it had taken over. */
    COLECTOR("colector"),

    /** A trader in packaging waste. Missing from the template we were given; present in the act. */
    COMERCIANT("comerciant");

    /** Exactly the word the form prints, lower case as nota 2 writes it. */
    private final String officialLabel;

    PackagingOrigin(String officialLabel) {
        this.officialLabel = officialLabel;
    }

    /**
     * The provenance to print for a takeover: what the movement says, and failing that what the
     * partner says.
     *
     * <p>Empty means nobody has answered. The quantity then stays <b>off</b> the table and is
     * listed as unclassified, the same treatment {@link PackagingMaterial} gives a movement whose
     * material nobody chose. A form filed with an authority does not get a guessed rubric
     * (regula de lucru 1).
     */
    public static java.util.Optional<PackagingOrigin> resolve(PackagingOrigin onMovement,
                                                              PackagingOrigin onPartner) {
        return java.util.Optional.ofNullable(onMovement != null ? onMovement : onPartner);
    }
}
