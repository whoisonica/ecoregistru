package ro.ecoregistru.enums;

import lombok.Getter;

/**
 * Starea fizică a deşeului — rubrica „Starea fizică:" din antetul fişei, HG 856/2002 anexa nr. 1,
 * cap. 1.
 *
 * <p>⚠️ <b>Singurul enum al fişei care NU e un cod.</b> Celelalte rubrici — tipul de stocare (nota 1),
 * metoda de tratare (nota 2), mijlocul de transport (nota 4), destinaţia (nota 5) — sunt abrevieri
 * pe care formularul însuşi le tipăreşte (`CT`, `TM`, `AN`, `DO`), aşa că acolo numele constantei
 * <i>este</i> ce trebuie scris pe hârtie. Aici nu: rubrica cere un cuvânt, iar numele constantei e
 * în engleză, ca tot codul.
 *
 * <p>De aceea starea fizică se tipăreşte din {@link #getLabel()}, niciodată din {@code name()}.
 * Până pe 20.09.2026 fişa scria „PASTY" şi „SLUDGE" pe un formular oficial în română. Nu s-a văzut
 * mai devreme fiindcă {@code SOLID} arată la fel în amândouă limbile, iar toate datele demo şi toate
 * probele foloseau {@code SOLID} — deci greşeala apărea abia la primul client cu un deşeu păstos,
 * nămol sau pulbere.
 *
 * <p>ℹ️ Setul de valori rămâne cel de lucru: actul nu dă o listă închisă pentru rubrica asta
 * (vezi {@code docs/legislatie.md} §4 Q6).
 */
@Getter
public enum PhysicalState {

    SOLID("Solid"),
    LIQUID("Lichid"),
    SLUDGE("Nămol"),
    PASTY("Păstos"),
    POWDER("Pulbere"),
    GASEOUS("Gazos");

    /** Cuvântul care se scrie pe fişă, în română. Aceleaşi texte ca în `strings.ts`. */
    private final String label;

    PhysicalState(String label) {
        this.label = label;
    }
}
