package ro.ecoregistru.util;

import java.util.Set;

/**
 * Codurile LER care sunt „deșeuri metalice feroase și neferoase și aliaje ale acestora” în sensul
 * OUG 31/2011 art. 1 alin. (1^2). La ele borderoul cere CNP-ul și actul de identitate, iar plata către
 * o persoană fizică poartă impozitul de 10% (Codul fiscal art. 114 alin. (2) lit. m²), art. 115 alin. (1)).
 *
 * <p>E doar o <b>propunere</b> pentru bifa `metal` a sortimentului; omul o poate schimba. Motivul:
 * un cod amestecat, ca 19 12 12, poate fi „fier vechi” la un depozit și nimic metalic la altul.
 * Acumulatorii (16 06 xx) <b>nu</b> sunt pe listă: sunt deșeu de baterii, cu regimul HG 1132/2008, și
 * art. 62 lit. f) din Codul fiscal nu-i impozitează (`surse-oficiale.md` §18.5).
 */
public final class MetalWasteCodes {

    /** 17 04 xx e toată grupa „metale (inclusiv aliajele lor)” din construcții și demolări. */
    private static final String CONSTRUCTION_METALS = "17 04 ";

    private static final Set<String> CODES = Set.of(
            "12 01 01", "12 01 02", "12 01 03", "12 01 04", // pilitură și șpan feros/neferos, praf și particule
            "15 01 04",                                     // ambalaje metalice
            "16 01 17", "16 01 18",                         // metale feroase/neferoase din vehicule scoase din uz
            "19 10 01", "19 10 02",                         // deșeuri de fier și oțel / neferoase de la mărunțire
            "19 12 02", "19 12 03",                         // metale feroase / neferoase de la tratarea mecanică
            "20 01 40"                                      // metale, din deșeurile municipale
    );

    private MetalWasteCodes() {
    }

    public static boolean suggests(String code) {
        if (code == null) {
            return false;
        }
        String plain = code.replace("*", "").trim();
        return plain.startsWith(CONSTRUCTION_METALS) || CODES.contains(plain);
    }
}
