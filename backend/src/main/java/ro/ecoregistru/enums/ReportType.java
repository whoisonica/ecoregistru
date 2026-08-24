package ro.ecoregistru.enums;

public enum ReportType {

    /**
     * The yearly filing due on <b>15 March</b> for the previous calendar year: the waste-management
     * evidence itself — the Anexa 1 sheets of HG 856/2002 — uploaded into the system APM provides.
     *
     * <p>The name is historical and stays for the stored rows; the label the client reads names the
     * document rather than the portal ("Anexa 1 — evidenţa gestiunii deşeurilor generate"), because
     * the portal is where it goes, not what has to be prepared. One date, one filing, one deadline:
     * a second entry for "Anexa 1" would put two reminders on the same day for the same act.
     *
     * <p>15 March is a legal term, not an ANPM custom — OUG 92/2021 art. 48 alin. (1) writes it.
     */
    SIM_ANNUAL,

    /**
     * The monthly Environment Fund filing, due on the 25th for the previous month. Produced by
     * {@link ro.ecoregistru.enums.AfmContribution#WITHHOLDING_2_PERCENT} — the 2% a collector
     * withholds at source — and, for accounts that have not answered which contributions they
     * owe, by the legacy {@code afmObligation} flag.
     */
    AFM_MONTHLY,

    /**
     * The quarterly Environment Fund filing, due on the 25th of the month after the quarter
     * (OUG 196/2005 art. 11 alin. (1^1)): the circular-economy contribution of a landfill.
     */
    AFM_QUARTERLY,

    /**
     * The yearly Environment Fund filing, due <b>25 January</b> for the previous year (art. 11
     * alin. (2)): the packaging contribution. Not 15 March, and not monthly — the eleven wrong
     * reminders a packaging-only client used to get were exactly this deadline mis-cadenced.
     */
    AFM_ANNUAL,

    OTHER
}
