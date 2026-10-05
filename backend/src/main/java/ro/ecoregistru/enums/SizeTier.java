package ro.ecoregistru.enums;

import java.math.BigDecimal;

/**
 * The employee-count tier of a generator subscription (grid of 05.10.2026). The client picks an
 * interval instead of stating an employee count: only the tier is stored. Lei, without VAT.
 *
 * <p>The base price is that of {@link SubscriptionPlan#GENERATOR}; with packaging the tier's
 * supplement is added.
 */
public enum SizeTier {

    TIER_1("0–2", 35, 15),
    TIER_2("3–9", 50, 20),
    TIER_3("10–19", 75, 25),
    TIER_4("20–39", 100, 30),
    TIER_5("40+", 149, 50);

    private final String range;
    private final BigDecimal basePrice;
    private final BigDecimal packagingSupplement;

    SizeTier(String range, int basePrice, int packagingSupplement) {
        this.range = range;
        this.basePrice = BigDecimal.valueOf(basePrice);
        this.packagingSupplement = BigDecimal.valueOf(packagingSupplement);
    }

    /** 1–5, as stored in the {@code size_tier} column. */
    public int number() {
        return ordinal() + 1;
    }

    /** The employee interval as the client sees it (with an en dash). */
    public String range() {
        return range;
    }

    public static SizeTier of(int number) {
        if (number < 1 || number > values().length) {
            throw new IllegalArgumentException("Treapta de angajați trebuie să fie între 1 și 5: " + number);
        }
        return values()[number - 1];
    }

    /** Only the generator plans have a tier price; any other plan is refused. */
    public BigDecimal monthlyPrice(SubscriptionPlan plan) {
        return switch (plan) {
            case GENERATOR -> basePrice;
            case GENERATOR_PACKAGING -> basePrice.add(packagingSupplement);
            default -> throw new IllegalArgumentException("Planul " + plan + " nu are preț pe treaptă");
        };
    }
}
