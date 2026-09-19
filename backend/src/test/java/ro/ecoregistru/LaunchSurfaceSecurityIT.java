package ro.ecoregistru;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantFilter;
import ro.ecoregistru.service.EmailService;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA de lansare, generator — Faza 3: G25, G27 (antetul), G29, G30, G31.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class LaunchSurfaceSecurityIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired TemplateEngine templateEngine;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;
    @MockitoBean EmailService emailService;

    private Company a;
    private Company b;
    private String operatorA;
    private String viewerA;
    private UUID workPointA;
    private UUID partnerA;
    private UUID workPointB;

    @BeforeEach
    void setUp() {
        a = company("Alfa Sec SRL");
        b = company("Beta Sec SRL");
        operatorA = jwtService.generateToken(user(a, Role.OPERATOR));
        viewerA = jwtService.generateToken(user(a, Role.CLIENT_VIEWER));
        workPointA = workPoint(a);
        workPointB = workPoint(b);
        partnerA = partnerRepository.save(Partner.builder()
                .company(a).name("Colector Alfa").authorizationNumber("AM 6/2025")
                .type(PartnerType.COLLECTOR).supplier(true).active(true).createdAt(Instant.now()).build()).getId();
    }

    // ---------- G25: X-Tenant-Id pentru rolurile de firmă ----------

    /**
     * OPERATOR cu antetul firmei B: scrierea ajunge în A, nu în B. Punctul de lucru e al lui A, deci
     * dacă antetul ar fi luat în seamă cererea ar cădea cu 404 (punct de lucru străin în B).
     */
    @Test
    void anOperatorsTenantHeaderIsIgnoredOnAWrite() throws Exception {
        mockMvc.perform(post("/api/v1/movements").header("Authorization", "Bearer " + operatorA)
                        .header(TenantFilter.TENANT_HEADER, b.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(handover()))
                .andExpect(status().isOk());

        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(a.getId())).hasSize(1);
        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(b.getId())).isEmpty();
    }

    /** CLIENT_VIEWER cu antetul firmei B: citește lista lui A, nu pe a lui B. */
    @Test
    void aViewersTenantHeaderIsIgnoredOnARead() throws Exception {
        String json = mockMvc.perform(get("/api/v1/work-points").header("Authorization", "Bearer " + viewerA)
                        .header(TenantFilter.TENANT_HEADER, b.getId().toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(json).contains(workPointA.toString()).doesNotContain(workPointB.toString());
    }

    // ---------- G27: antetul nosniff ----------

    @Test
    void apiResponsesCarryNosniff() throws Exception {
        mockMvc.perform(get("/api/v1/work-points").header("Authorization", "Bearer " + viewerA))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    // ---------- G29: resetarea parolei ----------

    /** Adresă existentă și inexistentă: același cod și același corp, deci răspunsul nu spune cine are cont. */
    @Test
    void theResetAnswerIsTheSameForAKnownAndAnUnknownAddress() throws Exception {
        AppUser known = user(a, Role.OPERATOR);

        MvcResult forKnown = requestReset(known.getEmail(), "10.8.0.1");
        MvcResult forUnknown = requestReset("nimeni+" + UUID.randomUUID() + "@client.ro", "10.8.0.2");

        assertThat(forUnknown.getResponse().getStatus()).isEqualTo(forKnown.getResponse().getStatus());
        assertThat(forUnknown.getResponse().getContentAsString()).isEqualTo(forKnown.getResponse().getContentAsString());
    }

    /** Un cod folosit o dată nu mai schimbă parola a doua oară. */
    @Test
    void aResetCodeWorksOnce() throws Exception {
        AppUser user = user(a, Role.OPERATOR);
        requestReset(user.getEmail(), "10.8.0.3");
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(any(), code.capture());

        assertThat(choose(code.getValue(), "Prima2026parola")).isEqualTo(200);
        int second = choose(code.getValue(), "ADoua2026parola");

        assertThat(second).isBetween(400, 499);
        assertThat(passwordEncoder.matches("Prima2026parola",
                appUserRepository.findByEmail(user.getEmail()).orElseThrow().getPassword())).isTrue();
    }

    /** Controlul negativ al probei de enumerare: adresa necunoscută nu primește niciun mail. */
    @Test
    void anUnknownAddressGetsNoMail() throws Exception {
        requestReset("nimeni+" + UUID.randomUUID() + "@client.ro", "10.8.0.4");
        verify(emailService, never()).sendPasswordResetEmail(any(), anyString());
    }

    // ---------- G30: HTML în mailuri ----------

    /** Un partener numit cu HTML: șablonul îl scrie ca text (numai {@code th:text} în șabloane). */
    @Test
    void aPartnerNameWithHtmlIsEscapedInTheMail() {
        Context ctx = new Context();
        ctx.setVariable("partnerName", "<script>alert(1)</script><b>Colector</b>");
        ctx.setVariable("partnerCui", "RO1");
        ctx.setVariable("authorizationNumber", "<img src=x onerror=alert(1)>");
        ctx.setVariable("expiryDate", "01.10.2026");
        ctx.setVariable("whenText", "expiră");

        String html = templateEngine.process("mail/partner_authorization_expiring", ctx);

        assertThat(html).doesNotContain("<script>").doesNotContain("<img src=x")
                .contains("&lt;script&gt;").contains("&lt;img src=x");
    }

    // ---------- G31 [DECIZIE]: CUI-ul partenerului ----------

    /** // DECIZIE: G31. Un CUI de partener cu cifra de control greșită se acceptă (firma îl refuză). */
    @Test
    void characterizes_aPartnerCuiWithAWrongControlDigitIsAccepted() throws Exception {
        String admin = jwtService.generateToken(user(a, Role.ADMIN));
        String valid = TestCui.random();
        String wrong = valid.substring(0, valid.length() - 1) + ((valid.charAt(valid.length() - 1) - '0' + 1) % 10);

        mockMvc.perform(post("/api/v1/partners").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "CUI greșit SRL", "cui", "RO" + wrong,
                                "type", "COLLECTOR", "supplier", true, "authorizationNumber", "AM 8/2025"))))
                .andExpect(status().is2xxSuccessful());
    }

    // --- helpers ---

    private Company company(String name) {
        return companyRepository.save(Company.builder()
                .name(name).cui(TestCui.random()).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Company c, Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + UUID.randomUUID().toString().substring(0, 8) + "@sec.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(role).company(c).enabled(true).createdAt(Instant.now()).build());
    }

    private UUID workPoint(Company c) {
        return workPointRepository.save(WorkPoint.builder()
                .company(c).name("PL " + c.getName()).active(true).createdAt(Instant.now()).build()).getId();
    }

    private String handover() {
        return """
                {"workPointId": "%s", "date": "2026-07-05", "wasteCodeId": "%s", "quantity": 5,
                 "unit": "KG", "physicalState": "SOLID", "operation": "RECOVERED",
                 "wasteDestination": "Vr", "operationCode": "R13", "partnerId": "%s"}
                """.formatted(workPointA, wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId(), partnerA);
    }

    private MvcResult requestReset(String email, String ip) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/request-reset-password")
                        .with(r -> { r.setRemoteAddr(ip); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email))))
                .andReturn();
    }

    private int choose(String code, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", code, "password", password, "confirmPassword", password))))
                .andReturn().getResponse().getStatus();
    }
}
