package ro.ecoregistru.service;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.enums.ScaleEventKind;
import ro.ecoregistru.enums.ScaleStatus;
import ro.ecoregistru.service.ScaleLegality.Event;
import ro.ecoregistru.service.ScaleLegality.State;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ro.ecoregistru.enums.ScaleEventKind.*;

/**
 * D2.3 — cântarul e legal la data unei cântăriri (OG 20/1992 art. 19, art. 24; L.O.-2022 art. 8–9;
 * IML 3-05 art. 13, 17). Regula e pură, deci se probează fără bază.
 */
class ScaleLegalityTest {

    static final LocalDate COMMISSIONED = LocalDate.of(2025, 3, 1);
    static final LocalDate DECLARED = LocalDate.of(2025, 2, 20);

    @Test
    void aNewScaleIsLegalForTheFirstYearFromCommissioning() {
        var inside = ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, DECLARED, List.of(), LocalDate.of(2026, 3, 1));
        assertThat(inside.state()).isEqualTo(State.VALID);
        assertThat(inside.validUntil()).isEqualTo(LocalDate.of(2026, 3, 1));

        var after = ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, DECLARED, List.of(), LocalDate.of(2026, 3, 2));
        assertThat(after.state()).isEqualTo(State.EXPIRED);

        var before = ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, DECLARED, List.of(), LocalDate.of(2025, 2, 28));
        assertThat(before.state()).as("înainte de punerea în funcțiune").isEqualTo(State.NO_VERIFICATION);
    }

    @Test
    void withoutCommissioningOrVerificationThereIsNoValidity() {
        var verdict = ScaleLegality.at(ScaleStatus.IN_USE, null, DECLARED, List.of(), LocalDate.of(2026, 1, 1));
        assertThat(verdict.state()).isEqualTo(State.NO_VERIFICATION);
        assertThat(verdict.validUntil()).isNull();
    }

    @Test
    void anAdmittedVerificationGivesValidityUntilItsDateInclusive() {
        var events = List.of(admis(LocalDate.of(2026, 2, 10), LocalDate.of(2027, 2, 10)));
        assertThat(ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, DECLARED, events, LocalDate.of(2027, 2, 10)).state())
                .isEqualTo(State.VALID);
        var expired = ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, DECLARED, events, LocalDate.of(2027, 2, 11));
        assertThat(expired.state()).isEqualTo(State.EXPIRED);
        assertThat(expired.validUntil()).isEqualTo(LocalDate.of(2027, 2, 10));
    }

    /** Evenimentele de după data cântăririi nu schimbă ce era atunci. */
    @Test
    void onlyEventsUpToTheWeighingDateCount() {
        var events = List.of(
                admis(LocalDate.of(2026, 2, 10), LocalDate.of(2027, 2, 10)),
                event(INCIDENT, LocalDate.of(2026, 6, 1), null, null, 2));
        assertThat(ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, DECLARED, events, LocalDate.of(2026, 5, 31)).state())
                .isEqualTo(State.VALID);
        assertThat(ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, DECLARED, events, LocalDate.of(2026, 6, 1)).state())
                .isEqualTo(State.INCIDENT);
    }

    @Test
    void aRepairOrARejectionCancelsTheValidityOnTheSpot() {
        var repaired = List.of(
                admis(LocalDate.of(2026, 2, 10), LocalDate.of(2027, 2, 10)),
                event(REPAIR, LocalDate.of(2026, 4, 1), null, null, 2));
        assertThat(ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, DECLARED, repaired, LocalDate.of(2026, 4, 2)).state())
                .isEqualTo(State.REPAIRED);

        var rejected = List.of(
                admis(LocalDate.of(2026, 2, 10), LocalDate.of(2027, 2, 10)),
                event(VERIFICATION, LocalDate.of(2026, 4, 1), false, null, 2));
        assertThat(ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, DECLARED, rejected, LocalDate.of(2026, 4, 2)).state())
                .isEqualTo(State.REJECTED);
    }

    /** Reparația și reverificarea în aceeași zi: ordinea o dă momentul înregistrării. */
    @Test
    void aReverificationTheSameDayAsTheRepairRestoresValidity() {
        LocalDate day = LocalDate.of(2026, 4, 1);
        var repair = event(REPAIR, day, null, null, 2);
        var reverification = event(VERIFICATION, day, true, LocalDate.of(2027, 4, 1), 3);
        // Ordinea din listă nu contează: rândurile vin din bază cum vin.
        for (var events : List.of(List.of(repair, reverification), List.of(reverification, repair))) {
            var verdict = ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, DECLARED, events, day);
            assertThat(verdict.state()).isEqualTo(State.VALID);
            assertThat(verdict.validUntil()).isEqualTo(LocalDate.of(2027, 4, 1));
        }
        // Invers: reverificare dimineața, incident după-amiaza.
        var incident = event(INCIDENT, day, null, null, 4);
        assertThat(ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, DECLARED, List.of(incident, reverification), day)
                .state()).isEqualTo(State.INCIDENT);
    }

    @Test
    void aMetrologicallyValidScaleNotDeclaredToBrmlBeforeTheWeighingIsNotLegal() {
        var events = List.of(admis(LocalDate.of(2026, 2, 10), LocalDate.of(2027, 2, 10)));
        assertThat(ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, null, events, LocalDate.of(2026, 3, 1)).state())
                .isEqualTo(State.NOT_DECLARED);
        assertThat(ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, LocalDate.of(2026, 3, 2), events,
                LocalDate.of(2026, 3, 1)).state()).isEqualTo(State.NOT_DECLARED);
        // Metrologia are întâietate: un cântar expirat și nedeclarat se arată ca expirat.
        assertThat(ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, null, events, LocalDate.of(2027, 3, 1)).state())
                .isEqualTo(State.EXPIRED);
    }

    @Test
    void aSealedOrRetiredScaleIsRefusedWhateverItsBulletin() {
        var events = List.of(admis(LocalDate.of(2026, 2, 10), LocalDate.of(2027, 2, 10)));
        var sealed = ScaleLegality.at(ScaleStatus.SEALED, COMMISSIONED, DECLARED, events, LocalDate.of(2026, 3, 1));
        assertThat(sealed.state()).isEqualTo(State.SEALED);
        assertThat(sealed.refused()).isTrue();
        var retired = ScaleLegality.at(ScaleStatus.OUT_OF_USE, COMMISSIONED, DECLARED, events, LocalDate.of(2026, 3, 1));
        assertThat(retired.state()).isEqualTo(State.OUT_OF_USE);
        assertThat(retired.refused()).isTrue();

        var valid = ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, DECLARED, events, LocalDate.of(2026, 3, 1));
        assertThat(valid.legal()).isTrue();
        assertThat(valid.refused()).isFalse();
        var expired = ScaleLegality.at(ScaleStatus.IN_USE, COMMISSIONED, DECLARED, events, LocalDate.of(2028, 1, 1));
        assertThat(expired.legal()).isFalse();
        assertThat(expired.refused()).as("expirat = confirmare cu motiv, nu refuz").isFalse();
    }

    private static Event admis(LocalDate date, LocalDate validUntil) {
        return event(VERIFICATION, date, true, validUntil, 1);
    }

    private static Event event(ScaleEventKind kind, LocalDate date, Boolean admitted, LocalDate validUntil, int order) {
        return new Event(kind, date, admitted, validUntil, Instant.ofEpochSecond(order));
    }
}
