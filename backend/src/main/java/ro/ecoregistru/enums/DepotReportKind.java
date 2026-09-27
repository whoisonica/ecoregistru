package ro.ecoregistru.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * D4.7 — rapoartele fixe ale depozitului (ecranul Cântar → Rapoarte). {@code slug} e partea din adresă și din numele
 * fișierului; {@code pdf} spune dacă raportul are și forma semnabilă (decizia proprietarului, 27.09.2026: doar cele care
 * se arhivează sau se arată la control); {@code access} e pragul; {@code wholeCompany} — declarațiile și plafonul de
 * numerar sunt ale firmei, deci depozitul ales nu le taie.
 */
public enum DepotReportKind {

    REGISTER("registru", false, Access.ANY, false),
    SCALE_LOG("jurnal-cantar", false, Access.ANY, false),
    ISSUED_DOCUMENTS("documente", false, Access.ANY, false),
    CANCELLED("anulate", false, Access.ANY, false),
    STOCK_CARD("fisa-stoc", true, Access.ANY, false),
    TRANSFERS("transferuri", false, Access.ANY, false),
    AFM("afm", true, Access.MONEY, true),
    INCOME_TAX("impozit", true, Access.MONEY, true),
    CASH_PF("numerar", false, Access.MONEY, true),
    INDIVIDUALS("persoane-fizice", true, Access.APPROVER, false),
    PARTNER_CARD("partener", false, Access.ANY, false);

    /**
     * {@code ANY} = cine vede lista Cântarului, pe depozitele lui; {@code APPROVER} = cine finalizează (CNP-uri
     * întregi); {@code MONEY} = cine finalizează <b>și</b> vede prețurile, ca raportul reținerilor.
     */
    public enum Access { ANY, APPROVER, MONEY }

    private final String slug;
    private final boolean pdf;
    private final Access access;
    private final boolean wholeCompany;

    DepotReportKind(String slug, boolean pdf, Access access, boolean wholeCompany) {
        this.slug = slug;
        this.pdf = pdf;
        this.access = access;
        this.wholeCompany = wholeCompany;
    }

    public String slug() {
        return slug;
    }

    public boolean pdf() {
        return pdf;
    }

    public Access access() {
        return access;
    }

    public boolean wholeCompany() {
        return wholeCompany;
    }

    public static Optional<DepotReportKind> ofSlug(String slug) {
        return Arrays.stream(values()).filter(k -> k.slug.equals(slug)).findFirst();
    }
}
