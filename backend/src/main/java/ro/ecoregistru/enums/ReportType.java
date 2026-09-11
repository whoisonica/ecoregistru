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
     * <p>15 March is a legal term, not an ANMAP custom — OUG 92/2021 art. 48 alin. (1) writes it.
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
     *
     * <p>Two obligations fall on this date, not one, and the label names both: besides the
     * contribution of OUG 196/2005, art. 3 of Ordinul 794/2012 puts the <em>notificare</em> — that
     * the company meets its packaging targets individually — on 25 January too, at the same
     * recipient, AFM. They are different documents, but a second deadline row would put two
     * reminders on the same day at the same address, which is the noise {@code V21} was built to
     * stop. One row, one date, a label that names both.
     */
    AFM_ANNUAL,

    /**
     * The packaging report of Ordinul 794/2012 — the Anexa 1 Ambalaje this application builds —
     * due <b>25 February</b> at the county/regional environmental agency (art. 1 and art. 6).
     *
     * <p>Added 04.09.2026, audit point 3: the application produced the document and the audit-file
     * README even named the term, but no alert existed, so a client who puts packaging on the
     * market got no warning for the single filing the whole module was built for.
     *
     * <p><b>Not</b> {@link #AFM_ANNUAL}, though the two are a month apart and both are "packaging":
     * this one goes to the environmental agency and reports quantities, that one goes to AFM and
     * pays a contribution. Different recipient, different month, different act.
     *
     * <p>Generated only for a company whose profile <em>answers</em> the market-role question and
     * puts packaging on the market. An unanswered profile gets nothing — deliberately the opposite
     * of the rule for screens (decizia 6, an empty profile restricts nothing), because an alert is
     * a claim while a screen is only an offer: a missing reminder is quieter than a false one, and
     * a false one is exactly what {@code V21} spent a migration removing.
     */
    PACKAGING_ANNUAL,

    /**
     * The yearly filing due on <b>30 April</b> at APM, for the previous calendar year —
     * OUG 92/2021 art. 49 alin. (9). Sanctioned by art. 62 alin. (1) lit. e),
     * 5.000–10.000 lei for a legal person.
     *
     * <p>Found on 10.09.2026, when the framework act was re-read on its consolidated form. It did
     * not exist anywhere: not in this enum, not in {@code DeadlineService}, not in any document we
     * produce. The calendar had 15 March, 25 January, 25 February and the AFM cadences, and a
     * client with a barrel of used oil was told, by our silence, that April held nothing.
     *
     * <p><b>Two obligations, one row</b> — the same shape as {@link #AFM_ANNUAL}, and for the same
     * reason: they fall on one date, at one recipient, and a second row would put two reminders on
     * the same day at the same address, which is the noise {@code V21} was built to stop. The
     * label names both: used oils (the measures of art. 31 alin. (1)) and construction waste
     * (conformity with art. 17 alin. (7), the 70% target).
     *
     * <p><b>The two halves are signalled differently, and that asymmetry is deliberate.</b> The
     * oils half is read from the movements — {@link ro.ecoregistru.util.UsedOilCodes} — because a
     * used-oil code in the evidence <em>is</em> the fact. The construction half is a profile
     * question ({@code Company.constructionPermitHolder}), because chapter 17 appears for anyone
     * who hauls rubble while the obligation belongs to the permit holder. Either half alone
     * creates the deadline; an unanswered profile contributes nothing.
     *
     * <p>No document is printed for it, so the deadline carries no link — the report goes into
     * APM's own system, and a link to a document we do not build would promise more than we hold.
     */
    APM_ANNUAL_APRIL,

    OTHER
}
