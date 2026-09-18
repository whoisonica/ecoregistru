package ro.ecoregistru.enums;

/**
 * Ce se cere de pe ecranul „Generare”, tabul Mișcări, când se apasă o tastă de ambalaje.
 *
 * <p>Există din 18.09.2026, când registrul de mișcări al tabului „Ambalaje” a fost scos:
 * aceleași rânduri se vedeau în două locuri, iar cel de-al doilea nu putea nici căuta, nici
 * sorta la server. Mișcările rămân într-un singur tabel, iar întrebarea „arată-mi numai
 * ambalajele” a devenit un filtru al lui.
 */
public enum PackagingFilter {

    /** Orice mișcare pe un cod {@code 15 01 xx}, indiferent cine a pus ambalajul pe piață. */
    ANY,

    /**
     * Numai ce hrănește Anexa 1 Ambalaje: cod de ambalaje și ambalajul pus pe piață de firmă.
     *
     * <p>Bifa neatinsă (dinaintea întrebării) trece, ca peste tot: un rând vechi se comportă ca
     * „da” până când cineva îl deschide și spune altceva — vezi {@code WasteMovement#packagingOnMarket}.
     */
    ON_MARKET,

    /**
     * Rândurile care hrănesc declarația, dar cărora le lipsește materialul (și codul nu-l decide)
     * sau felul ambalajului — adică exact ce nu poate intra în tabelul 1.
     *
     * <p>⚠️ Poate arăta mai multe rânduri decât numără banda „Nu intră în declarație” de pe tabul
     * „Ambalaje”: banda numără <b>încărcături</b> (generarea și predarea aceleiași încărcături se
     * socotesc o dată), filtrul arată <b>mișcări</b>. Amândouă au de completat același lucru, deci
     * niciuna nu minte; numărul care contează pentru declarație rămâne cel din bandă.
     */
    INCOMPLETE
}
