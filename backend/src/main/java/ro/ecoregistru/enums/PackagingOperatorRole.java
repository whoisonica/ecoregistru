package ro.ecoregistru.enums;

import lombok.Getter;

/**
 * What the company is in the packaging-waste chain, and therefore <b>which table of Anexa 3 la
 * Ordinul 794/2012 it files</b>.
 *
 * <p><b>Art. 4 alin. (1)</b>, verbatim: colectorii, reciclatorii, valorificatorii, <b>comercianţii</b>
 * de deşeuri de ambalaje şi operatorii de salubritate autorizaţi pentru colectare "sunt obligaţi să
 * raporteze datele prevăzute în anexa nr. 3, <b>tabelul 1 sau, după caz, tabelul 2</b>". The act
 * names the four qualities and pairs them with a table; it does not leave the pairing to us.
 *
 * <p><b>Alin. (3)</b> adds where each files: collectors, recyclers and recoverers report to the
 * agency in whose area they operate, <b>traders report to ANPM</b>. That is why
 * {@link #COMERCIANT} is a value of its own rather than a flavour of collector — the two file the
 * same table at different addressees.
 *
 * <p><b>Why this is asked rather than derived.</b> The obvious shortcut is to look at whether the
 * company has recorded R3/R4/R5 operations of its own and call it a recycler. That is a guess on a
 * form filed with an authority, which regula de lucru 1 forbids — and worse, an unstable one: a
 * quiet quarter would silently move the company to the other table. A quality is what a company
 * <em>is</em>, not what it happened to record last month.
 *
 * <p><b>Null means unanswered, and then no document prints.</b> This does not contradict decision 6
 * ("empty profile = no restriction") — it is decision 37 applied to a document: a screen is an
 * offer, a document is an assertion. Printing "Colectori/Comercianţi" in the header of a form we
 * were never told applies would be asserting the company's legal quality on its behalf.
 */
@Getter
public enum PackagingOperatorRole {

    /** Takes packaging waste over from others. Tabelul 1. */
    COLECTOR("Colector", Anexa3Table.TABEL_1),

    /** Trades in packaging waste. Tabelul 1 — but filed at ANPM, per art. 4 alin. (3). */
    COMERCIANT("Comerciant", Anexa3Table.TABEL_1),

    /** Reprocesses it into materials. Tabelul 2. */
    RECICLATOR("Reciclator", Anexa3Table.TABEL_2),

    /** Recovers it by other means than recycling. Tabelul 2. */
    VALORIFICATOR("Valorificator", Anexa3Table.TABEL_2);

    /** Which of the annex's two tables this quality fills in. */
    public enum Anexa3Table { TABEL_1, TABEL_2 }

    private final String officialLabel;
    private final Anexa3Table table;

    PackagingOperatorRole(String officialLabel, Anexa3Table table) {
        this.officialLabel = officialLabel;
        this.table = table;
    }

    /**
     * The heading the chosen table prints, verbatim from the annex: tabelul 1 is headed
     * "Operatori economici colectori şi comercianţi de deşeuri de ambalaje" and tabelul 2
     * "Operatori economici reciclatori şi valorificatori de deşeuri de ambalaje".
     */
    public String tableHeading() {
        return table == Anexa3Table.TABEL_1
                ? "Operatori economici colectori şi comercianţi de deşeuri de ambalaje"
                : "Operatori economici reciclatori şi valorificatori de deşeuri de ambalaje";
    }

    /**
     * Where the report goes. Art. 4 alin. (3): everyone but the trader files at the county agency
     * in whose area the work point lies; the trader files at ANPM. Printed on the screen, not on
     * the form — the form has no rubric for it.
     */
    public String addressee() {
        return this == COMERCIANT
                ? "ANPM"
                : "agenţia judeţeană pentru protecţia mediului din raza punctului de lucru";
    }
}
