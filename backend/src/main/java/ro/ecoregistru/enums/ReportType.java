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

    AFM_MONTHLY,

    OTHER
}
