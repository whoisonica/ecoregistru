package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The subscription of a direct company or of a consultancy — never both (see {@code V43}).
 *
 * <p>The prices are the grid of the day it was created, copied from {@link SubscriptionPlan}. A
 * company without a row here is not billed and not restricted.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "subscriptions")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    Company company;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultancy_id")
    Consultancy consultancy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    SubscriptionStatus status;

    @Column(nullable = false)
    BigDecimal monthlyPrice;

    @Column(nullable = false)
    BigDecimal implementationFee;

    /** Companies only: each active work point after the first. */
    BigDecimal extraWorkPointPrice;

    /** Consultancies only: per managed company, 1–10 / 11–30 / 31 and over. */
    @Column(name = "company_price_tier1")
    BigDecimal companyPriceTier1;

    @Column(name = "company_price_tier2")
    BigDecimal companyPriceTier2;

    @Column(name = "company_price_tier3")
    BigDecimal companyPriceTier3;

    /** Consultancies only: per managed company that puts packaging on the market. */
    BigDecimal packagingCompanyPrice;

    /** One of the first 30 clients: no implementation fee, price locked for two years. */
    @Column(nullable = false)
    boolean founder;

    /** The first billed day. A first month that starts after the 1st is billed by the day. */
    @Column(nullable = false)
    LocalDate startedAt;

    // --- F2: who the invoice is addressed to (V44). Name and CUI come from the company or cabinet. ---

    /** Falls back to the company's contact email; a cabinet has none of its own. */
    String billingEmail;

    /** As FGO's county nomenclature spells it ("Bucuresti", "Cluj"). */
    String billingCounty;

    String billingCity;

    String billingAddress;

    @Column(nullable = false)
    Instant createdAt;
}
