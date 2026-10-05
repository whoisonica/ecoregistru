package ro.ecoregistru;

import org.junit.jupiter.api.Test;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.enums.SizeTier;
import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.service.BillingCalculator;
import ro.ecoregistru.service.BillingCalculator.Invoice;
import ro.ecoregistru.service.BillingCalculator.Line;

import java.math.BigDecimal;
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
    void aConsultancyWithTwelveCompaniesPays539() {
        Invoice invoice = BillingCalculator.invoice(consultancy(), 0, 12, 0, NEXT);
        assertThat(invoice.total()).isEqualByComparingTo("539");
        assertThat(invoice.lines())
                .extracting(Line::label, Line::quantity, l -> l.amount().intValueExact())
                .containsExactly(
                        tuple("Abonament de consultant", 1, 199),
                        tuple("Firmă gestionată, 1–10", 10, 290),
                        tuple("Firmă gestionată, 11–30", 2, 50));
    }

    @Test
    void aConsultancyWithTwentyFiveCompaniesPays864() {
        assertThat(BillingCalculator.invoice(consultancy(), 0, 25, 0, NEXT).total()).isEqualByComparingTo("864");
    }

    /** 31 și peste: al treilea preț, pe restul. 199 + 290 + 500 + 5 × 19 + 3 × 15 ambalaje. */
    @Test
    void theThirdTierAndPackagingAreAddedPerCompany() {
        Invoice invoice = BillingCalculator.invoice(consultancy(), 0, 35, 3, NEXT);
        assertThat(invoice.lines())
                .extracting(Line::label, Line::quantity, l -> l.amount().intValueExact())
                .containsExactly(
                        tuple("Abonament de consultant", 1, 199),
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
        assertThat(BillingCalculator.invoice(consultancy(), 0, 0, 0, 0).total())
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

    /** Contract art. 4.3: cu angajament, prima factură arată implementarea gratuită, nu o încasează. */
    @Test
    void aTwelveMonthCommitmentPaysNoImplementationUpFront() {
        Invoice invoice = BillingCalculator.invoice(committed(SubscriptionPlan.GENERATOR, null), 1, 0, 0, 0);
        assertThat(invoice.total()).isEqualByComparingTo("99");
        assertThat(invoice.lines()).extracting(Line::label)
                .containsExactly("Generator", "Implementare (angajament 12 luni, gratuită)");
    }

    /**
     * Oprit în a cincea lună: ultima perioadă facturată poartă implementarea, o singură dată, iar
     * perioadele dinainte rămân la 99. Lunile rămase până la 12 nu apar nicăieri.
     */
    @Test
    void stoppingBeforeTwelveMonthsBillsTheImplementationOnTheLastInvoice() {
        LocalDate lastDay = BillingCalculator.periodStart(committed(SubscriptionPlan.GENERATOR, null), 5).minusDays(1);
        Subscription s = committed(SubscriptionPlan.GENERATOR, lastDay);
        assertThat(BillingCalculator.invoice(s, 1, 0, 0, 3).total()).isEqualByComparingTo("99");
        Invoice last = BillingCalculator.invoice(s, 1, 0, 0, 4);
        assertThat(last.to()).isEqualTo(lastDay);
        assertThat(last.total()).isEqualByComparingTo("389");
        assertThat(last.lines()).extracting(Line::label)
                .containsExactly("Generator", "Implementare (oprire înainte de 12 luni)");
    }

    /** Oprit cu a 11-a lună ca ultimă: încă înainte de termen. Hotarul de lângă cel de mai jos. */
    @Test
    void stoppingAfterElevenMonthsStillBillsTheImplementation() {
        Subscription probe = committed(SubscriptionPlan.GENERATOR, null);
        Subscription s = committed(SubscriptionPlan.GENERATOR,
                BillingCalculator.periodStart(probe, BillingCalculator.COMMITMENT_PERIODS - 1).minusDays(1));
        assertThat(BillingCalculator.invoice(s, 1, 0, 0, BillingCalculator.COMMITMENT_PERIODS - 2).total())
                .isEqualByComparingTo("389");
    }

    @Test
    void stoppingAfterTwelveMonthsBillsNoImplementation() {
        Subscription probe = committed(SubscriptionPlan.GENERATOR, null);
        Subscription s = committed(SubscriptionPlan.GENERATOR,
                BillingCalculator.periodStart(probe, BillingCalculator.COMMITMENT_PERIODS).minusDays(1));
        assertThat(BillingCalculator.invoice(s, 1, 0, 0, BillingCalculator.COMMITMENT_PERIODS - 1).total())
                .isEqualByComparingTo("99");
    }

    /** Proba negativă a regulii: fără angajament, oprirea timpurie nu mai adaugă nimic pe ultima factură. */
    @Test
    void withoutCommitmentAnEarlyStopAddsNothing() {
        Subscription s = direct(SubscriptionPlan.GENERATOR, false, START);
        s.setEndsOn(BillingCalculator.periodStart(s, 5).minusDays(1));
        assertThat(BillingCalculator.invoice(s, 1, 0, 0, 4).total()).isEqualByComparingTo("99");
    }

    @Test
    void aCommittedFounderPaysNoImplementationEvenWhenStoppingEarly() {
        Subscription s = committed(SubscriptionPlan.GENERATOR, null);
        s.setFounder(true);
        s.setEndsOn(BillingCalculator.periodStart(s, 3).minusDays(1));
        assertThat(BillingCalculator.invoice(s, 1, 0, 0, 2).total()).isEqualByComparingTo("99");
    }

    @Test
    void aCommittedConsultancyPaysTheStartOnlyWhenStoppingEarly() {
        Subscription s = consultancy();
        s.setTwelveMonthCommitment(true);
        assertThat(BillingCalculator.invoice(s, 0, 5, 0, 0).total()).isEqualByComparingTo("344");
        s.setEndsOn(BillingCalculator.periodStart(s, 2).minusDays(1));
        assertThat(BillingCalculator.invoice(s, 0, 5, 0, 1).lines()).extracting(Line::label)
                .contains("Pornirea contului de consultant (oprire înainte de 12 luni)");
        assertThat(BillingCalculator.invoice(s, 0, 5, 0, 1).total()).isEqualByComparingTo("834");
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

    @Test
    void aTierTwoGeneratorWithThreeWorkPointsHasOneLine() {
        Invoice invoice = BillingCalculator.invoice(tiered(SubscriptionPlan.GENERATOR, SizeTier.TIER_2), 3, 0, 0, 0);
        assertThat(invoice.lines())
                .extracting(Line::label, Line::quantity, l -> l.amount().intValueExact())
                .containsExactly(tuple("Generator, treapta 2 (3–9 angajați)", 1, 50));
        assertThat(invoice.total()).isEqualByComparingTo("50");
    }

    @Test
    void packagingOnTierFiveIsLabelled() {
        Invoice invoice = BillingCalculator.invoice(
                tiered(SubscriptionPlan.GENERATOR_PACKAGING, SizeTier.TIER_5), 1, 0, 0, 0);
        assertThat(invoice.lines())
                .extracting(Line::label, Line::quantity, l -> l.amount().intValueExact())
                .containsExactly(tuple("Generator + Ambalaje, treapta 5 (40+ angajați)", 1, 199));
        assertThat(invoice.total()).isEqualByComparingTo("199");
    }

    @Test
    void aCustomPriceHasNoTierInTheLabel() {
        Subscription s = tiered(SubscriptionPlan.GENERATOR, SizeTier.TIER_2);
        s.setCustomPrice(true);
        s.setMonthlyPrice(BigDecimal.valueOf(42));
        Invoice invoice = BillingCalculator.invoice(s, 1, 0, 0, 0);
        assertThat(invoice.lines())
                .extracting(Line::label, Line::quantity, l -> l.amount().intValueExact())
                .containsExactly(tuple("Generator", 1, 42));
    }

    @Test
    void aZeroPricedLineIsLeftOut() {
        Subscription s = direct(SubscriptionPlan.GENERATOR, false, START);
        s.setExtraWorkPointPrice(BigDecimal.ZERO);
        assertThat(BillingCalculator.invoice(s, 3, 0, 0, NEXT).lines())
                .extracting(Line::label)
                .containsExactly("Generator");
    }

    /** A subscription saved on the tier grid: no implementation fee, extra work points free. */
    private static Subscription tiered(SubscriptionPlan plan, SizeTier tier) {
        return Subscription.builder().plan(plan).sizeTier(tier)
                .monthlyPrice(tier.monthlyPrice(plan)).implementationFee(BigDecimal.ZERO)
                .extraWorkPointPrice(BigDecimal.ZERO)
                .founder(false).startedAt(START).build();
    }

    /** Un abonament semnat pe grila din 14.09.2026, dinainte de trepte: prețurile lui rămân pe rând. */
    private static Subscription direct(SubscriptionPlan plan, boolean founder, LocalDate start) {
        int[] prices = switch (plan) {
            case GENERATOR -> new int[]{99, 290};
            case GENERATOR_PACKAGING -> new int[]{149, 390};
            default -> new int[]{249, 0};
        };
        return Subscription.builder().plan(plan)
                .monthlyPrice(BigDecimal.valueOf(prices[0])).implementationFee(BigDecimal.valueOf(prices[1]))
                .extraWorkPointPrice(BigDecimal.valueOf(29))
                .founder(founder).startedAt(start).build();
    }

    private static Subscription committed(SubscriptionPlan plan, LocalDate endsOn) {
        Subscription s = direct(plan, false, START);
        s.setTwelveMonthCommitment(true);
        s.setEndsOn(endsOn);
        return s;
    }

    private static Subscription consultancy() {
        return Subscription.builder().plan(SubscriptionPlan.CONSULTANCY)
                .monthlyPrice(SubscriptionPlan.CONSULTANCY_MONTHLY_PRICE)
                .implementationFee(SubscriptionPlan.CONSULTANCY_IMPLEMENTATION_FEE)
                .companyPriceTier1(SubscriptionPlan.COMPANY_PRICE_TIER1)
                .companyPriceTier2(SubscriptionPlan.COMPANY_PRICE_TIER2)
                .companyPriceTier3(SubscriptionPlan.COMPANY_PRICE_TIER3)
                .packagingCompanyPrice(SubscriptionPlan.PACKAGING_COMPANY_PRICE)
                .founder(false).startedAt(START).build();
    }
}
