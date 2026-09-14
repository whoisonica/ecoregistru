package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.enums.SubscriptionStatus;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.SubscriptionInvoiceRepository;
import ro.ecoregistru.repository.SubscriptionRepository;
import ro.ecoregistru.service.BillingRunService;
import ro.ecoregistru.service.EmailService;
import ro.ecoregistru.service.FgoClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F2 din plata-abonamente.md — rularea zilnică a facturării, cu FGO înlocuit de un mock. Ce acceptă
 * FGO de fapt se probează pe api-testuat, cu cheia; aici e ce ține de noi: o singură factură pe
 * perioadă oricâte rulări ar fi, reluarea cu același `IdExtern` după o cădere, datele de facturare
 * lipsă oprite înainte de FGO, starea abonamentului din plăți.
 *
 * <p>Toate testele rulează pe aceeași bază, deci fiecare își caută factura după firma lui.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class BillingRunIT {

    static final LocalDate START = LocalDate.of(2026, 10, 17);

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired SubscriptionInvoiceRepository invoiceRepository;
    @Autowired BillingRunService billing;

    @MockBean EmailService emailService;
    @MockBean FgoClient fgo;

    @BeforeEach
    void setUp() {
        when(fgo.isConfigured()).thenReturn(true);
        when(fgo.emit(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> issued(inv.getArgument(0)));
        when(fgo.status(any(), any())).thenReturn(new FgoClient.Status(new BigDecimal("389"), BigDecimal.ZERO));
    }

    /** Generator fără puncte de lucru în plus: 99 + implementarea 290. */
    @Test
    void aSecondRunOnTheSameDayIssuesNothingMore() {
        Company company = company();
        Subscription s = subscription(company, true);

        billing.run(START);
        billing.run(START);

        assertThat(invoices(s)).singleElement().satisfies(i -> {
            assertThat(i.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
            assertThat(i.getTotal()).isEqualByComparingTo("389");
            assertThat(i.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 11, 16));
            assertThat(i.getDueDate()).isEqualTo(START.plusDays(10));
            assertThat(i.getFgoNumar()).isNotBlank();
        });
        verify(fgo, times(1)).emit(any(), argThat(b -> b != null && b.name().equals(company.getName())),
                any(), eq(START), eq(START.plusDays(10)), any());
    }

    @Test
    void theNextPeriodIsInvoicedOnItsFirstDayWithoutTheImplementation() {
        Company company = company();
        Subscription s = subscription(company, true);

        billing.run(START);
        billing.run(LocalDate.of(2026, 11, 16));
        assertThat(invoices(s)).hasSize(1);

        billing.run(LocalDate.of(2026, 11, 17));
        assertThat(invoices(s)).hasSize(2).first().satisfies(i -> {
            assertThat(i.getPeriodStart()).isEqualTo(LocalDate.of(2026, 11, 17));
            assertThat(i.getTotal()).isEqualByComparingTo("99");
        });
    }

    /** FGO cade: rândul rămâne DRAFT cu motivul, iar reluarea trimite același IdExtern, nu o factură nouă. */
    @Test
    void aFailedIssueIsRetriedWithTheSameExternalId() {
        Company company = company();
        Subscription s = subscription(company, true);
        // doThrow().when(), nu when(): forma cealaltă apelează mock-ul cu argumente nule chiar la stubbing.
        doThrow(new FgoClient.FgoException("FGO /factura/emitere: indisponibil"))
                .doAnswer(inv -> issued(inv.getArgument(0)))
                .when(fgo).emit(any(), argThat(b -> b != null && b.name().equals(company.getName())),
                        any(), any(), any(), any());

        billing.run(START);
        SubscriptionInvoice draft = invoices(s).get(0);
        assertThat(draft.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(draft.getLastError()).contains("indisponibil");

        billing.run(START.plusDays(1));
        assertThat(invoices(s)).singleElement().satisfies(i -> {
            assertThat(i.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
            assertThat(i.getLastError()).isNull();
        });

        ArgumentCaptor<String> externalIds = ArgumentCaptor.forClass(String.class);
        verify(fgo, times(2)).emit(externalIds.capture(),
                argThat(b -> b != null && b.name().equals(company.getName())), any(), any(), any(), any());
        assertThat(externalIds.getAllValues()).containsOnly(draft.getId().toString());
    }

    @Test
    void withoutAnAddressNothingReachesFgo() {
        Company company = company();
        Subscription s = subscription(company, false);

        billing.run(START);

        assertThat(invoices(s)).singleElement().satisfies(i -> {
            assertThat(i.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
            assertThat(i.getLastError()).contains("județul", "localitatea", "adresa");
        });
        verify(fgo, never()).emit(any(), argThat(b -> b != null && b.name().equals(company.getName())),
                any(), any(), any(), any());
    }

    @Test
    void anUnpaidInvoicePastItsDueDateIsPastDueUntilFgoSeesThePayment() {
        Subscription s = subscription(company(), true);

        billing.run(START);
        assertThat(subscriptionStatus(s)).isEqualTo(SubscriptionStatus.PENDING);

        billing.run(START.plusDays(10));
        assertThat(subscriptionStatus(s)).isEqualTo(SubscriptionStatus.PENDING);

        billing.run(START.plusDays(11));
        assertThat(subscriptionStatus(s)).isEqualTo(SubscriptionStatus.PAST_DUE);

        SubscriptionInvoice invoice = invoices(s).get(0);
        when(fgo.status("WH", invoice.getFgoNumar()))
                .thenReturn(new FgoClient.Status(new BigDecimal("389"), new BigDecimal("389")));
        billing.run(START.plusDays(12));

        assertThat(invoices(s).get(0).getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(subscriptionStatus(s)).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void withoutFgoKeysNothingIsReserved() {
        when(fgo.isConfigured()).thenReturn(false);
        Subscription s = subscription(company(), true);

        assertThat(billing.run(START).configured()).isFalse();
        assertThat(invoices(s)).isEmpty();
    }

    /** Garda lipsă din F1: cu facturi, abonamentul e singura legătură cu ele în FGO. */
    @Test
    void aSubscriptionWithInvoicesCannotBeDeleted() throws Exception {
        Company company = company();
        Subscription s = subscription(company, true);
        billing.run(START);

        String platformToken = jwtService.generateToken(
                appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow());
        mockMvc.perform(delete("/api/v1/subscriptions/company/" + company.getId())
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error-code", is("subscription.has.invoices")));
        assertThat(subscriptionRepository.findById(s.getId())).isPresent();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static FgoClient.Issued issued(String externalId) {
        return new FgoClient.Issued("WH", externalId.substring(0, 8), "https://fgo.test/" + externalId + ".pdf", null);
    }

    private List<SubscriptionInvoice> invoices(Subscription s) {
        return invoiceRepository.findAllBySubscription_IdOrderByPeriodStartDesc(s.getId());
    }

    private SubscriptionStatus subscriptionStatus(Subscription s) {
        return subscriptionRepository.findById(s.getId()).orElseThrow().getStatus();
    }

    private Company company() {
        return companyRepository.save(Company.builder()
                .name("Firma " + UUID.randomUUID().toString().substring(0, 8))
                .cui(String.valueOf(ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L)))
                .type(CompanyType.GENERATOR).active(true).createdAt(Instant.now()).build());
    }

    private Subscription subscription(Company company, boolean withAddress) {
        Subscription.SubscriptionBuilder b = Subscription.builder()
                .company(company)
                .plan(SubscriptionPlan.GENERATOR)
                .status(SubscriptionStatus.PENDING)
                .monthlyPrice(SubscriptionPlan.GENERATOR.monthlyPrice())
                .implementationFee(SubscriptionPlan.GENERATOR.implementationFee())
                .extraWorkPointPrice(SubscriptionPlan.EXTRA_WORK_POINT_PRICE)
                .startedAt(START)
                .createdAt(Instant.now())
                .billingEmail("facturi@firma.ro");
        if (withAddress) {
            b.billingCounty("Cluj").billingCity("Cluj-Napoca").billingAddress("Str. Memorandumului nr. 1");
        }
        return subscriptionRepository.save(b.build());
    }
}
