package ro.ecoregistru;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AccountRequest;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.entity.Subscription;
import ro.ecoregistru.enums.AccountRequestStatus;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AccountRequestRepository;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ConsultancyRepository;
import ro.ecoregistru.repository.SubscriptionRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.service.EmailService;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-C — „Client nou” în pași: firma, cererea din care vine, abonamentul și administratorul se salvează împreună sau
 * deloc. Înainte, aprobarea făcea doar firma, iar un client rămânea „pe jumătate făcut” fără ca ceva să arate asta.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ClientOnboardingIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired ConsultancyRepository consultancyRepository;
    @Autowired AccountRequestRepository accountRequestRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired WorkPointRepository workPointRepository;

    @MockitoBean EmailService emailService;

    private String platformToken;

    @BeforeEach
    void setUp() {
        platformToken = jwtService.generateToken(
                appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow());
    }

    /** Din cerere: firma cu ce a corectat platforma, punctul de lucru din cerere, abonamentul și invitația. */
    @Test
    void aRequestBecomesACompleteClient() throws Exception {
        AccountRequest request = request(TestCui.random());
        String corrected = TestCui.random();
        String email = "admin+" + UUID.randomUUID() + "@client.ro";

        onboard(platformToken, company("Firma Corectata SRL", corrected), request.getId(),
                subscription("GENERATOR", false), admin(email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company.cui", is(corrected)))
                .andExpect(jsonPath("$.company.name", is("Firma Corectata SRL")))
                .andExpect(jsonPath("$.plan", is("GENERATOR")))
                .andExpect(jsonPath("$.firstInvoice.total").value(50))
                .andExpect(jsonPath("$.invitedEmail", is(email)));

        AccountRequest handled = accountRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(handled.getStatus()).isEqualTo(AccountRequestStatus.APPROVED);
        UUID companyId = handled.getCreatedCompany().getId();
        assertThat(workPointRepository.countByCompany_IdAndActiveTrue(companyId)).isEqualTo(1);

        Subscription s = subscriptionRepository.findByCompany_Id(companyId).orElseThrow();
        assertThat(s.getBillingCounty()).isEqualTo("Bihor");
        assertThat(s.getBillingEmail()).isEqualTo("facturi@client.ro");

        AppUser invited = appUserRepository.findByEmail(email).orElseThrow();
        assertThat(invited.getRole()).isEqualTo(Role.ADMIN);
        assertThat(invited.getCompany().getId()).isEqualTo(companyId);
        verify(emailService).sendInviteEmail(any(), anyString(), anyInt());
    }

    /** Un email care are deja cont refuză tot: nici firma, nici abonamentul, iar cererea rămâne nouă. */
    @Test
    void aRefusedInvitationUndoesTheCompanyAndTheSubscription() throws Exception {
        AccountRequest request = request(TestCui.random());
        String cui = TestCui.random();

        onboard(platformToken, company("Nu Ramane SRL", cui), request.getId(),
                subscription("GENERATOR", false), admin("admin@demo.ro"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(companyRepository.existsByCui(cui)).isFalse();
        assertThat(accountRequestRepository.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(AccountRequestStatus.NEW);
        verify(emailService, never()).sendInviteEmail(any(), anyString(), anyInt());
    }

    /** „Salvează fără abonament” și fără administrator: doar firma. */
    @Test
    void theSubscriptionAndTheAdministratorAreOptional() throws Exception {
        String cui = TestCui.random();
        onboard(platformToken, company("Doar Firma SRL", cui), null, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan", nullValue()))
                .andExpect(jsonPath("$.invitedEmail", nullValue()));

        UUID id = companyRepository.findAll().stream().filter(c -> cui.equals(c.getCui())).findFirst().orElseThrow().getId();
        assertThat(subscriptionRepository.findByCompany_Id(id)).isEmpty();
    }

    /** Consultantul își adaugă firme în portofoliu, dar nu pune abonamente: le plătește firma de consultanță. */
    @Test
    void aConsultantMayNotSetASubscription() throws Exception {
        Consultancy consultancy = consultancyRepository.save(Consultancy.builder()
                .name("Consultant " + UUID.randomUUID()).cui(TestCui.random()).createdAt(Instant.now()).build());
        String token = jwtService.generateToken(appUserRepository.save(AppUser.builder()
                .email("cons+" + UUID.randomUUID() + "@demo.ro").password("x").role(Role.CONSULTANT)
                .consultancy(consultancy).enabled(true).createdAt(Instant.now()).build()));
        String cui = TestCui.random();

        onboard(token, company("Consultantul Plateste SRL", cui), null, subscription("GENERATOR", false), null)
                .andExpect(status().isForbidden());
        assertThat(companyRepository.existsByCui(cui)).isFalse();

        onboard(token, company("Consultantul Plateste SRL", cui), null, null, null)
                .andExpect(status().isOk());
        assertThat(companyRepository.findAllByConsultancy_Id(consultancy.getId()))
                .anyMatch(c -> cui.equals(c.getCui()));
    }

    /** Grila pe trepte, fără implementare; firma de consultanță nu se face din pașii ăștia. */
    @Test
    void thePreviewPricesAPlanNotSavedYet() throws Exception {
        preview(subscription("GENERATOR", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstInvoice.total").value(50))
                .andExpect(jsonPath("$.monthlyInvoice.total").value(50))
                .andExpect(jsonPath("$.firstInvoice.lines.length()").value(1))
                .andExpect(jsonPath("$.firstInvoice.from", is("2026-10-01")))
                .andExpect(jsonPath("$.firstInvoice.to", is("2026-10-31")));
        preview(subscription("GENERATOR_PACKAGING", false))
                .andExpect(jsonPath("$.firstInvoice.total").value(70))
                .andExpect(jsonPath("$.monthlyInvoice.total").value(70));
        preview(subscription("CONSULTANCY", false))
                .andExpect(status().isUnprocessableEntity());
    }

    /** The full service is priced by hand; a custom price drops the tier from the line label. */
    @Test
    void thePreviewBillsThePriceWrittenByHand() throws Exception {
        preview(Map.of("plan", "FULL_SERVICE", "monthlyPrice", 300, "startedAt", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstInvoice.total").value(300))
                .andExpect(jsonPath("$.monthlyInvoice.total").value(300));
        preview(Map.of("plan", "GENERATOR", "sizeTier", 2, "customPrice", true, "monthlyPrice", 42,
                "startedAt", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstInvoice.total").value(42))
                .andExpect(jsonPath("$.monthlyInvoice.total").value(42))
                .andExpect(jsonPath("$.firstInvoice.lines.length()").value(1))
                .andExpect(jsonPath("$.firstInvoice.lines[0].label", is("Generator")))
                .andExpect(jsonPath("$.monthlyInvoice.lines[0].label", is("Generator")));
    }

    /** Annual: the price written is per year, one line, and the next invoice a year later. */
    @Test
    void thePreviewOfAnAnnualPlanBillsTheYear() throws Exception {
        preview(Map.of("plan", "GENERATOR", "sizeTier", 2, "billingMonths", 12, "monthlyPrice", 600,
                "startedAt", "2026-10-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstInvoice.total").value(600))
                .andExpect(jsonPath("$.firstInvoice.to", is("2027-10-04")))
                .andExpect(jsonPath("$.firstInvoice.lines[0].label", is("Generator, abonament anual")))
                .andExpect(jsonPath("$.monthlyInvoice.from", is("2027-10-05")));
        preview(Map.of("plan", "GENERATOR", "sizeTier", 2, "billingMonths", 12, "startedAt", "2026-10-05"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error-code", is("subscription.price.required")));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private ResultActions onboard(String token, Map<String, Object> company, UUID requestId,
                                  Map<String, Object> subscription, Map<String, Object> admin) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("company", company);
        body.put("accountRequestId", requestId);
        body.put("subscription", subscription);
        body.put("admin", admin);
        return mockMvc.perform(post("/api/v1/companies/onboard").with(bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions preview(Map<String, Object> subscription) throws Exception {
        return mockMvc.perform(post("/api/v1/subscriptions/preview").with(bearer(platformToken))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(subscription)));
    }

    private static Map<String, Object> company(String name, String cui) {
        Map<String, Object> c = new HashMap<>();
        c.put("name", name);
        c.put("cui", cui);
        c.put("type", CompanyType.GENERATOR.name());
        c.put("afmObligation", false);
        c.put("address", "Str. Facliei nr. 79, Santandrei");
        c.put("authorizedOperationCodes", List.of());
        c.put("marketRoles", List.of());
        c.put("afmContributions", List.of());
        c.put("authorizedWasteCodeIds", List.of());
        return c;
    }

    private static Map<String, Object> subscription(String plan, boolean founder) {
        return Map.of("plan", plan, "founder", founder, "sizeTier", 2, "startedAt", "2026-10-01",
                "billingEmail", "facturi@client.ro", "billingCounty", "Bihor", "billingCity", "Santandrei",
                "billingAddress", "Str. Facliei nr. 79");
    }

    private static Map<String, Object> admin(String email) {
        return Map.of("email", email, "firstName", "Ana", "lastName", "Pop");
    }

    private AccountRequest request(String cui) {
        return accountRequestRepository.save(AccountRequest.builder()
                .companyName("Din Cerere SRL").cui(cui).companyType(CompanyType.GENERATOR)
                .workPointName("Sediu").workPointAddress("Str. Facliei nr. 79")
                .contactEmail("contact@client.ro").status(AccountRequestStatus.NEW).createdAt(Instant.now()).build());
    }

    private static RequestPostProcessor bearer(String token) {
        return req -> {
            req.addHeader("Authorization", "Bearer " + token);
            return req;
        };
    }
}
