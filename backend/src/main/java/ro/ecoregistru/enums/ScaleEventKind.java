package ro.ecoregistru.enums;

/**
 * Ce s-a întâmplat cu un cântar (D2.3). O verificare ADMIS dă valabilitate; o verificare RESPINS, o
 * reparație sau un incident o anulează pe loc, până la următoarea verificare (IML 3-05 art. 7 lit. d),
 * art. 13, art. 14 alin. (2)).
 */
public enum ScaleEventKind {
    VERIFICATION,
    REPAIR,
    INCIDENT
}
