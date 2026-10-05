package ro.ecoregistru.enums;

import java.math.BigDecimal;

/**
 * What a client pays for. Lei, without VAT.
 *
 * <p>The company plans carry no price of their own since 05.10.2026: {@link #GENERATOR} and
 * {@link #GENERATOR_PACKAGING} are priced by their {@link SizeTier}, and {@link #FULL_SERVICE} by
 * the price the platform writes on the client. The consultant keeps the grid of 14.09.2026
 * (monetizare.md §3), in the constants below.
 *
 * <p>The price is read only when a subscription is created or changes plan, tier or custom price:
 * it is copied onto the subscription, so a grid changed here never reaches a client who already
 * signed. The subscriptions signed before 05.10.2026 keep their 99/149/249, implementation and
 * founder flag.
 *
 * <p>A collector is billed as {@link #GENERATOR}: the depot module is not sold yet.
 */
public enum SubscriptionPlan {

    GENERATOR("Generator"),
    GENERATOR_PACKAGING("Generator + Ambalaje"),
    /** The specialist keeps the records in the client's account. Priced on request. */
    FULL_SERVICE("Serviciu complet"),
    /** The only plan of a consultancy; its companies have no subscription of their own. */
    CONSULTANCY("Abonament de consultant");

    public static final BigDecimal CONSULTANCY_MONTHLY_PRICE = BigDecimal.valueOf(199);
    public static final BigDecimal CONSULTANCY_IMPLEMENTATION_FEE = BigDecimal.valueOf(490);
    public static final BigDecimal COMPANY_PRICE_TIER1 = BigDecimal.valueOf(29);
    public static final BigDecimal COMPANY_PRICE_TIER2 = BigDecimal.valueOf(25);
    public static final BigDecimal COMPANY_PRICE_TIER3 = BigDecimal.valueOf(19);
    public static final BigDecimal PACKAGING_COMPANY_PRICE = BigDecimal.valueOf(15);

    private final String label;

    SubscriptionPlan(String label) {
        this.label = label;
    }

    /** Printed as the first line of the invoice. */
    public String label() {
        return label;
    }

    public boolean forConsultancy() {
        return this == CONSULTANCY;
    }

    /** Priced by the employee-count tier ({@link SizeTier}), which a subscription on it must have. */
    public boolean forTiers() {
        return this == GENERATOR || this == GENERATOR_PACKAGING;
    }
}
