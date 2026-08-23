package ro.ecoregistru.enums;

import java.util.Collection;

/**
 * What the company is on the market for the goods it sells — the "tip de generator" question from
 * the meeting of 23.08.2026: <em>producător / importator / comerciant</em>.
 *
 * <p>The trio is not ours: it is the classification the packaging act uses. Legea 249/2015,
 * anexa nr. 1, defines them together — "operatori economici - referitor la ambalaje, înseamnă
 * furnizorii de materiale de ambalare, producătorii de ambalaje şi produse ambalate,
 * <b>importatorii, comercianţii, distribuitorii</b>, autorităţile publice şi organizaţiile
 * neguvernamentale" — and the declaration built on it, Ordinul MMP 794/2012 anexa 1, is titled
 * "Producători şi importatori de ambalaje de desfacere, <b>de produse ambalate</b>,
 * supraambalatori de produse ambalate".
 *
 * <p><b>What it decides.</b> Only a company that puts packaged goods on the national market puts
 * the packaging there with them, so only it files that declaration and owes the packaging
 * contribution to AFM (OUG 196/2005 art. 9 alin. (1) lit. d). A {@link #TRADER} sells goods
 * somebody else packaged — "nu are deşeuri proprii", in the specialist's words: none of the
 * packaging on its shelves was introduced by it.
 *
 * <p><b>What it does NOT decide.</b> The Anexa 1 of HG 856/2002 — the fişa de evidenţă a gestiunii
 * deşeurilor this application prints — is a different document that happens to share a name. Art. 1
 * alin. (1) binds <em>every</em> agent that generates waste, whatever it sells, so a TRADER with a
 * cardboard bin in the yard keeps it exactly like anyone else. See docs/surse-oficiale.md §5 for
 * the two documents side by side; confusing them is the cheapest way to break this module.
 *
 * <p>An empty set means the question has not been answered, and nothing follows from it — the same
 * rule the rest of the account profile lives by.
 */
public enum MarketRole {

    /** Fabrică sau ambalează el produsele pe care le pune pe piaţa naţională. */
    PRODUCER,

    /** Aduce în ţară produse ambalate şi le pune pe piaţa naţională. */
    IMPORTER,

    /** Vinde marfă ambalată de altcineva: nu el introduce ambalajul pe piaţă. */
    TRADER;

    /** Whether this role introduces packaging on the national market together with the goods. */
    public boolean putsPackagingOnMarket() {
        return this != TRADER;
    }

    /**
     * Whether the company owes the packaging declaration. {@code false} for an unanswered profile
     * too — the caller must ask {@link #answered(Collection)} first if it needs to tell "no" from
     * "we do not know yet".
     */
    public static boolean putsPackagingOnMarket(Collection<MarketRole> roles) {
        return roles != null && roles.stream().anyMatch(MarketRole::putsPackagingOnMarket);
    }

    /** Whether the intake form's "tip de generator" question has an answer at all. */
    public static boolean answered(Collection<MarketRole> roles) {
        return roles != null && !roles.isEmpty();
    }
}
