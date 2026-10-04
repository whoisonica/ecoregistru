package ro.ecoregistru.enums;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * The rows of Anexa 1 (consum sub 1000 tep, art. 9 alin. (7) din Legea 121/2014), in the order of the form. Each
 * carries the unit the form prints and the conversion coefficient printed on it (tep per unit). Coal and
 * "alti combustibili" have no coefficient on the form: the person writes the tep by hand.
 */
public enum EnergyCarrier {
    ELECTRICITY("MWh", "0.086"),
    HEAT("Gcal", "0.1"),
    NATURAL_GAS("MWh", "0.086"),
    FUEL_OIL("t", "0.95"),
    LIGHT_FUEL_OIL("t", "0.97"),
    PETROL("t", "1.05"),
    DIESEL("t", "1.015"),
    COAL("t", null),
    OTHER_FUEL("u.m.", null),
    RENEWABLE_ELECTRICITY("MWh", "0.086"),
    RENEWABLE_HEAT("Gcal", "0.1");

    private final String unit;
    private final BigDecimal coefficient;

    EnergyCarrier(String unit, String coefficient) {
        this.unit = unit;
        this.coefficient = coefficient == null ? null : new BigDecimal(coefficient);
    }

    public String unit() {
        return unit;
    }

    public Optional<BigDecimal> coefficient() {
        return Optional.ofNullable(coefficient);
    }

    public boolean tepWrittenByHand() {
        return coefficient == null;
    }
}
