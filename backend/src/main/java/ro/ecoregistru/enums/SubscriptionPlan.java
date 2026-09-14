package ro.ecoregistru.enums;

import java.math.BigDecimal;

/**
 * What a client pays for, with the grid decided on 14.09.2026 (monetizare.md §3). Lei, without VAT.
 *
 * <p>The grid is read only when a subscription is created or changes plan: the prices are copied
 * onto the subscription, so a grid changed here never reaches a client who already signed.
 *
 * <p>A collector is billed as {@link #GENERATOR}: the depot module is not sold yet.
 */
public enum SubscriptionPlan {

    GENERATOR("Generator", 99, 290),
    GENERATOR_PACKAGING("Generator + Ambalaje", 149, 390),
    /** The specialist keeps the records in the client's account. Implementation included. */
    FULL_SERVICE("Serviciu complet", 249, 0),
    /** The only plan of a consultancy; its companies have no subscription of their own. */
    CONSULTANCY("Abonament de cabinet", 199, 490);

    public static final BigDecimal EXTRA_WORK_POINT_PRICE = BigDecimal.valueOf(29);
    public static final BigDecimal COMPANY_PRICE_TIER1 = BigDecimal.valueOf(29);
    public static final BigDecimal COMPANY_PRICE_TIER2 = BigDecimal.valueOf(25);
    public static final BigDecimal COMPANY_PRICE_TIER3 = BigDecimal.valueOf(19);
    public static final BigDecimal PACKAGING_COMPANY_PRICE = BigDecimal.valueOf(15);

    private final String label;
    private final BigDecimal monthlyPrice;
    private final BigDecimal implementationFee;

    SubscriptionPlan(String label, int monthlyPrice, int implementationFee) {
        this.label = label;
        this.monthlyPrice = BigDecimal.valueOf(monthlyPrice);
        this.implementationFee = BigDecimal.valueOf(implementationFee);
    }

    /** Printed as the first line of the invoice. */
    public String label() {
        return label;
    }

    public BigDecimal monthlyPrice() {
        return monthlyPrice;
    }

    public BigDecimal implementationFee() {
        return implementationFee;
    }

    public boolean forConsultancy() {
        return this == CONSULTANCY;
    }
}
