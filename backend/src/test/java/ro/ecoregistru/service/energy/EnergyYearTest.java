package ro.ecoregistru.service.energy;

import static org.assertj.core.api.Assertions.assertThat;
import static ro.ecoregistru.enums.EnergyCarrier.COAL;
import static ro.ecoregistru.enums.EnergyCarrier.DIESEL;
import static ro.ecoregistru.enums.EnergyCarrier.ELECTRICITY;
import static ro.ecoregistru.enums.EnergyCarrier.HEAT;
import static ro.ecoregistru.enums.EnergyCarrier.OTHER_FUEL;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import ro.ecoregistru.enums.EnergyCarrier;
import ro.ecoregistru.service.energy.EnergyYear.Cell;

class EnergyYearTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static List<Cell> months(EnergyCarrier carrier, int from, int to, String quantity, String tep) {
        return IntStream.rangeClosed(from, to)
                .mapToObj(m -> new Cell(carrier, m, bd(quantity), tep == null ? null : bd(tep)))
                .toList();
    }

    @Test
    void electricityTwelveMonthsOf10MwhIs120MwhAnd10_32Tep() {
        EnergyYear y = EnergyYear.of(2025, Set.of(ELECTRICITY), months(ELECTRICITY, 1, 12, "10", null));

        assertThat(y.totals()).hasSize(1);
        assertThat(y.totals().get(0).carrier()).isEqualTo(ELECTRICITY);
        assertThat(y.totals().get(0).quantity()).isEqualByComparingTo("120");
        assertThat(y.totals().get(0).tep()).isEqualByComparingTo("10.32");
        assertThat(y.totalTep()).isEqualByComparingTo("10.32");
        assertThat(y.monthsComplete()).isEqualTo(12);
        assertThat(y.overThreshold()).isFalse();
    }

    @Test
    void dieselUsesTheFixedCoefficientNotZero() {
        EnergyYear y = EnergyYear.of(2025, Set.of(DIESEL), months(DIESEL, 1, 12, "1", "0"));

        assertThat(y.totals().get(0).tep()).isEqualByComparingTo("12.180");
        assertThat(y.totalTep()).isEqualByComparingTo("12.180");
    }

    @Test
    void aMissingMonthLeavesTheYearNullButKeepsOtherCarriers() {
        List<Cell> cells = new ArrayList<>(months(ELECTRICITY, 1, 11, "10", null));
        cells.addAll(months(HEAT, 1, 12, "5", null));
        EnergyYear y = EnergyYear.of(2025, Set.of(ELECTRICITY, HEAT), cells);

        assertThat(y.totals()).extracting(EnergyYear.CarrierTotal::carrier).containsExactly(ELECTRICITY, HEAT);
        assertThat(y.totals().get(0).quantity()).isNull();
        assertThat(y.totals().get(0).tep()).isNull();
        assertThat(y.totals().get(1).quantity()).isEqualByComparingTo("60");
        assertThat(y.totals().get(1).tep()).isEqualByComparingTo("6");
        assertThat(y.totalTep()).isEqualByComparingTo("6");
        assertThat(y.monthsComplete()).isEqualTo(11);
    }

    @Test
    void coalWithoutTepMakesTheMonthIncomplete() {
        List<Cell> cells = new ArrayList<>(months(COAL, 1, 11, "2", "1.5"));
        cells.add(new Cell(COAL, 12, bd("2"), null));
        EnergyYear y = EnergyYear.of(2025, Set.of(COAL), cells);

        assertThat(y.monthsComplete()).isEqualTo(11);
        assertThat(y.totals().get(0).quantity()).isNull();
        assertThat(y.totals().get(0).tep()).isNull();

        cells.set(11, new Cell(COAL, 12, bd("2"), bd("1.5")));
        EnergyYear full = EnergyYear.of(2025, Set.of(COAL), cells);
        assertThat(full.monthsComplete()).isEqualTo(12);
        assertThat(full.totals().get(0).quantity()).isEqualByComparingTo("24");
        assertThat(full.totals().get(0).tep()).isEqualByComparingTo("18");
    }

    @Test
    void untickedCarrierIsIgnoredEvenWithRows() {
        List<Cell> cells = new ArrayList<>(months(ELECTRICITY, 1, 12, "10", null));
        cells.addAll(months(DIESEL, 1, 12, "1", null));
        EnergyYear y = EnergyYear.of(2025, Set.of(ELECTRICITY), cells);

        assertThat(y.totals()).hasSize(1);
        assertThat(y.totalTep()).isEqualByComparingTo("10.32");
        assertThat(y.monthsComplete()).isEqualTo(12);
    }

    @Test
    void exactly1000TepIsOverTheThreshold() {
        EnergyYear y = EnergyYear.of(2025, Set.of(OTHER_FUEL),
                IntStream.rangeClosed(1, 12)
                        .mapToObj(m -> new Cell(OTHER_FUEL, m, bd("1"), m == 1 ? bd("1000") : BigDecimal.ZERO))
                        .toList());
        assertThat(y.totalTep()).isEqualByComparingTo("1000");
        assertThat(y.overThreshold()).isTrue();
    }

    @Test
    void _999_999TepIsUnder() {
        EnergyYear y = EnergyYear.of(2025, Set.of(OTHER_FUEL),
                IntStream.rangeClosed(1, 12)
                        .mapToObj(m -> new Cell(OTHER_FUEL, m, bd("1"), m == 1 ? bd("999.999") : BigDecimal.ZERO))
                        .toList());
        assertThat(y.totalTep()).isEqualByComparingTo("999.999");
        assertThat(y.overThreshold()).isFalse();
    }

    @Test
    void elevenMonthsAlreadyOver1000IsOverTheThreshold() {
        // December not entered yet: the year's total is unknown, but it can only end higher than 1100.
        EnergyYear y = EnergyYear.of(2025, Set.of(OTHER_FUEL), months(OTHER_FUEL, 1, 11, "1", "100"));

        assertThat(y.monthsComplete()).isEqualTo(11);
        assertThat(y.totals().get(0).tep()).isNull();
        assertThat(y.totalTep()).isEqualByComparingTo("0");
        assertThat(y.knownTep()).isEqualByComparingTo("1100");
        assertThat(y.overThreshold()).isTrue();
    }

    @Test
    void elevenMonthsOfElectricityOverTheLineAreOverTheThreshold() {
        // 11 × 1100 MWh × 0,086 = 1040,6 tep, with December missing.
        EnergyYear y = EnergyYear.of(2025, Set.of(ELECTRICITY), months(ELECTRICITY, 1, 11, "1100", null));

        assertThat(y.totalTep()).isEqualByComparingTo("0");
        assertThat(y.knownTep()).isEqualByComparingTo("1040.6");
        assertThat(y.overThreshold()).isTrue();
    }

    @Test
    void knownTepCountsEveryEnteredMonthOfTheTickedCarriersOnly() {
        List<Cell> cells = new ArrayList<>(months(ELECTRICITY, 1, 3, "10", null)); // 3 × 0,86
        cells.add(new Cell(COAL, 1, bd("2"), bd("1.5")));
        cells.add(new Cell(COAL, 2, bd("2"), null)); // no tep yet: adds nothing
        cells.addAll(months(DIESEL, 1, 12, "100", null)); // unticked
        EnergyYear y = EnergyYear.of(2025, Set.of(ELECTRICITY, COAL), cells);

        assertThat(y.knownTep()).isEqualByComparingTo("4.08");
        assertThat(y.knownTep().scale()).isEqualTo(4);
        assertThat(y.totalTep()).isEqualByComparingTo("0");
        assertThat(y.overThreshold()).isFalse();
    }

    @Test
    void aCompleteYearKnowsItsWholeTotal() {
        EnergyYear y = EnergyYear.of(2025, Set.of(ELECTRICITY), months(ELECTRICITY, 1, 12, "10", null));

        assertThat(y.knownTep()).isEqualByComparingTo(y.totalTep());
    }

    @Test
    void noCarriersMeansZeroCompleteMonths() {
        EnergyYear y = EnergyYear.of(2025, Set.of(), List.of());

        assertThat(y.totals()).isEmpty();
        assertThat(y.monthsComplete()).isZero();
        assertThat(y.totalTep()).isEqualByComparingTo("0");
        assertThat(y.knownTep()).isEqualByComparingTo("0");
        assertThat(y.overThreshold()).isFalse();
    }
}
