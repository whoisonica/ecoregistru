package ro.ecoregistru.service.energy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import ro.ecoregistru.enums.EnergyCarrier;

/**
 * The year of an energy declaration, computed from the monthly cells of the ticked carriers: per-carrier annual
 * totals, total tep, how many months are complete and whether the 1000 tep threshold is reached. Pure calculation.
 */
public record EnergyYear(
        int year, List<CarrierTotal> totals, BigDecimal totalTep, int monthsComplete, boolean overThreshold) {

    public record Cell(EnergyCarrier carrier, int month, BigDecimal quantity, BigDecimal tep) {}

    /** Annual figures of one ticked carrier; null when some of the 12 months are not complete. */
    public record CarrierTotal(EnergyCarrier carrier, BigDecimal quantity, BigDecimal tep) {}

    public static final BigDecimal THRESHOLD_TEP = new BigDecimal("1000");

    public static EnergyYear of(int year, Set<EnergyCarrier> used, List<Cell> cells) {
        List<EnergyCarrier> carriers =
                Arrays.stream(EnergyCarrier.values()).filter(used::contains).toList();
        Map<EnergyCarrier, Map<Integer, Cell>> byCarrier = cells.stream()
                .filter(c -> used.contains(c.carrier()))
                .collect(Collectors.groupingBy(
                        Cell::carrier, Collectors.toMap(Cell::month, Function.identity(), (a, b) -> b)));

        int monthsComplete = 0;
        if (!carriers.isEmpty()) {
            for (int month = 1; month <= 12; month++) {
                final int m = month;
                if (carriers.stream().allMatch(c -> isComplete(c, byCarrier.get(c), m))) {
                    monthsComplete++;
                }
            }
        }

        List<CarrierTotal> totals = carriers.stream()
                .map(c -> total(c, byCarrier.get(c)))
                .toList();
        BigDecimal totalTep = totals.stream()
                .map(CarrierTotal::tep)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EnergyYear(year, totals, totalTep, monthsComplete, totalTep.compareTo(THRESHOLD_TEP) >= 0);
    }

    private static boolean isComplete(EnergyCarrier carrier, Map<Integer, Cell> months, int month) {
        Cell cell = months == null ? null : months.get(month);
        return cell != null && cell.quantity() != null && (!carrier.tepWrittenByHand() || cell.tep() != null);
    }

    private static CarrierTotal total(EnergyCarrier carrier, Map<Integer, Cell> months) {
        for (int month = 1; month <= 12; month++) {
            if (!isComplete(carrier, months, month)) {
                return new CarrierTotal(carrier, null, null);
            }
        }
        BigDecimal quantity = months.values().stream()
                .filter(c -> c.month() >= 1 && c.month() <= 12)
                .map(Cell::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tep = carrier.coefficient()
                .map(k -> quantity.multiply(k))
                .orElseGet(() -> months.values().stream()
                        .filter(c -> c.month() >= 1 && c.month() <= 12)
                        .map(Cell::tep)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .setScale(4, RoundingMode.HALF_UP);
        return new CarrierTotal(carrier, quantity, tep);
    }
}
