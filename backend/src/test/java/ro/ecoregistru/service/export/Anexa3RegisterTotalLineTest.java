package ro.ecoregistru.service.export;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Rândul de total din Registrul Anexa 3: numeralul acordat (decizia 62; 29.09.2026). */
class Anexa3RegisterTotalLineTest {

    @Test
    void theTotalAgreesWithTheNumber() {
        assertThat(Anexa3RegisterGenerator.totalLine(1)).isEqualTo("1 formular emis.");
        assertThat(Anexa3RegisterGenerator.totalLine(2)).isEqualTo("2 formulare emise.");
        assertThat(Anexa3RegisterGenerator.totalLine(19)).isEqualTo("19 formulare emise.");
        assertThat(Anexa3RegisterGenerator.totalLine(20)).isEqualTo("20 de formulare emise.");
        assertThat(Anexa3RegisterGenerator.totalLine(101)).isEqualTo("101 formulare emise.");
        assertThat(Anexa3RegisterGenerator.totalLine(120)).isEqualTo("120 de formulare emise.");
        assertThat(Anexa3RegisterGenerator.totalLine(200)).isEqualTo("200 de formulare emise.");
    }
}
