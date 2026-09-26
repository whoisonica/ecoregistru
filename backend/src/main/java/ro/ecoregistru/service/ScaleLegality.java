package ro.ecoregistru.service;

import ro.ecoregistru.enums.ScaleEventKind;
import ro.ecoregistru.enums.ScaleStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * D2.3 — e legal cântarul la data unei cântăriri? Temeiul e în {@code surse-oficiale.md} §17.1:
 * <ul>
 *   <li>unul fără marcaj în termen „nu are calitatea de mijloc de măsurare legal” și nu se folosește
 *       (OG 20/1992 art. 19); se declară la BRML înainte de utilizare (art. 24 alin. 1);</li>
 *   <li>intervalul e de un an între două verificări, oricare ar fi ele, iar la un cântar nou primul an
 *       curge de la punerea în funcțiune (L.O.-2022 art. 8–9);</li>
 *   <li>după o reparație sau un incident, cântarul se reverifică indiferent de termen (IML 3-05 art. 7
 *       lit. d), art. 13), deci valabilitatea cade pe loc; un buletin RESPINS nu dă valabilitate.</li>
 * </ul>
 * Contează ultimul eveniment până la data cântăririi inclusiv; în aceeași zi, cel înregistrat mai
 * târziu (reparația de dimineață, reverificarea de după-amiază).
 */
public final class ScaleLegality {

    /** Un rând din istoricul cântarului. {@code admitted} și {@code validUntil} sunt doar ale verificărilor. */
    public record Event(ScaleEventKind kind, LocalDate date, Boolean admitted, LocalDate validUntil,
                        Instant recordedAt) {
    }

    public enum State {
        VALID,
        /** Nici verificare, nici an de la punerea în funcțiune. */
        NO_VERIFICATION,
        EXPIRED,
        REJECTED,
        REPAIRED,
        INCIDENT,
        NOT_DECLARED,
        SEALED,
        OUT_OF_USE
    }

    /**
     * @param validUntil până când ține ultima valabilitate cunoscută (și când e deja trecută), sau null
     */
    public record Verdict(State state, LocalDate validUntil) {
        public boolean legal() {
            return state == State.VALID;
        }

        /** Sigilat sau scos din uz: nu se cântărește deloc. Restul cer doar confirmare cu motiv. */
        public boolean refused() {
            return state == State.SEALED || state == State.OUT_OF_USE;
        }
    }

    /** Intervalul dintre verificări la toate cântarele (L.O.-2022). */
    public static final int VALIDITY_MONTHS = 12;

    private ScaleLegality() {
    }

    public static Verdict at(ScaleStatus status, LocalDate commissionedOn, LocalDate brmlDeclaredOn,
                             List<Event> events, LocalDate date) {
        if (status == ScaleStatus.SEALED) return new Verdict(State.SEALED, null);
        if (status == ScaleStatus.OUT_OF_USE) return new Verdict(State.OUT_OF_USE, null);

        Verdict metrology = metrology(commissionedOn, events, date);
        if (!metrology.legal()) {
            return metrology;
        }
        if (brmlDeclaredOn == null || brmlDeclaredOn.isAfter(date)) {
            return new Verdict(State.NOT_DECLARED, metrology.validUntil());
        }
        return metrology;
    }

    private static Verdict metrology(LocalDate commissionedOn, List<Event> events, LocalDate date) {
        Optional<Event> last = events.stream()
                .filter(e -> !e.date().isAfter(date))
                .max(Comparator.comparing(Event::date).thenComparing(Event::recordedAt));
        if (last.isEmpty()) {
            if (commissionedOn == null || commissionedOn.isAfter(date)) {
                return new Verdict(State.NO_VERIFICATION, null);
            }
            return byDate(commissionedOn.plusMonths(VALIDITY_MONTHS), date);
        }
        Event event = last.get();
        return switch (event.kind()) {
            case REPAIR -> new Verdict(State.REPAIRED, null);
            case INCIDENT -> new Verdict(State.INCIDENT, null);
            case VERIFICATION -> Boolean.TRUE.equals(event.admitted())
                    ? byDate(event.validUntil(), date)
                    : new Verdict(State.REJECTED, null);
        };
    }

    private static Verdict byDate(LocalDate validUntil, LocalDate date) {
        return new Verdict(date.isAfter(validUntil) ? State.EXPIRED : State.VALID, validUntil);
    }
}
