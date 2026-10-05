package ro.ecoregistru.entity;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.enums.SizeTier;

import static org.assertj.core.api.Assertions.assertThat;

class SizeTierConverterTest {

    private final SizeTierConverter converter = new SizeTierConverter();

    @Test
    void nullStaysNullBothWays() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void tierMapsToItsNumberAndBack() {
        assertThat(converter.convertToDatabaseColumn(SizeTier.TIER_3)).isEqualTo(3);
        assertThat(converter.convertToEntityAttribute(3)).isEqualTo(SizeTier.TIER_3);
    }
}
