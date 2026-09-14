package ro.ecoregistru;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.service.BillingCalculator;
import ro.ecoregistru.service.BillingCalculator.Invoice;
import ro.ecoregistru.service.BillingCalculator.Line;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Suma unei perioade de abonament — probele obligatorii din plata-abonamente.md §3, care sunt
 * exemplele din monetizare.md §3. Fiecare cifră de aici e o cifră spusă unui client la telefon.
 *
 * <p>Rândurile se verifică întregi (etichetă, cantitate, sumă), nu doar totalul: un total bun din
 * tranșe greșite (11 × 29 în loc de 10 × 29 + 1 × 25) tot ar da o factură pe care clientul o contestă.
 */
class BillingCalculatorTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 17);
    /** A doua perioadă, fără implementare: exemplele din monetizare.md sunt sume lunare. */
    private static final int NEXT = 1;

    @Test
    void aCabinetWithTwelveCompaniesPays539() {
        Invoice invoice = BillingCalculator.invoice(cabinet(), 0, 12, 0, NEXT);
        assertThat(invoice.total()).isEqualByComparingTo("539");
        assertThat(invoice.lines())
                .extracting(Line::label, Line::quantity, l -> l.amount().intValueExact())
                .containsExactly(
                        tuple("Abonament de cabinet", 1, 199),
                        tuple("Firmă gestionată, 1–10", 10, 290),
                        tuple("Firmă gestionată, 11–30", 2, 50));
    }

    @Test
    void aCabinetWithTwentyFiveCompaniesPays864() {
        assertThat(BillingCalculator.invoice(cabinet(), 0, 25, 0, NEXT).total()).isEqualByComparingTo("864");
    }

    /** 31 și peste: al treilea preț, pe restul. 199 + 290 + 500 + 5 × 19 + 3 × 15 ambalaje. */
    @Test
    void theThirdTierAndPackagingAreAddedPerCompany() {
        Invoice invoice = BillingCalculator.invoice(cabinet(), 0, 35, 3, NEXT);
        assertThat(invoice.lines())
                .extracting(Line::label, Line::quantity, l -> l.amount().intValueExact())
                .containsExactly(
                        tuple("Abonament de cabinet", 1, 199),
                        tuple("Firmă gestionată, 1–10", 10, 290),
                        tuple("Firmă gestionată, 11–30", 20, 500),
                        tuple("Firmă gestionată, 31 și peste", 5, 95),
                        tuple("Ambalaje, pe firmă", 3, 45));
        assertThat(invoice.total()).isEqualByComparingTo("1129");
    }

    @Test
    void aGeneratorWithThreeWorkPointsPays157() {
        Invoice invoice = BillingCalculator.invoice(direct(SubscriptionPlan.GENERATOR, false, START), 3, 0, 0, NEXT);
        assertThat(invoice.total()).isEqualByComparingTo("157");
        assertThat(invoice.lines())
                .extracting(Line::label, Line::quantity, l -> l.amount().intValueExact())
                .containsExactly(
                        tuple("Generator", 1, 99),
                        tuple("Punct de lucru în plus", 2, 58));
    }

    /**
     * Monetizare §3.1, coloana „Prima lună": 389 și 539, oricare ar fi ziua de start. Abonamentul
     * pornit pe 17 nu se împarte pe zile (decizia proprietarului, 15.09.2026).
     */
    @Test
    void theFirstPeriodIsFullPricePlusTheImplementationWhateverTheStartDay() {
        assertThat(BillingCalculator.invoice(direct(SubscriptionPlan.GENERATOR, false, START), 1, 0, 0, 0).total())
                .isEqualByComparingTo("389");
        assertThat(BillingCalculator.invoice(direct(SubscriptionPlan.GENERATOR_PACKAGING, false, START), 1, 0, 0, 0).total())
                .isEqualByComparingTo("539");
        assertThat(BillingCalculator.invoice(cabinet(), 0, 0, 0, 0).total())
                .isEqualByComparingTo("689");
    }

    /** Pornit pe 17.10: 17.10–16.11, apoi 17.11–16.12. */
    @Test
    void aPeriodRunsFromTheStartDayToTheDayBeforeItNextMonth() {
        Subscription s = direct(SubscriptionPlan.GENERATOR, false, START);
        Invoice first = BillingCalculator.invoice(s, 1, 0, 0, 0);
        Invoice second = BillingCalculator.invoice(s, 1, 0, 0, 1);
        assertThat(first.from()).isEqualTo("2026-10-17");
        assertThat(first.to()).isEqualTo("2026-11-16");
        assertThat(second.from()).isEqualTo("2026-11-17");
        assertThat(second.to()).isEqualTo("2026-12-16");
    }

    /**
     * Pornit pe 31 ianuarie, perioadele se numără de la data de start, nu din perioadă în perioadă:
     * februarie începe pe 28, dar martie se întoarce pe 31. Fără goluri și fără zile facturate de două ori.
     */
    @Test
    void aStartOnThe31stDoesNotDriftToThe28th() {
        Subscription s = direct(SubscriptionPlan.GENERATOR, false, LocalDate.of(2027, 1, 31));
        Invoice jan = BillingCalculator.invoice(s, 1, 0, 0, 0);
        Invoice feb = BillingCalculator.invoice(s, 1, 0, 0, 1);
        Invoice mar = BillingCalculator.invoice(s, 1, 0, 0, 2);
        assertThat(jan.to()).isEqualTo("2027-02-27");
        assertThat(feb.from()).isEqualTo("2027-02-28");
        assertThat(feb.to()).isEqualTo("2027-03-30");
        assertThat(mar.from()).isEqualTo("2027-03-31");
    }

    @Test
    void theImplementationIsNotBilledAgainInTheNextPeriod() {
        assertThat(BillingCalculator.invoice(direct(SubscriptionPlan.GENERATOR, false, START), 1, 0, 0, NEXT).total())
                .isEqualByComparingTo("99");
    }

    @Test
    void aFounderPaysNoImplementation() {
        Invoice invoice = BillingCalculator.invoice(direct(SubscriptionPlan.GENERATOR, true, START), 1, 0, 0, 0);
        assertThat(invoice.total()).isEqualByComparingTo("99");
        assertThat(invoice.lines()).extracting(Line::label)
                .containsExactly("Generator", "Implementare (client fondator, gratuită)");
    }

    @Test
    void fullServiceHasNoImplementationLine() {
        Invoice invoice = BillingCalculator.invoice(direct(SubscriptionPlan.FULL_SERVICE, false, START), 1, 0, 0, 0);
        assertThat(invoice.lines()).extracting(Line::label).containsExactly("Serviciu complet");
        assertThat(invoice.total()).isEqualByComparingTo("249");
    }

    @Test
    void thereIsNoPeriodBeforeTheStart() {
        assertThatThrownBy(() -> BillingCalculator.invoice(direct(SubscriptionPlan.GENERATOR, false, START), 1, 0, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Subscription direct(SubscriptionPlan plan, boolean founder, LocalDate start) {
        return Subscription.builder().plan(plan)
                .monthlyPrice(plan.monthlyPrice()).implementationFee(plan.implementationFee())
                .extraWorkPointPrice(SubscriptionPlan.EXTRA_WORK_POINT_PRICE)
                .founder(founder).startedAt(start).build();
    }

    private static Subscription cabinet() {
        SubscriptionPlan plan = SubscriptionPlan.CONSULTANCY;
        return Subscription.builder().plan(plan)
                .monthlyPrice(plan.monthlyPrice()).implementationFee(plan.implementationFee())
                .companyPriceTier1(SubscriptionPlan.COMPANY_PRICE_TIER1)
                .companyPriceTier2(SubscriptionPlan.COMPANY_PRICE_TIER2)
                .companyPriceTier3(SubscriptionPlan.COMPANY_PRICE_TIER3)
                .packagingCompanyPrice(SubscriptionPlan.PACKAGING_COMPANY_PRICE)
                .founder(false).startedAt(START).build();
    }
}
