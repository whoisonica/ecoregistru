package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.enums.SubscriptionStatus;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ConsultancyRepository;
import ro.ecoregistru.repository.SubscriptionInvoiceRepository;
import ro.ecoregistru.repository.SubscriptionRepository;
import ro.ecoregistru.service.BillingRunService;
import ro.ecoregistru.service.EmailService;
import ro.ecoregistru.service.FgoClient;
import ro.ecoregistru.service.NetopiaClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F4 din plata-abonamente.md, cu doar-citirea PORNITĂ: ce refuză filtrul și ce nu, cine e restricționat de
 * abonamentul cui, mementourile pe zile și oprirea abonamentului (§9.3). Cu flagul oprit, restul suitei rulează
 * exact ca înainte.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@TestPropertySource(properties = "app.billing.read-only-enabled=true")
class BillingReadOnlyIT {

    static final LocalDate START = LocalDate.of(2026, 10, 17);
    static final LocalDate DUE = START.plusDays(10);
    static final String REMINDER_MAIL = "mail/billing_reminder";
    static final String PARTNER = "{\"name\":\"Partener\",\"type\":\"COLLECTOR\"}";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired ConsultancyRepository consultancyRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired SubscriptionInvoiceRepository invoiceRepository;
    @Autowired BillingRunService billing;
    @Autowired TemplateEngine templateEngine;

    @MockBean EmailService emailService;
    @MockBean FgoClient fgo;
    @MockBean NetopiaClient netopia;

    @BeforeEach
    void setUp() {
        when(fgo.isConfigured()).thenReturn(true);
        when(fgo.emit(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            return new FgoClient.Issued("WH", id.substring(0, 8), "https://fgo.test/" + id + ".pdf", null);
        });
        when(fgo.status(any(), any())).thenAnswer(inv -> new FgoClient.Status(new BigDecimal("389"), BigDecimal.ZERO));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filtrul
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void aReadOnlyAccountReadsAndPaysButWritesNothing() throws Exception {
        Company company = company();
        subscription(company, SubscriptionStatus.READ_ONLY);
        AppUser admin = user(Role.ADMIN, company, null);
        AppUser operator = user(Role.OPERATOR, company, null);

        write(admin).andExpect(readOnlyRefusal());
        write(operator).andExpect(readOnlyRefusal());
        mockMvc.perform(get("/api/v1/partners").header("Authorization", bearer(operator)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/billing/payment-method").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMethod\":\"CARD\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/billing/access").header("Authorization", bearer(operator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("READ_ONLY")))
                .andExpect(jsonPath("$.readOnly", is(true)));
    }

    /** Platforma lucrează în continuare pe firmă: ea e cea care repară. */
    @Test
    void thePlatformIsNeverRestricted() throws Exception {
        Company company = company();
        subscription(company, SubscriptionStatus.READ_ONLY);
        AppUser platform = appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow();

        mockMvc.perform(post("/api/v1/partners").header("Authorization", bearer(platform))
                        .header("X-Tenant-Id", company.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(PARTNER))
                .andExpect(notReadOnlyRefusal());
    }

    @Test
    void anAccountNobodyPaysForOrThatIsOnlyLateIsNotRestricted() throws Exception {
        write(user(Role.ADMIN, company(), null)).andExpect(notReadOnlyRefusal());

        Company late = company();
        subscription(late, SubscriptionStatus.PAST_DUE);
        write(user(Role.ADMIN, late, null)).andExpect(notReadOnlyRefusal());
    }

    /** Cabinetul plătește pentru firmele lui: restanța lui le oprește pe toate, și pe consultant. */
    @Test
    void theCabinetsSubscriptionRestrictsItsCompaniesAndItsConsultants() throws Exception {
        Consultancy cabinet = consultancyRepository.save(Consultancy.builder()
                .name("Cabinet " + suffix()).cui(cui()).createdAt(Instant.now()).build());
        subscriptionRepository.save(Subscription.builder()
                .consultancy(cabinet).plan(SubscriptionPlan.CONSULTANCY).status(SubscriptionStatus.CANCELLED)
                .monthlyPrice(SubscriptionPlan.CONSULTANCY.monthlyPrice())
                .implementationFee(SubscriptionPlan.CONSULTANCY.implementationFee())
                .companyPriceTier1(SubscriptionPlan.COMPANY_PRICE_TIER1)
                .companyPriceTier2(SubscriptionPlan.COMPANY_PRICE_TIER2)
                .companyPriceTier3(SubscriptionPlan.COMPANY_PRICE_TIER3)
                .packagingCompanyPrice(SubscriptionPlan.PACKAGING_COMPANY_PRICE)
                .startedAt(START).createdAt(Instant.now()).build());
        Company managed = company();
        managed.setConsultancy(cabinet);
        companyRepository.save(managed);

        write(user(Role.ADMIN, managed, null)).andExpect(readOnlyRefusal());
        AppUser consultant = user(Role.CONSULTANT, null, cabinet);
        mockMvc.perform(post("/api/v1/partners").header("Authorization", bearer(consultant))
                        .header("X-Tenant-Id", managed.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(PARTNER))
                .andExpect(readOnlyRefusal());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rularea: stări și mementouri
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * §2.3 pe zile: +1 restant și primul mail, +8 avertismentul, +15 doar-citire și mailul lui — fiecare o dată.
     * Plata aduce contul înapoi la rularea următoare.
     */
    @Test
    void theRoadOfAnUnpaidInvoiceAndBack() throws Exception {
        Company company = company();
        Subscription s = subscription(company, SubscriptionStatus.PENDING);
        String to = billingEmail(company);
        AppUser operator = user(Role.OPERATOR, company, null);

        for (int day = 0; day <= 26; day++) {
            billing.run(START.plusDays(day));
            if (day == 11) {
                assertThat(statusOf(s)).isEqualTo(SubscriptionStatus.PAST_DUE);
            }
            if (day == 24) {
                assertThat(statusOf(s)).isEqualTo(SubscriptionStatus.PAST_DUE);
                write(operator).andExpect(notReadOnlyRefusal());
            }
        }
        assertThat(statusOf(s)).isEqualTo(SubscriptionStatus.READ_ONLY);
        write(operator).andExpect(readOnlyRefusal());

        verify(emailService, times(1)).send(eq(to), contains("a trecut de scadență"), eq(REMINDER_MAIL), any());
        verify(emailService, times(1)).send(eq(to), contains("în 7 zile"), eq(REMINDER_MAIL), any());
        verify(emailService, times(1)).send(eq(to), contains("doar pentru citire"), eq(REMINDER_MAIL), any());

        SubscriptionInvoice invoice = invoiceRepository.findAllBySubscription_IdOrderByPeriodStartDesc(s.getId()).get(0);
        when(fgo.status("WH", invoice.getFgoNumar())).thenReturn(new FgoClient.Status(new BigDecimal("389"), new BigDecimal("389")));
        billing.run(START.plusDays(27));

        assertThat(statusOf(s)).isEqualTo(SubscriptionStatus.ACTIVE);
        write(operator).andExpect(notReadOnlyRefusal());
    }

    /** Șablonul randat de-adevăratelea: avertismentul spune ziua în care contul se oprește. */
    @Test
    void theWarningMailNamesTheDayWritesStop() {
        Company company = company();
        subscription(company, SubscriptionStatus.PENDING);
        for (int day = 0; day <= 18; day++) {
            billing.run(START.plusDays(day));
        }

        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(emailService).send(eq(billingEmail(company)), contains("în 7 zile"), eq(REMINDER_MAIL), context.capture());
        String html = templateEngine.process(REMINDER_MAIL, context.getValue());
        assertThat(html).contains(company.getName(), "389 lei", DUE.plusDays(15).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                "/abonament");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // §9.3 — oprirea
    // ─────────────────────────────────────────────────────────────────────────

    /** Oprit cu ultima zi 16.11: perioada 17.11 nu se mai facturează, iar după 16.11 contul doar citește. */
    @Test
    void aStoppedSubscriptionIsBilledToItsLastDayThenOnlyReads() throws Exception {
        Company company = company();
        Subscription s = subscription(company, SubscriptionStatus.PENDING);
        billing.run(START);
        SubscriptionInvoice first = invoiceRepository.findAllBySubscription_IdOrderByPeriodStartDesc(s.getId()).get(0);
        when(fgo.status("WH", first.getFgoNumar())).thenReturn(new FgoClient.Status(new BigDecimal("389"), new BigDecimal("389")));
        Subscription reloaded = subscriptionRepository.findById(s.getId()).orElseThrow();
        reloaded.setEndsOn(LocalDate.of(2026, 11, 16));
        subscriptionRepository.save(reloaded);

        billing.run(LocalDate.of(2026, 11, 16));
        assertThat(statusOf(s)).isEqualTo(SubscriptionStatus.ACTIVE);
        billing.run(LocalDate.of(2026, 11, 17));

        assertThat(invoiceRepository.findAllBySubscription_IdOrderByPeriodStartDesc(s.getId())).hasSize(1);
        assertThat(statusOf(s)).isEqualTo(SubscriptionStatus.CANCELLED);
        write(user(Role.ADMIN, company, null)).andExpect(readOnlyRefusal());
    }

    @Test
    void thePlatformStopsAndTakesBackAStop() throws Exception {
        Company company = company();
        subscription(company, SubscriptionStatus.ACTIVE);
        AppUser platform = appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow();

        mockMvc.perform(post("/api/v1/subscriptions/company/" + company.getId() + "/cancel").header("Authorization", bearer(platform)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endsOn").isNotEmpty());
        mockMvc.perform(post("/api/v1/subscriptions/company/" + company.getId() + "/resume").header("Authorization", bearer(platform)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endsOn").doesNotExist());
        mockMvc.perform(post("/api/v1/subscriptions/company/" + company.getId() + "/cancel")
                        .header("Authorization", bearer(user(Role.ADMIN, company, null))))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private org.springframework.test.web.servlet.ResultActions write(AppUser user) throws Exception {
        return mockMvc.perform(post("/api/v1/partners").header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(PARTNER));
    }

    private static ResultMatcher readOnlyRefusal() {
        return result -> {
            status().isForbidden().match(result);
            jsonPath("$.error-code", is("subscription.read_only")).match(result);
        };
    }

    private static ResultMatcher notReadOnlyRefusal() {
        return result -> assertThat(result.getResponse().getContentAsString()).doesNotContain("subscription.read_only");
    }

    private SubscriptionStatus statusOf(Subscription s) {
        return subscriptionRepository.findById(s.getId()).orElseThrow().getStatus();
    }

    private Company company() {
        return companyRepository.save(Company.builder()
                .name("Firma " + suffix()).cui(cui())
                .type(CompanyType.GENERATOR).active(true).createdAt(Instant.now()).build());
    }

    private static String billingEmail(Company company) {
        return "facturi+" + company.getCui() + "@firma.ro";
    }

    private Subscription subscription(Company company, SubscriptionStatus status) {
        return subscriptionRepository.save(Subscription.builder()
                .company(company)
                .plan(SubscriptionPlan.GENERATOR)
                .status(status)
                .monthlyPrice(SubscriptionPlan.GENERATOR.monthlyPrice())
                .implementationFee(SubscriptionPlan.GENERATOR.implementationFee())
                .extraWorkPointPrice(SubscriptionPlan.EXTRA_WORK_POINT_PRICE)
                .startedAt(START)
                .createdAt(Instant.now())
                .billingEmail(billingEmail(company))
                .billingCounty("Cluj").billingCity("Cluj-Napoca").billingAddress("Str. Memorandumului nr. 1")
                .build());
    }

    private AppUser user(Role role, Company company, Consultancy consultancy) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + suffix() + "@firma.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(role).company(company).consultancy(consultancy)
                .enabled(true).createdAt(Instant.now()).build());
    }

    private String bearer(AppUser user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private static String cui() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
