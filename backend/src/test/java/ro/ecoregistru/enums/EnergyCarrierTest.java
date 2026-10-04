package ro.ecoregistru.enums;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyCarrierTest {

    @Test
    void coefficientsArePrintedOnAnnex1() {
        assertThat(EnergyCarrier.ELECTRICITY.coefficient()).contains(new BigDecimal("0.086"));
        assertThat(EnergyCarrier.HEAT.coefficient()).contains(new BigDecimal("0.1"));
        assertThat(EnergyCarrier.NATURAL_GAS.coefficient()).contains(new BigDecimal("0.086"));
        assertThat(EnergyCarrier.FUEL_OIL.coefficient()).contains(new BigDecimal("0.95"));
        assertThat(EnergyCarrier.LIGHT_FUEL_OIL.coefficient()).contains(new BigDecimal("0.97"));
        assertThat(EnergyCarrier.PETROL.coefficient()).contains(new BigDecimal("1.05"));
        assertThat(EnergyCarrier.DIESEL.coefficient()).contains(new BigDecimal("1.015"));
        assertThat(EnergyCarrier.RENEWABLE_ELECTRICITY.coefficient()).contains(new BigDecimal("0.086"));
        assertThat(EnergyCarrier.RENEWABLE_HEAT.coefficient()).contains(new BigDecimal("0.1"));
    }

    @Test
    void coalAndOtherFuelsHaveTheirTepWrittenByHand() {
        for (EnergyCarrier c : EnergyCarrier.values()) {
            boolean byHand = c == EnergyCarrier.COAL || c == EnergyCarrier.OTHER_FUEL;
            assertThat(c.tepWrittenByHand()).as(c.name()).isEqualTo(byHand);
            assertThat(c.coefficient().isEmpty()).as(c.name()).isEqualTo(byHand);
        }
    }

    @Test
    void unitsFollowTheAnnex() {
        assertThat(EnergyCarrier.values()).extracting(EnergyCarrier::unit).containsExactly(
                "MWh", "Gcal", "MWh", "t", "t", "t", "t", "t", "u.m.", "MWh", "Gcal");
        assertThat(EnergyCarrier.values()).extracting(Enum::name).containsExactly(
                "ELECTRICITY", "HEAT", "NATURAL_GAS", "FUEL_OIL", "LIGHT_FUEL_OIL", "PETROL", "DIESEL", "COAL",
                "OTHER_FUEL", "RENEWABLE_ELECTRICITY", "RENEWABLE_HEAT");
    }
}
