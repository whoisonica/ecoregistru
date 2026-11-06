package ro.ecoregistru;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.AuditLog;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.AuditLogRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ConsultancyRepository;
import ro.ecoregistru.service.AuthenticationService;
import ro.ecoregistru.service.EmailService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Turul aplicației (V87): serverul ține minte, pe om, că l-a văzut. Răspunsul de login spune
 * {@code tourSeen}, iar {@code POST /api/v1/users/me/tour-seen} îl închide o dată pentru totdeauna.
 *
 * <p>Contul nou e făcut pe drumul adevărat — invitație, apoi parola pusă din linkul din mail —, fiindcă
 * exact acolo trebuie să rămână coloana goală. Fiecare probă își face firma ei: invitațiile se numără pe
 * firmă ({@code INVITE_PER_ACCOUNT}), iar firma demo o folosesc și alte clase din același context.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class TourSeenIT {

    private static final String PASSWORD = "ProbaTur2026";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired ConsultancyRepository consultancyRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired AuthenticationService authenticationService;

    /** Mocked so the invite mail goes nowhere and its code can be read back. */
    @MockitoBean EmailService emailService;

    @Test
    void anInviteeWhoJustSetTheirPasswordHasNotSeenTheTour() throws Exception {
        AppUser user = activatedInvitee();
        assertThat(login(user).get("tourSeen").asBoolean()).isFalse();
    }

    @Test
    void afterTheTourTheNextLoginSaysItWasSeen() throws Exception {
        AppUser user = activatedInvitee();
        String token = login(user).get("token").asText();

        tourSeen(token).andExpect(status().isNoContent());

        assertThat(seenAt(user)).isNotNull();
        assertThat(login(user).get("tourSeen").asBoolean()).isTrue();
    }

    /** Two tabs, or a retry: the second call answers the same and does not move the first time. */
    @Test
    void aSecondCallKeepsTheFirstTimestamp() throws Exception {
        AppUser user = activatedInvitee();
        String token = login(user).get("token").asText();

        tourSeen(token).andExpect(status().isNoContent());
        Instant first = seenAt(user);
        tourSeen(token).andExpect(status().isNoContent());

        assertThat(first).isNotNull();
        assertThat(seenAt(user)).isEqualTo(first);
    }

    /**
     * Every role, the viewer and the scale operator included, and the two that work without a tenant of their
     * own: no {@code X-Tenant-Id} is sent here, so the platform admin and the consultant have none.
     */
    @Test
    void everyRoleMayCloseTheTourWithOrWithoutATenant() throws Exception {
        Company company = company();
        Consultancy consultancy = consultancyRepository.save(Consultancy.builder()
                .name("Consultanta Tur " + suffix()).cui(TestCui.random()).createdAt(Instant.now()).build());

        for (Role role : Role.values()) {
            AppUser user = switch (role) {
                case PLATFORM_ADMIN -> user(role, null, null);
                case CONSULTANT -> user(role, null, consultancy);
                default -> user(role, company, null);
            };
            tourSeen(jwtService.generateToken(user)).andExpect(status().isNoContent());
            assertThat(seenAt(user)).as(role.name()).isNotNull();
        }
    }

    /** G1 — the phone gets the flag at login and again on every refresh. */
    @Test
    void thePhoneLoginAndTheRefreshCarryTheFlagToo() throws Exception {
        AppUser user = activatedInvitee();
        JsonNode phone = json(mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Forwarded-For", freshIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", user.getEmail(), "password", PASSWORD,
                        "deviceName", "Pixel 8", "devicePlatform", "ANDROID")))));
        assertThat(phone.get("tourSeen").asBoolean()).isFalse();

        tourSeen(phone.get("token").asText()).andExpect(status().isNoContent());

        JsonNode refreshed = json(mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", phone.get("refreshToken").asText())))));
        assertThat(refreshed.get("tourSeen").asBoolean()).isTrue();
    }

    /** Housekeeping, not a deed on the firm's data: no „Modificare" row in the firm's history. */
    @Test
    void closingTheTourLeavesNothingInTheAuditLog() throws Exception {
        AppUser user = activatedInvitee();
        String token = login(user).get("token").asText();
        long before = auditRowsOf(user);

        tourSeen(token).andExpect(status().isNoContent());

        assertThat(seenAt(user)).isNotNull();
        assertThat(auditRowsOf(user)).isEqualTo(before);
    }

    @Test
    void withoutASessionItIs401() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/tour-seen")).andExpect(status().isUnauthorized());
    }

    /** The accounts the e2e suite and the screenshot scripts drive: a tour over them would break every probe. */
    @Test
    void theSeededDemoAccountsHaveSeenTheTour() {
        for (String email : List.of("platform@ecoregistru.ro", "admin@demo.ro", "operator@demo.ro",
                "cantar@demo.ro", "viewer@demo.ro")) {
            assertThat(appUserRepository.findByEmail(email).orElseThrow().getTourSeenAt()).as(email).isNotNull();
        }
    }

    // ── ajutoare ─────────────────────────────────────────────────────────────

    /** Invited through the real service, then activated through the link in the mail, as a client would. */
    private AppUser activatedInvitee() throws Exception {
        String email = "tur+" + suffix() + "@client.ro";
        authenticationService.inviteUser(company(), email, Role.OPERATOR, "Tudor", "Tur");
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendInviteEmail(any(), code.capture(), anyInt());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", code.getValue(), "password", PASSWORD, "confirmPassword", PASSWORD))))
                .andExpect(status().isOk());

        AppUser user = appUserRepository.findByEmail(email).orElseThrow();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getTourSeenAt()).isNull();
        return user;
    }

    private JsonNode login(AppUser user) throws Exception {
        return json(mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Forwarded-For", freshIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", user.getEmail(), "password", PASSWORD)))));
    }

    private ResultActions tourSeen(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/users/me/tour-seen").header("Authorization", "Bearer " + token));
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private Instant seenAt(AppUser user) {
        return appUserRepository.findById(user.getId()).orElseThrow().getTourSeenAt();
    }

    private long auditRowsOf(AppUser user) {
        return auditLogRepository.count((Specification<AuditLog>) (root, query, cb) ->
                cb.equal(root.get("entityId"), user.getId()));
    }

    private Company company() {
        return companyRepository.save(Company.builder()
                .name("Firma Tur " + suffix()).cui(TestCui.random())
                .type(CompanyType.GENERATOR).active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Role role, Company company, Consultancy consultancy) {
        return appUserRepository.save(AppUser.builder()
                .email("tur-" + role.name().toLowerCase() + "+" + suffix() + "@firma.ro")
                .password(passwordEncoder.encode(PASSWORD))
                .role(role).company(company).consultancy(consultancy)
                .enabled(true).createdAt(Instant.now()).build());
    }

    /** Its own caller for the per-IP login limit, so these logins never share a bucket with other classes. */
    private static String freshIp() {
        return "10.8." + Math.floorMod(UUID.randomUUID().hashCode(), 250) + "." + Math.floorMod(UUID.randomUUID().hashCode(), 250);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
