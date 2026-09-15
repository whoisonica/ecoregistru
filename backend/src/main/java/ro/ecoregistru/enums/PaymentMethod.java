package ro.ecoregistru.enums;

/**
 * Cum se plătește o operațiune de depozit. Borderoul PF cere „chitanța nr. ... sau [...] virament
 * bancar” (OUG 31/2011, anexa), iar numerarul către o persoană are plafon de 10.000 lei pe zi
 * (Legea 70/2015 art. 4).
 */
public enum PaymentMethod {
    VIREMENT,
    NUMERAR
}
