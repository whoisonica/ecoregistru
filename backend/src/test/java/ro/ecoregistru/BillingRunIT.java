package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.BillingRun;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.SubscriptionPaymentMethod;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F2 din plata-abonamente.md — rularea zilnică a facturării, cu FGO înlocuit de un mock. Ce acceptă
 * FGO de fapt se probează pe api-testuat, cu cheia; aici e ce ține de noi: o singură factură pe
 * perioadă oricâte rulări ar fi, reluarea cu același `IdExtern` după o cădere, datele de facturare
 * lipsă oprite înainte de FGO, starea abonamentului din plăți, mailul cu factura (§9.4) și ce vede
 * clientul pe `/abonament`.
 *
 * <p>Toate testele rulează pe aceeași bază, deci fiecare își caută factura după firma lui, iar mailul
 * după adresa de facturare, unică pe firmă.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class BillingRunIT {

    static final LocalDate START = LocalDate.of(2026, 10, 17);
    static final String INVOICE_MAIL = "mail/subscription_invoice";

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

    @MockitoBean EmailService emailService;
    @MockitoBean FgoClient fgo;

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

    /** WH 1, ONSIA S.R.L., 17.09.2026: adresa întreagă de la ANAF ajungea la FGO lângă județ și localitate. */
    @Test
    void theAnafAddressReachesFgoWithoutTheCountyAndTheLocalityAgain() {
        Company company = company();
        Subscription s = subscription(company, false);
        s.setBillingCounty("Bihor");
        s.setBillingCity("Sat Sântandrei Com. Sântandrei");
        s.setBillingAddress("JUD. BIHOR, SAT SÂNTANDREI COM. SÂNTANDREI, STR. FĂCLIEI, NR.79");
        subscriptionRepository.save(s);
        doAnswer(inv -> issued(inv.getArgument(0)))
                .when(fgo).emit(any(), argThat(b -> b != null && b.name().equals(company.getName())),
                        any(), any(), any(), any());

        billing.run(START);

        verify(fgo).emit(any(), argThat(b -> b != null && b.name().equals(company.getName())
                        && "Bihor".equals(b.county()) && "Sântandrei".equals(b.city())
                        && "STR. FĂCLIEI, NR.79".equals(b.address())),
                any(), any(), any(), eq("Abonament WasteHouse, perioada 17.10.2026 - 16.11.2026."));
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

    /**
     * Testul de pe producție din 15.09: „1 căzute" era altă firmă, iar abonamentul care începea pe 22.09
     * lipsea din cifre. Rezultatul le numește pe amândouă.
     */
    @Test
    void theResultNamesWhoFailedWhyAndWhoStartsLater() {
        Company noAddress = company();
        subscription(noAddress, false);
        Company later = company();
        Subscription upcoming = subscription(later, true);
        upcoming.setStartedAt(START.plusDays(5));
        subscriptionRepository.save(upcoming);

        BillingRunService.Result result = billing.run(START);

        assertThat(result.failures()).filteredOn(f -> f.client().equals(noAddress.getName()))
                .singleElement().satisfies(f -> assertThat(f.reason()).contains("județul", "adresa"));
        assertThat(result.failed()).isEqualTo(result.failures().size());
        assertThat(result.notStarted()).filteredOn(n -> n.client().equals(later.getName()))
                .singleElement().satisfies(n -> assertThat(n.startsOn()).isEqualTo(START.plusDays(5)));
        assertThat(result.notStarted()).noneMatch(n -> n.client().equals(noAddress.getName()));
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

    /** Contract art. 13.3: when the contract ends, the saved card goes with it. */
    @Test
    void theSavedCardIsForgottenOnceTheLastPeriodIsOver() {
        Subscription s = subscription(company(), true);
        s.setPaymentMethod(SubscriptionPaymentMethod.CARD);
        s.setCardToken("tok-" + suffix());
        s.setCardPanMasked("****1234");
        s.setCardExpiry("12/2029");
        s.setEndsOn(START.plusDays(40));
        subscriptionRepository.save(s);

        billing.run(START.plusDays(40));
        assertThat(subscriptionRepository.findById(s.getId()).orElseThrow().getCardToken()).isNotNull();

        billing.run(START.plusDays(41));
        assertThat(subscriptionRepository.findById(s.getId()).orElseThrow()).satisfies(ended -> {
            assertThat(ended.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(ended.getCardToken()).isNull();
            assertThat(ended.getCardPanMasked()).isNull();
            assertThat(ended.getCardExpiry()).isNull();
        });
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

        mockMvc.perform(delete("/api/v1/subscriptions/company/" + company.getId())
                        .header("Authorization", "Bearer " + platformToken()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error-code", is("subscription.has.invoices")));
        assertThat(subscriptionRepository.findById(s.getId())).isPresent();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // §9.4 — mailul cu factura, trimis de aplicație
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void theIssuedInvoiceIsMailedOnceWithItsNumberInTheSubject() {
        Company company = company();
        Subscription s = subscription(company, true);

        billing.run(START);
        billing.run(START.plusDays(1));

        SubscriptionInvoice invoice = invoices(s).get(0);
        verify(emailService, times(1)).send(eq(billingEmail(company)), contains("WH " + invoice.getFgoNumar()),
                eq(INVOICE_MAIL), any());
        assertThat(invoice.getEmailedAt()).isNotNull();
    }

    /** Șablonul randat de-adevăratelea: EmailService e mock, deci altfel o expresie greșită cade abia în producție. */
    @Test
    void theInvoiceMailCarriesTheNumberTheTotalAndThePdfLink() {
        Company company = company();
        Subscription s = subscription(company, true);
        billing.run(START);
        SubscriptionInvoice invoice = invoices(s).get(0);

        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(emailService).send(eq(billingEmail(company)), any(), eq(INVOICE_MAIL), context.capture());
        String html = templateEngine.process(INVOICE_MAIL, context.getValue());

        assertThat(html).contains("WH " + invoice.getFgoNumar(), company.getName(), "389 lei",
                "17.10.2026 – 16.11.2026", "27.10.2026", invoice.getFgoLink(), "/abonament");
        assertThat(html).doesNotContain("Plătește online");
    }

    /** Serverul de mail cade: factura rămâne nemarcată și pleacă la rularea următoare. */
    @Test
    void aMailThatFailsIsSentAgainOnTheNextRun() {
        Company company = company();
        Subscription s = subscription(company, true);
        String to = billingEmail(company);
        doThrow(new RuntimeException("SMTP indisponibil")).doNothing()
                .when(emailService).send(eq(to), any(), eq(INVOICE_MAIL), any());

        billing.run(START);
        assertThat(invoices(s).get(0).getEmailedAt()).isNull();

        billing.run(START.plusDays(1));
        assertThat(invoices(s).get(0).getEmailedAt()).isNotNull();
        verify(emailService, times(2)).send(eq(to), any(), eq(INVOICE_MAIL), any());
    }

    /** Fără email de facturare și fără emailul de contact al firmei, factura așteaptă o adresă. */
    @Test
    void withoutAnEmailTheInvoiceWaitsUntilOneIsSet() {
        Company company = company();
        Subscription s = subscription(company, true, null);

        billing.run(START);
        assertThat(invoices(s).get(0).getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(invoices(s).get(0).getEmailedAt()).isNull();

        Subscription reloaded = subscriptionRepository.findById(s.getId()).orElseThrow();
        reloaded.setBillingEmail(billingEmail(company));
        subscriptionRepository.save(reloaded);
        billing.run(START.plusDays(1));

        assertThat(invoices(s).get(0).getEmailedAt()).isNotNull();
        verify(emailService, times(1)).send(eq(billingEmail(company)), any(), eq(INVOICE_MAIL), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // /abonament — ce vede cine plătește
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void anAdminSeesTheirCompanysIssuedInvoices() throws Exception {
        Company company = company();
        Subscription s = subscription(company, true);
        billing.run(START);
        String numar = invoices(s).get(0).getFgoNumar();

        mockMvc.perform(get("/api/v1/billing").header("Authorization", "Bearer " + token(admin(company))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName", is(company.getName())))
                .andExpect(jsonPath("$.plan", is("GENERATOR")))
                .andExpect(jsonPath("$.billingEmail", is(billingEmail(company))))
                .andExpect(jsonPath("$.nextInvoice.total").exists())
                .andExpect(jsonPath("$.invoices", hasSize(1)))
                .andExpect(jsonPath("$.invoices[0].fgoNumar", is(numar)))
                .andExpect(jsonPath("$.invoices[0].fgoLink").exists());
    }

    /** O factură neemisă e a noastră, cu tot cu eroarea FGO de pe ea: clientul nu le vede. */
    @Test
    void anInvoiceNotYetIssuedAndItsErrorStayWithThePlatform() throws Exception {
        Company company = company();
        subscription(company, false);
        billing.run(START);

        mockMvc.perform(get("/api/v1/billing").header("Authorization", "Bearer " + token(admin(company))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoices", hasSize(0)))
                .andExpect(content().string(not(containsString("Lipsesc"))));
    }

    /** Consultantul plătește abonamentul cabinetului; firma din cabinet n-are ce plăti; operatorul n-are acces. */
    @Test
    void theCabinetPaysForItsCompanies() throws Exception {
        Consultancy cabinet = consultancyRepository.save(Consultancy.builder()
                .name("Cabinet " + suffix()).cui(cui()).createdAt(Instant.now()).build());
        subscriptionRepository.save(Subscription.builder()
                .consultancy(cabinet)
                .plan(SubscriptionPlan.CONSULTANCY)
                .status(SubscriptionStatus.PENDING)
                .monthlyPrice(SubscriptionPlan.CONSULTANCY.monthlyPrice())
                .implementationFee(SubscriptionPlan.CONSULTANCY.implementationFee())
                .companyPriceTier1(SubscriptionPlan.COMPANY_PRICE_TIER1)
                .companyPriceTier2(SubscriptionPlan.COMPANY_PRICE_TIER2)
                .companyPriceTier3(SubscriptionPlan.COMPANY_PRICE_TIER3)
                .packagingCompanyPrice(SubscriptionPlan.PACKAGING_COMPANY_PRICE)
                .startedAt(START)
                .createdAt(Instant.now())
                .build());
        Company managed = company();
        managed.setConsultancy(cabinet);
        companyRepository.save(managed);

        AppUser consultant = user(Role.CONSULTANT, null, cabinet);
        mockMvc.perform(get("/api/v1/billing").header("Authorization", "Bearer " + token(consultant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName", is(cabinet.getName())))
                .andExpect(jsonPath("$.plan", is("CONSULTANCY")));

        mockMvc.perform(get("/api/v1/billing").header("Authorization", "Bearer " + token(admin(managed))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/billing")
                        .header("Authorization", "Bearer " + token(user(Role.OPERATOR, managed, null))))
                .andExpect(status().isForbidden());
    }

    // ─── F-A: ecranul Facturare ──────────────────────────────────────────────

    /**
     * 17.09.2026: rularea de la 06:30 căzuse pe două firme, iar singurul loc unde se vedea era logul. Rularea își
     * scrie acum rezultatul, cu firma, motivul pe limba omului și clientul de deschis.
     */
    @Test
    void theRunIsSavedWithWhoFailedWhyAndWhereToFixIt() throws Exception {
        Company company = company();
        subscription(company, true);
        doThrow(new FgoClient.FgoException("FGO /factura/emitere: Client[CodUnic] are format invalid"))
                .when(fgo).emit(any(), argThat(b -> b != null && b.name().equals(company.getName())),
                        any(), any(), any(), any());
        AppUser platform = appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow();

        billing.run(START, BillingRun.Kind.MANUAL, platform.getId());

        mockMvc.perform(get("/api/v1/subscriptions/billing/runs/last")
                        .header("Authorization", "Bearer " + platformToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind", is("MANUAL")))
                .andExpect(jsonPath("$.result.failures[?(@.client == '" + company.getName() + "')].reason",
                        hasItem(containsString("CUI-ul „" + company.getCui() + "” nu e valid"))))
                .andExpect(jsonPath("$.result.failures[?(@.client == '" + company.getName() + "')].owner.id",
                        hasItem(company.getId().toString())));
        assertThat(invoices(subscriptionRepository.findByCompany_Id(company.getId()).orElseThrow()))
                .singleElement().satisfies(i -> assertThat(i.getLastError()).startsWith("CUI-ul"));
    }

    @Test
    void theRunNamesTheInvoicesItIssuedAndThoseFoundPaid() {
        Company company = company();
        Subscription s = subscription(company, true);

        BillingRunService.Result first = billing.run(START);
        assertThat(first.issuedInvoices()).filteredOn(d -> d.client().equals(company.getName()))
                .singleElement().satisfies(d -> assertThat(d.number()).startsWith("WH "));

        when(fgo.status("WH", invoices(s).get(0).getFgoNumar()))
                .thenReturn(new FgoClient.Status(new BigDecimal("389"), new BigDecimal("389")));
        assertThat(billing.run(START.plusDays(1)).paidInvoices())
                .anyMatch(d -> d.client().equals(company.getName()));
    }

    @Test
    void everyClientsInvoicesAreListedForThePlatformOnly() throws Exception {
        Company company = company();
        Subscription s = subscription(company, true);
        billing.run(START);

        mockMvc.perform(get("/api/v1/subscriptions/invoices?filter=ALL&size=200").header("Authorization", "Bearer " + platformToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + invoices(s).get(0).getId() + "')].client",
                        hasItem(company.getName())))
                .andExpect(jsonPath("$.content[?(@.id == '" + invoices(s).get(0).getId() + "')].ownerKind",
                        hasItem("company")));

        mockMvc.perform(get("/api/v1/subscriptions/invoices").header("Authorization", "Bearer " + token(admin(company))))
                .andExpect(status().isForbidden());
    }

    /** „Verifică plata acum”: o singură factură, fără rularea întreagă, cu ora la care a răspuns FGO. */
    @Test
    void thePaymentOfOneInvoiceIsCheckedOnRequest() throws Exception {
        Subscription s = subscription(company(), true);
        billing.run(START);
        SubscriptionInvoice invoice = invoices(s).get(0);
        when(fgo.status("WH", invoice.getFgoNumar()))
                .thenReturn(new FgoClient.Status(new BigDecimal("389"), new BigDecimal("389")));
        clearInvocations(fgo);

        mockMvc.perform(post("/api/v1/subscriptions/invoices/" + invoice.getId() + "/check-payment")
                        .header("Authorization", "Bearer " + platformToken()))
                .andExpect(status().isNoContent());

        verify(fgo, times(1)).status(any(), any());
        assertThat(invoiceRepository.findById(invoice.getId()).orElseThrow()).satisfies(i -> {
            assertThat(i.getStatus()).isEqualTo(InvoiceStatus.PAID);
            assertThat(i.getPaymentCheckedAt()).isNotNull();
        });

        mockMvc.perform(post("/api/v1/subscriptions/invoices/" + invoice.getId() + "/check-payment")
                        .header("Authorization", "Bearer " + platformToken()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("invoice.not.issued")));
    }

    @Test
    void anFgoThatDoesNotAnswerIsSaidSoAndNothingIsMarked() throws Exception {
        Subscription s = subscription(company(), true);
        billing.run(START);
        SubscriptionInvoice invoice = invoices(s).get(0);
        doThrow(new FgoClient.FgoException("FGO /factura/getstatus: HTTP 409"))
                .when(fgo).status("WH", invoice.getFgoNumar());

        mockMvc.perform(post("/api/v1/subscriptions/invoices/" + invoice.getId() + "/check-payment")
                        .header("Authorization", "Bearer " + platformToken()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$['error-code']", is("fgo.unavailable")));
        // The run itself asked FGO once; the failed check leaves that hour, not a new one.
        assertThat(invoiceRepository.findById(invoice.getId()).orElseThrow().getPaymentCheckedAt())
                .isEqualTo(invoice.getPaymentCheckedAt());
    }

    @Test
    void withoutFgoKeysNoPaymentIsChecked() throws Exception {
        Subscription s = subscription(company(), true);
        billing.run(START);
        when(fgo.isConfigured()).thenReturn(false);
        clearInvocations(fgo);

        mockMvc.perform(post("/api/v1/subscriptions/invoices/" + invoices(s).get(0).getId() + "/check-payment")
                        .header("Authorization", "Bearer " + platformToken()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$['error-code']", is("fgo.not.configured")));
        verify(fgo, never()).status(any(), any());
    }

    /** Transilvania ABC, 17.09: un abonament de probă care pica la fiecare rulare. „Oprește” îl scoate de tot. */
    @Test
    void discardingTheOnlyRefusedInvoiceRemovesTheSubscription() throws Exception {
        Company company = company();
        Subscription s = subscription(company, false);
        billing.run(START);
        SubscriptionInvoice draft = invoices(s).get(0);

        mockMvc.perform(post("/api/v1/subscriptions/invoices/" + draft.getId() + "/discard")
                        .header("Authorization", "Bearer " + platformToken()))
                .andExpect(status().isNoContent());

        assertThat(subscriptionRepository.findById(s.getId())).isEmpty();
        assertThat(invoiceRepository.findById(draft.getId())).isEmpty();
        billing.run(START.plusDays(1));
        assertThat(subscriptionRepository.findByCompany_Id(company.getId())).isEmpty();
    }

    /** Cu o factură emisă înainte, abonamentul rămâne (e legătura cu ea), oprit în ziua dinaintea perioadei refuzate. */
    @Test
    void discardingARefusedInvoiceAfterAnIssuedOneStopsTheSubscriptionBeforeIt() throws Exception {
        Subscription s = subscription(company(), true);
        billing.run(START);
        s = subscriptionRepository.findById(s.getId()).orElseThrow();
        s.setBillingAddress(null);
        subscriptionRepository.save(s);
        LocalDate next = LocalDate.of(2026, 11, 17);
        billing.run(next);
        SubscriptionInvoice draft = invoices(s).get(0);
        assertThat(draft.getStatus()).isEqualTo(InvoiceStatus.DRAFT);

        mockMvc.perform(post("/api/v1/subscriptions/invoices/" + draft.getId() + "/discard")
                        .header("Authorization", "Bearer " + platformToken()))
                .andExpect(status().isNoContent());

        assertThat(subscriptionRepository.findById(s.getId()).orElseThrow()).satisfies(stopped -> {
            assertThat(stopped.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(stopped.getEndsOn()).isEqualTo(next.minusDays(1));
        });
        billing.run(next.plusDays(1));
        assertThat(invoices(s)).singleElement().satisfies(i -> assertThat(i.getStatus()).isEqualTo(InvoiceStatus.ISSUED));
    }

    @Test
    void anIssuedInvoiceCannotBeDiscarded() throws Exception {
        Subscription s = subscription(company(), true);
        billing.run(START);

        mockMvc.perform(post("/api/v1/subscriptions/invoices/" + invoices(s).get(0).getId() + "/discard")
                        .header("Authorization", "Bearer " + platformToken()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("invoice.not.discardable")));
        assertThat(invoices(s)).hasSize(1);
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
                .name("Firma " + suffix())
                .cui(cui())
                .type(CompanyType.GENERATOR).active(true).createdAt(Instant.now()).build());
    }

    private static String billingEmail(Company company) {
        return "facturi+" + company.getCui() + "@firma.ro";
    }

    private Subscription subscription(Company company, boolean withAddress) {
        return subscription(company, withAddress, billingEmail(company));
    }

    private Subscription subscription(Company company, boolean withAddress, String billingEmail) {
        Subscription.SubscriptionBuilder b = Subscription.builder()
                .company(company)
                .plan(SubscriptionPlan.GENERATOR)
                .status(SubscriptionStatus.PENDING)
                .monthlyPrice(SubscriptionPlan.GENERATOR.monthlyPrice())
                .implementationFee(SubscriptionPlan.GENERATOR.implementationFee())
                .extraWorkPointPrice(SubscriptionPlan.EXTRA_WORK_POINT_PRICE)
                .startedAt(START)
                .createdAt(Instant.now())
                .billingEmail(billingEmail);
        if (withAddress) {
            b.billingCounty("Cluj").billingCity("Cluj-Napoca").billingAddress("Str. Memorandumului nr. 1");
        }
        return subscriptionRepository.save(b.build());
    }

    private AppUser admin(Company company) {
        return user(Role.ADMIN, company, null);
    }

    private AppUser user(Role role, Company company, Consultancy consultancy) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + suffix() + "@firma.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(role).company(company).consultancy(consultancy)
                .enabled(true).createdAt(Instant.now()).build());
    }

    private String token(AppUser user) {
        return jwtService.generateToken(user);
    }

    private String platformToken() {
        return token(appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow());
    }

    private static String cui() {
        return TestCui.random();
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
