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
import org.springframework.test.web.servlet.MockMvc;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import ro.ecoregistru.audit.AuditChangeCodec;
import ro.ecoregistru.audit.PendingAudit;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.AuditLog;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.entity.SubscriptionInvoice;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.InvoiceStatus;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.enums.SubscriptionStatus;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.AuditLogRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-E din todo-clienti-abonamente.md — ce face clientul singur pe {@code /abonament}: își ține la zi datele de
 * facturare (contract art. 7.5), fără denumire și CUI, cu fapta în jurnalul firmei și mail pe adresa veche; și
 * „Am plătit — verifică acum”, doar pe facturile lui, cu pauză între două întrebări la FGO.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class BillingSelfServiceIT {

    static final LocalDate START = LocalDate.of(2026, 10, 17);
    static final String CHANGED_MAIL = "mail/billing_email_changed";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired ConsultancyRepository consultancyRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired SubscriptionInvoiceRepository invoiceRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired BillingRunService billing;
    @Autowired TemplateEngine templateEngine;

    @MockBean EmailService emailService;
    @MockBean FgoClient fgo;

    @BeforeEach
    void setUp() {
        when(fgo.isConfigured()).thenReturn(true);
        when(fgo.emit(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> new FgoClient.Issued("WH",
                inv.<String>getArgument(0).substring(0, 8), "https://fgo.test/x.pdf", null));
        when(fgo.status(any(), any())).thenReturn(new FgoClient.Status(new BigDecimal("389"), BigDecimal.ZERO));
        when(fgo.statusForClient(any(), any())).thenReturn(new FgoClient.Status(new BigDecimal("389"), BigDecimal.ZERO));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Datele de facturare
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void theClientSeesWhereToPayByTransferAndItsCui() throws Exception {
        Company company = company();
        subscription(company);

        mockMvc.perform(get("/api/v1/billing").header("Authorization", "Bearer " + token(admin(company))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientCui", is(company.getCui())))
                .andExpect(jsonPath("$.payee.name", is("ONSIA S.R.L.")))
                .andExpect(jsonPath("$.payee.iban", is("RO32RZBR0000060027995375")))
                .andExpect(jsonPath("$.payee.bank", is("Raiffeisen Bank România")));
    }

    @Test
    void theAdminUpdatesTheBillingDataAndTheJournalSaysWhatChanged() throws Exception {
        Company company = company();
        Subscription s = subscription(company);
        AppUser admin = admin(company);

        mockMvc.perform(put("/api/v1/billing/details")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("contabil@firma.ro", "Bihor", "Oradea", " Str. Republicii nr. 2 ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingEmail", is("contabil@firma.ro")))
                .andExpect(jsonPath("$.billingCounty", is("Bihor")))
                .andExpect(jsonPath("$.billingAddress", is("Str. Republicii nr. 2")));

        Subscription saved = subscriptionRepository.findById(s.getId()).orElseThrow();
        assertThat(saved.getBillingCity()).isEqualTo("Oradea");

        AuditLog row = journal(s);
        assertThat(row.getCompany().getId()).isEqualTo(company.getId());
        assertThat(row.getActorEmail()).isEqualTo(admin.getEmail());
        assertThat(AuditChangeCodec.read(row.getChanges())).extracting(PendingAudit.FieldChange::field)
                .containsExactlyInAnyOrder("billingEmail", "billingCounty", "billingCity", "billingAddress");
    }

    /** Pe adresa veche pleacă vestea; șablonul se randează de-adevăratelea, EmailService fiind mock. */
    @Test
    void theOldAddressIsToldWhenTheInvoiceEmailChanges() throws Exception {
        Company company = company();
        subscription(company);
        String old = billingEmail(company);

        mockMvc.perform(put("/api/v1/billing/details")
                        .header("Authorization", "Bearer " + token(admin(company)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("nou+" + company.getCui() + "@firma.ro", "Cluj", "Cluj-Napoca",
                                "Str. Memorandumului nr. 1")))
                .andExpect(status().isOk());

        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(emailService).send(eq(old), any(), eq(CHANGED_MAIL), context.capture());
        assertThat(templateEngine.process(CHANGED_MAIL, context.getValue()))
                .contains(company.getName(), "nou+" + company.getCui() + "@firma.ro", "/abonament");
    }

    /** Doar adresa schimbată: nimeni nu primește mail, și nimic neschimbat nu scrie în jurnal. */
    @Test
    void sameEmailNoMailAndNothingChangedNothingWritten() throws Exception {
        Company company = company();
        Subscription s = subscription(company);
        String body = details(billingEmail(company), "Cluj", "Cluj-Napoca", "Str. Memorandumului nr. 1");

        mockMvc.perform(put("/api/v1/billing/details").header("Authorization", "Bearer " + token(admin(company)))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());

        verify(emailService, never()).send(any(), any(), eq(CHANGED_MAIL), any());
        assertThat(auditLogRepository.findAll()).noneMatch(r -> s.getId().equals(r.getEntityId()));
    }

    /** Un mail care nu pleacă nu anulează schimbarea: clientul a făcut ce-i cere contractul. */
    @Test
    void aMailThatFailsKeepsTheChange() throws Exception {
        Company company = company();
        Subscription s = subscription(company);
        doThrow(new RuntimeException("smtp down")).when(emailService).send(any(), any(), eq(CHANGED_MAIL), any());

        mockMvc.perform(put("/api/v1/billing/details").header("Authorization", "Bearer " + token(admin(company)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("altul@firma.ro", "Cluj", "Cluj-Napoca", "Str. Memorandumului nr. 1")))
                .andExpect(status().isOk());

        assertThat(subscriptionRepository.findById(s.getId()).orElseThrow().getBillingEmail())
                .isEqualTo("altul@firma.ro");
    }

    /** Denumirea și CUI-ul vin de la ANAF și intră în e-Factura: un câmp strecurat în cerere nu le atinge. */
    @Test
    void theNameAndTheCuiCannotBeChangedFromTheBillingData() throws Exception {
        Company company = company();
        subscription(company);

        mockMvc.perform(put("/api/v1/billing/details").header("Authorization", "Bearer " + token(admin(company)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"billingEmail\":\"a@firma.ro\",\"billingCounty\":\"Cluj\",\"billingCity\":\"Cluj\","
                                + "\"billingAddress\":\"Str. 1\",\"cui\":\"RO1\",\"name\":\"Alta SRL\",\"clientName\":\"Alta SRL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientName", is(company.getName())))
                .andExpect(jsonPath("$.clientCui", is(company.getCui())));

        assertThat(companyRepository.findById(company.getId()).orElseThrow())
                .satisfies(c -> {
                    assertThat(c.getCui()).isEqualTo(company.getCui());
                    assertThat(c.getName()).isEqualTo(company.getName());
                });
    }

    @Test
    void incompleteOrInvalidBillingDataIsRefused() throws Exception {
        Company company = company();
        Subscription s = subscription(company);
        String token = token(admin(company));

        mockMvc.perform(put("/api/v1/billing/details").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("nu-e-mail", "Cluj", "Cluj-Napoca", "Str. 1")))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(put("/api/v1/billing/details").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("a@firma.ro", " ", "Cluj-Napoca", "Str. 1")))
                .andExpect(status().isUnprocessableEntity());

        assertThat(subscriptionRepository.findById(s.getId()).orElseThrow().getBillingEmail())
                .isEqualTo(billingEmail(company));
    }

    /** Scanarea din 17.09.2026: un județ pe care FGO nu-l are se oprește la salvare, nu dimineața, la emitere. */
    @Test
    void aCountyOutsideTheFgoListIsRefused() throws Exception {
        Company company = company();
        Subscription s = subscription(company);

        mockMvc.perform(put("/api/v1/billing/details").header("Authorization", "Bearer " + token(admin(company)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("a@firma.ro", "Judetul Cluj", "Cluj-Napoca", "Str. 1")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("billing.county.invalid")));

        assertThat(subscriptionRepository.findById(s.getId()).orElseThrow().getBillingCounty()).isEqualTo("Cluj");
    }

    @Test
    void anOperatorCannotChangeTheBillingData() throws Exception {
        Company company = company();
        subscription(company);

        mockMvc.perform(put("/api/v1/billing/details")
                        .header("Authorization", "Bearer " + token(user(Role.OPERATOR, company, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("a@firma.ro", "Cluj", "Cluj-Napoca", "Str. 1")))
                .andExpect(status().isForbidden());
    }

    /** Cabinetul n-are jurnal (e al unei firme); schimbarea se face, iar vestea tot pleacă pe adresa veche. */
    @Test
    void theConsultantUpdatesTheCabinetsBillingData() throws Exception {
        Consultancy cabinet = consultancyRepository.save(Consultancy.builder()
                .name("Cabinet " + suffix()).cui(TestCui.random()).createdAt(Instant.now()).build());
        Subscription s = subscriptionRepository.save(Subscription.builder()
                .consultancy(cabinet).plan(SubscriptionPlan.CONSULTANCY).status(SubscriptionStatus.PENDING)
                .monthlyPrice(SubscriptionPlan.CONSULTANCY.monthlyPrice())
                .implementationFee(SubscriptionPlan.CONSULTANCY.implementationFee())
                .companyPriceTier1(SubscriptionPlan.COMPANY_PRICE_TIER1)
                .companyPriceTier2(SubscriptionPlan.COMPANY_PRICE_TIER2)
                .companyPriceTier3(SubscriptionPlan.COMPANY_PRICE_TIER3)
                .packagingCompanyPrice(SubscriptionPlan.PACKAGING_COMPANY_PRICE)
                .startedAt(START).createdAt(Instant.now()).billingEmail("cabinet+" + suffix() + "@firma.ro").build());

        mockMvc.perform(put("/api/v1/billing/details")
                        .header("Authorization", "Bearer " + token(user(Role.CONSULTANT, null, cabinet)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(details("birou@cabinet.ro", "Bihor", "Oradea", "Str. 3")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientCui", is(cabinet.getCui())));

        assertThat(subscriptionRepository.findById(s.getId()).orElseThrow().getBillingEmail())
                .isEqualTo("birou@cabinet.ro");
        verify(emailService).send(eq(s.getBillingEmail()), any(), eq(CHANGED_MAIL), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // „Am plătit — verifică acum”
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void theClientChecksItsOwnInvoiceAndAPaidOneShowsAtOnce() throws Exception {
        Company company = company();
        Subscription s = subscription(company);
        SubscriptionInvoice invoice = issuedAnHourAgo(s);
        when(fgo.statusForClient(any(), eq(invoice.getFgoNumar())))
                .thenReturn(new FgoClient.Status(new BigDecimal("389"), new BigDecimal("389")));
        clearInvocations(fgo);

        mockMvc.perform(post("/api/v1/billing/invoices/" + invoice.getId() + "/check-payment")
                        .header("Authorization", "Bearer " + token(admin(company))))
                .andExpect(status().isNoContent());

        verify(fgo, times(1)).statusForClient(any(), any());
        verify(fgo, never()).status(any(), any());
        mockMvc.perform(get("/api/v1/billing").header("Authorization", "Bearer " + token(admin(company))))
                .andExpect(jsonPath("$.invoices[0].status", is("PAID")))
                .andExpect(jsonPath("$.invoices[0].paymentCheckedAt").exists());
    }

    /** Două apăsări la rând: FGO e întrebat o dată; a doua răspunde fără el, iar pagina arată ora citirii. */
    @Test
    void aSecondCheckWithinTwoMinutesDoesNotAskFgoAgain() throws Exception {
        Company company = company();
        SubscriptionInvoice invoice = issuedAnHourAgo(subscription(company));
        clearInvocations(fgo);
        String token = token(admin(company));

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/billing/invoices/" + invoice.getId() + "/check-payment")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        verify(fgo, times(1)).statusForClient(any(), any());
        assertThat(invoiceRepository.findById(invoice.getId()).orElseThrow().getStatus())
                .isEqualTo(InvoiceStatus.ISSUED);
    }

    /**
     * Scanarea din 17.09.2026: cu FGO căzut (409, timeout), `paymentCheckedAt` nu se scrie, deci pauza nu ținea și
     * fiecare clic punea încă o cerere la coadă. Acum încercarea însăși oprește următoarea, două minute.
     */
    @Test
    void afterACheckFgoDidNotAnswerTheNextClickDoesNotAskAgain() throws Exception {
        Company company = company();
        SubscriptionInvoice invoice = issuedAnHourAgo(subscription(company));
        when(fgo.statusForClient(any(), eq(invoice.getFgoNumar())))
                .thenThrow(new FgoClient.FgoException("HTTP 409, conflict"));
        clearInvocations(fgo);
        String token = token(admin(company));

        mockMvc.perform(post("/api/v1/billing/invoices/" + invoice.getId() + "/check-payment")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$['error-code']", is("fgo.unavailable")));
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/billing/invoices/" + invoice.getId() + "/check-payment")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$['error-code']", is("fgo.recently.asked")));
        }

        verify(fgo, times(1)).statusForClient(any(), any());
        verify(fgo, never()).status(any(), any());
    }

    /** Factura altei firme nu există pentru client: 404, nu 403, și FGO nu e întrebat. */
    @Test
    void anotherCompanysInvoiceIsNotFound() throws Exception {
        Company mine = company();
        subscription(mine);
        SubscriptionInvoice theirs = issuedAnHourAgo(subscription(company()));
        clearInvocations(fgo);

        mockMvc.perform(post("/api/v1/billing/invoices/" + theirs.getId() + "/check-payment")
                        .header("Authorization", "Bearer " + token(admin(mine))))
                .andExpect(status().isNotFound());
        verify(fgo, never()).status(any(), any());
    }

    @Test
    void anOperatorCannotCheckPayment() throws Exception {
        Company company = company();
        SubscriptionInvoice invoice = issuedAnHourAgo(subscription(company));

        mockMvc.perform(post("/api/v1/billing/invoices/" + invoice.getId() + "/check-payment")
                        .header("Authorization", "Bearer " + token(user(Role.OPERATOR, company, null))))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private SubscriptionInvoice issuedAnHourAgo(Subscription s) {
        billing.run(START);
        SubscriptionInvoice invoice = invoiceRepository.findAllBySubscription_IdOrderByPeriodStartDesc(s.getId()).get(0);
        invoice.setPaymentCheckedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        return invoiceRepository.save(invoice);
    }

    private AuditLog journal(Subscription s) {
        List<AuditLog> rows = auditLogRepository.findAll().stream()
                .filter(r -> s.getId().equals(r.getEntityId()))
                .toList();
        assertThat(rows).singleElement().satisfies(r -> assertThat(r.getEntityType()).isEqualTo("Subscription"));
        return rows.get(0);
    }

    private static String details(String email, String county, String city, String address) {
        return "{\"billingEmail\":\"" + email + "\",\"billingCounty\":\"" + county + "\",\"billingCity\":\"" + city
                + "\",\"billingAddress\":\"" + address + "\"}";
    }

    private Company company() {
        return companyRepository.save(Company.builder()
                .name("Firma " + suffix())
                .cui(TestCui.random())
                .type(CompanyType.GENERATOR).active(true).createdAt(Instant.now()).build());
    }

    private static String billingEmail(Company company) {
        return "facturi+" + company.getCui() + "@firma.ro";
    }

    private Subscription subscription(Company company) {
        return subscriptionRepository.save(Subscription.builder()
                .company(company)
                .plan(SubscriptionPlan.GENERATOR)
                .status(SubscriptionStatus.PENDING)
                .monthlyPrice(SubscriptionPlan.GENERATOR.monthlyPrice())
                .implementationFee(SubscriptionPlan.GENERATOR.implementationFee())
                .extraWorkPointPrice(SubscriptionPlan.EXTRA_WORK_POINT_PRICE)
                .startedAt(START)
                .createdAt(Instant.now())
                .billingEmail(billingEmail(company))
                .billingCounty("Cluj").billingCity("Cluj-Napoca").billingAddress("Str. Memorandumului nr. 1")
                .build());
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

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
