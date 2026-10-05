package ro.ecoregistru.enums;

import java.math.BigDecimal;

/**
 * Treapta de angajați a unui abonament de generator (grila din 05.10.2026). Clientul alege un
 * interval, nu declară un număr de angajați: doar treapta se păstrează. Lei, fără TVA.
 *
 * <p>Prețul de bază e al planului {@link SubscriptionPlan#GENERATOR}; cu ambalaje se adaugă
 * suplimentul treptei.
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

    /** 1–5, cum se salvează în coloana {@code size_tier}. */
    public int number() {
        return ordinal() + 1;
    }

    /** Intervalul de angajați, cum îl vede clientul (cu linioară de interval). */
    public String range() {
        return range;
    }

    public static SizeTier of(int number) {
        if (number < 1 || number > values().length) {
            throw new IllegalArgumentException("Treapta de angajați trebuie să fie între 1 și 5: " + number);
        }
        return values()[number - 1];
    }

    /** Doar generatorul și generatorul cu ambalaje au preț pe treaptă. */
    public BigDecimal monthlyPrice(SubscriptionPlan plan) {
        return switch (plan) {
            case GENERATOR -> basePrice;
            case GENERATOR_PACKAGING -> basePrice.add(packagingSupplement);
            default -> throw new IllegalArgumentException("Planul " + plan + " nu are preț pe treaptă");
        };
    }
}
