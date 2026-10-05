package ro.ecoregistru.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ro.ecoregistru.enums.SizeTier;

/** The tier is stored as a number 1–5 ({@code size_tier SMALLINT}); {@code null} stays {@code null}. */
@Converter
public class SizeTierConverter implements AttributeConverter<SizeTier, Integer> {

    @Override
    public Integer convertToDatabaseColumn(SizeTier tier) {
        return tier == null ? null : tier.number();
    }

    @Override
    public SizeTier convertToEntityAttribute(Integer number) {
        return number == null ? null : SizeTier.of(number);
    }
}
