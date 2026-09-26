package ro.ecoregistru.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D2.5 — toleranța a două cântare la un transfer: HG 710/2015 anexa 1 tabelul 3, dublată în exploatare (pct. 4.2).
 * Exemplul din `surse-oficiale.md` §4 (runda 2): la ~10 t cu e = 20 kg, fiecare pod greșește cu cel mult ±20 kg,
 * deci două poduri conforme diferă cu cel mult 40 kg — 80 kg dacă netul vine din două cântăriri (brut și tara).
 */
class TransferToleranceTest {

    static final BigDecimal E20 = new BigDecimal("20");

    @Test
    void theExampleFromTheSourcesTenTonnesOnTwentyKilogramDivisions() {
        var a = new TransferTolerance.Side("III", E20, new BigDecimal("10000"), false);
        var b = new TransferTolerance.Side("III", E20, new BigDecimal("10000"), false);
        assertThat(TransferTolerance.of(a, b)).isEqualByComparingTo("40");

        var aTwice = new TransferTolerance.Side("III", E20, new BigDecimal("10000"), true);
        var bTwice = new TransferTolerance.Side("III", E20, new BigDecimal("10000"), true);
        assertThat(TransferTolerance.of(aTwice, bTwice)).isEqualByComparingTo("80");
    }

    @Test
    void theStepsOfClassThreeAreHalfOneAndOneAndAHalfDivisions() {
        // În exploatare: 2 × (0,5 e | 1 e | 1,5 e), pragurile la 500 e și 2 000 e.
        assertThat(TransferTolerance.inService("III", E20, new BigDecimal("10000"))).isEqualByComparingTo("20");
        assertThat(TransferTolerance.inService("III", E20, new BigDecimal("10020"))).isEqualByComparingTo("40");
        assertThat(TransferTolerance.inService("III", E20, new BigDecimal("40000"))).isEqualByComparingTo("40");
        assertThat(TransferTolerance.inService("III", E20, new BigDecimal("40020"))).isEqualByComparingTo("60");
        // Peste capătul tabelului rămâne ultima treaptă.
        assertThat(TransferTolerance.inService("III", E20, new BigDecimal("900000"))).isEqualByComparingTo("60");
    }

    @Test
    void theOtherClassesHaveTheirOwnThresholds() {
        BigDecimal e = new BigDecimal("1");
        assertThat(TransferTolerance.inService("IIII", e, new BigDecimal("50"))).isEqualByComparingTo("1");
        assertThat(TransferTolerance.inService("IIII", e, new BigDecimal("51"))).isEqualByComparingTo("2");
        assertThat(TransferTolerance.inService("II", e, new BigDecimal("5000"))).isEqualByComparingTo("1");
        assertThat(TransferTolerance.inService("II", e, new BigDecimal("20001"))).isEqualByComparingTo("3");
        assertThat(TransferTolerance.inService("I", e, new BigDecimal("50001"))).isEqualByComparingTo("2");
    }

    @Test
    void withoutTheClassOrTheDivisionOfEitherScaleTheToleranceIsUnknown() {
        var known = new TransferTolerance.Side("III", E20, new BigDecimal("10000"), false);
        assertThat(TransferTolerance.of(known, new TransferTolerance.Side(null, E20, new BigDecimal("10000"), false))).isNull();
        assertThat(TransferTolerance.of(known, new TransferTolerance.Side("III", null, new BigDecimal("10000"), false))).isNull();
        assertThat(TransferTolerance.of(null, known)).isNull();
    }
}
