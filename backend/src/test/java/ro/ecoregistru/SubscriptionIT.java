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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.MarketRole;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.service.EmailService;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F1 din plata-abonamente.md — platforma pune pachetul pe un client și vede ce se va factura.
 *
 * <p>Calculul e probat în {@link BillingCalculatorTest}. Aici e ce nu se vede dintr-o funcție pură:
 * că se numără ce trebuie din bază (punctele de lucru active, firmele active ale cabinetului, cele cu
 * ambalaje), că prețul rămâne cel de la creare, că nimeni din afara platformei nu ajunge aici și că o
 * firmă nu poate ajunge să plătească de două ori.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class SubscriptionIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired ConsultancyRepository consultancyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired SubscriptionRepository subscriptionRepository;

    @MockitoBean EmailService emailService;

    private String platformToken;

    @BeforeEach
    void setUp() {
        platformToken = jwtService.generateToken(
                appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow());
    }

    /** Trei puncte active și unul inactiv: se plătesc două în plus, nu trei. */
    @Test
    void aDirectCompanyIsBilledOnItsActiveWorkPoints() throws Exception {
        Company company = company(null);
        workPoint(company, true);
        workPoint(company, true);
        workPoint(company, true);
        workPoint(company, false);

        mockMvc.perform(put("/api/v1/subscriptions/company/" + company.getId()).with(platform())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("GENERATOR", false, "2026-10-17")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.firstInvoice.from", is("2026-10-17")))
                .andExpect(jsonPath("$.firstInvoice.to", is("2026-11-16")))
                .andExpect(jsonPath("$.firstInvoice.total").value(447))
                .andExpect(jsonPath("$.monthlyInvoice.from", is("2026-11-17")))
                .andExpect(jsonPath("$.monthlyInvoice.total").value(157));
    }

    /** 12 firme active (plus una inactivă), două cu ambalaje: 539 + 2 × 15. */
    @Test
    void aConsultancyIsBilledOnItsActiveCompaniesAndThoseWithPackaging() throws Exception {
        Consultancy consultancy = consultancy();
        for (int i = 0; i < 12; i++) {
            Company c = company(consultancy);
            if (i < 2) {
                c.setMarketRoles(Set.of(MarketRole.PRODUCER));
                companyRepository.save(c);
            }
        }
        Company inactive = company(consultancy);
        inactive.setActive(false);
        companyRepository.save(inactive);

        mockMvc.perform(put("/api/v1/subscriptions/consultancy/" + consultancy.getId()).with(platform())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CONSULTANCY", false, "2026-10-01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyInvoice.total").value(569));
    }

    @Test
    void theGridIsCopiedAndSavingAgainOnTheSamePlanKeepsThePrice() throws Exception {
        Company company = company(null);
        mockMvc.perform(put("/api/v1/subscriptions/company/" + company.getId()).with(platform())
                        .contentType(MediaType.APPLICATION_JSON).content(body("GENERATOR", false, "2026-10-01")))
                .andExpect(status().isOk());

        // Prețul negociat altfel, scris direct în bază: o salvare pe același plan nu-l readuce la grilă.
        Subscription s = subscriptionRepository.findByCompany_Id(company.getId()).orElseThrow();
        s.setMonthlyPrice(java.math.BigDecimal.valueOf(79));
        subscriptionRepository.save(s);

        mockMvc.perform(put("/api/v1/subscriptions/company/" + company.getId()).with(platform())
                        .contentType(MediaType.APPLICATION_JSON).content(body("GENERATOR", true, "2026-10-01")))
                .andExpect(jsonPath("$.monthlyPrice").value(79))
                .andExpect(jsonPath("$.founder", is(true)));

        mockMvc.perform(put("/api/v1/subscriptions/company/" + company.getId()).with(platform())
                        .contentType(MediaType.APPLICATION_JSON).content(body("GENERATOR_PACKAGING", true, "2026-10-01")))
                .andExpect(jsonPath("$.monthlyPrice").value(149))
                .andExpect(jsonPath("$.implementationFee").value(390));
    }

    @Test
    void aClientWithoutASubscriptionAnswersNoContent() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/company/" + company(null).getId()).with(platform()))
                .andExpect(status().isNoContent());
    }

    @Test
    void aCompanyOfAConsultancyHasNoSubscriptionOfItsOwn() throws Exception {
        Company managed = company(consultancy());
        mockMvc.perform(put("/api/v1/subscriptions/company/" + managed.getId()).with(platform())
                        .contentType(MediaType.APPLICATION_JSON).content(body("GENERATOR", false, "2026-10-01")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error-code", is("subscription.company.in.consultancy")));
        assertThat(subscriptionRepository.existsByCompany_Id(managed.getId())).isFalse();
    }

    /** Invers: o firmă care plătește singură nu intră într-un cabinet până nu i se șterge abonamentul. */
    @Test
    void aCompanyThatPaysForItselfCannotBeMovedIntoAConsultancy() throws Exception {
        Company company = company(null);
        Consultancy consultancy = consultancy();
        mockMvc.perform(put("/api/v1/subscriptions/company/" + company.getId()).with(platform())
                        .contentType(MediaType.APPLICATION_JSON).content(body("GENERATOR", false, "2026-10-01")))
                .andExpect(status().isOk());

        String assign = objectMapper.writeValueAsString(Map.of("consultancyId", consultancy.getId()));
        mockMvc.perform(put("/api/v1/companies/" + company.getId() + "/consultancy").with(platform())
                        .contentType(MediaType.APPLICATION_JSON).content(assign))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error-code", is("company.has.own.subscription")));

        mockMvc.perform(delete("/api/v1/subscriptions/company/" + company.getId()).with(platform()))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/v1/companies/" + company.getId() + "/consultancy").with(platform())
                        .contentType(MediaType.APPLICATION_JSON).content(assign))
                .andExpect(status().isOk());
    }

    @Test
    void theCabinetPlanBelongsOnlyToACabinet() throws Exception {
        mockMvc.perform(put("/api/v1/subscriptions/company/" + company(null).getId()).with(platform())
                        .contentType(MediaType.APPLICATION_JSON).content(body("CONSULTANCY", false, "2026-10-01")))
                .andExpect(jsonPath("$.error-code", is("subscription.plan.mismatch")));
        mockMvc.perform(put("/api/v1/subscriptions/consultancy/" + consultancy().getId()).with(platform())
                        .contentType(MediaType.APPLICATION_JSON).content(body("GENERATOR", false, "2026-10-01")))
                .andExpect(jsonPath("$.error-code", is("subscription.plan.mismatch")));
    }

    /** Prețul unui client e al platformei: nici adminul firmei, nici consultantul lui nu-l văd sau schimbă. */
    @Test
    void nobodyButThePlatformReachesSubscriptions() throws Exception {
        Consultancy consultancy = consultancy();
        Company company = company(consultancy);
        String admin = jwtService.generateToken(appUserRepository.save(AppUser.builder()
                .email("admin+" + suffix() + "@firma.ro").password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build()));
        String consultant = jwtService.generateToken(appUserRepository.save(AppUser.builder()
                .email("cons+" + suffix() + "@cabinet.ro").password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.CONSULTANT).consultancy(consultancy).enabled(true).createdAt(Instant.now()).build()));

        for (String token : new String[]{admin, consultant}) {
            mockMvc.perform(get("/api/v1/subscriptions/consultancy/" + consultancy.getId()).with(bearer(token)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/api/v1/subscriptions/consultancy/" + consultancy.getId()).with(bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content(body("CONSULTANCY", false, "2026-10-01")))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/subscriptions/founders").with(bearer(token)))
                    .andExpect(status().isForbidden());
        }
        assertThat(subscriptionRepository.findByConsultancy_Id(consultancy.getId())).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private org.springframework.test.web.servlet.request.RequestPostProcessor platform() {
        return bearer(platformToken);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor bearer(String token) {
        return req -> {
            req.addHeader("Authorization", "Bearer " + token);
            return req;
        };
    }

    private String body(String plan, boolean founder, String startedAt) throws Exception {
        return objectMapper.writeValueAsString(Map.of("plan", plan, "founder", founder, "startedAt", startedAt));
    }

    private Consultancy consultancy() {
        return consultancyRepository.save(Consultancy.builder()
                .name("Cabinet " + suffix()).cui(digitsCui()).createdAt(Instant.now()).build());
    }

    private Company company(Consultancy consultancy) {
        return companyRepository.save(Company.builder()
                .name("Firma " + suffix()).cui(digitsCui()).type(CompanyType.GENERATOR).consultancy(consultancy)
                .active(true).createdAt(Instant.now()).build());
    }

    private void workPoint(Company company, boolean active) {
        workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL " + suffix()).active(active).createdAt(Instant.now()).build());
    }

    private static String digitsCui() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
