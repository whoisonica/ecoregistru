package ro.ecoregistru.enums;

import lombok.Getter;

/**
 * A contribution owed to the Environment Fund, with the rhythm it is declared in.
 *
 * <p><b>Why a set and not a boolean.</b> {@code Company.afmObligation} said only "something is
 * owed", and the application turned that into one monthly deadline for everyone who had it. But
 * OUG 196/2005 art. 11 has three rhythms, so a company that owes only the yearly packaging
 * contribution was getting <b>eleven wrong reminders a year</b>. That was the oldest piece of
 * wrong output in the application.
 *
 * <p>The three below are the ones this product can actually meet a client with. Which of them a
 * company owes is answered, never guessed: an unanswered company keeps the legacy flag and the
 * noisy monthly deadline it always had, because switching an alert off on an assumption is worse
 * than leaving one that is too loud.
 *
 * <p>Confirmed with the specialist on 24.08.2026 (question L): the 2% is owed on the sale of
 * <em>any</em> waste, not only packaging — "2% pe orice deşeu, păstrăm alerta" — and it is the
 * collector who withholds it at source and pays it over.
 */
@Getter
public enum AfmContribution {

    /**
     * Art. 9 alin. (1) lit. a): 2% of the revenue from selling waste, <b>withheld at source by the
     * operator who collects or recovers it</b>. Declared monthly, by the 25th (art. 11 alin. (1)).
     *
     * <p>So this belongs to a collection centre, not to the corner shop that sold the cardboard:
     * the shop owes it, the collector withholds and pays it, and the shop sees it on the invoice.
     */
    WITHHOLDING_2_PERCENT(AfmCadence.MONTHLY),

    /**
     * Art. 9 alin. (1) lit. c): the circular-economy contribution, owed by the owners or operators
     * of landfills for municipal and construction waste sent to disposal. Declared quarterly, by
     * the 25th of the month after the quarter (art. 11 alin. (1^1)).
     *
     * <p>The rate itself is in anexa nr. 2, which the consolidated text on the legal portal
     * truncates — still missing, and the reason the landfill profile is not built.
     */
    CIRCULAR_ECONOMY(AfmCadence.QUARTERLY),

    /**
     * Art. 9 alin. (1) lit. d): the packaging contribution, owed by whoever puts packaged goods on
     * the national market and misses the recovery targets. Declared annually, by <b>25 January</b>
     * (art. 11 alin. (2)) — not in March, and not monthly.
     *
     * <p>This is the one the specialist meant by "obligaţia AFM, doar producătorii/importatorii".
     * It travels with {@link MarketRole#putsPackagingOnMarket}.
     */
    PACKAGING(AfmCadence.ANNUAL);

    private final AfmCadence cadence;

    AfmContribution(AfmCadence cadence) {
        this.cadence = cadence;
    }

    /** The three rhythms of art. 11, and nothing else. */
    public enum AfmCadence {
        /** By the 25th of the following month. */
        MONTHLY,
        /** By the 25th of the month following the quarter. */
        QUARTERLY,
        /** By 25 January of the following year. */
        ANNUAL
    }
}
