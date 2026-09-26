package ro.ecoregistru.enums;

/**
 * Starea unui cântar de depozit (D2.3). Doar {@code IN_USE} se poate folosi la o cântărire: unul
 * scos din uz sau sigilat de BRML nu mai e mijloc de măsurare legal (OG 20/1992 art. 19 alin. 2).
 */
public enum ScaleStatus {
    IN_USE,
    OUT_OF_USE,
    SEALED
}
