package ro.ecoregistru;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.DeviceSession;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.DeviceSessionRepository;
import ro.ecoregistru.service.EmailService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * G2 — tokenul de push stă pe sesiunea de dispozitiv (V56). Fiecare regulă de aici are proba ei negativă:
 * cine își poate pune tokenul, pe ce sesiune, și cui <em>nu</em> mai pleacă o notificare.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@TestPropertySource(properties = "app.demo-password=ProbaTelefon1")
class DevicePushTokenIT {

    private static final String PASSWORD = "ProbaTelefon1";
    private static final String TOKEN = "ExponentPushToken[AbC123_-x]";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired DeviceSessionRepository deviceSessionRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean EmailService emailService;

    private AppUser magazioner;
    private AppUser colleague;

    @BeforeEach
    void setUp() {
        Company company = companyRepository.findAll().getFirst();
        magazioner = user(company);
        colleague = user(company);
    }

    @Test
    void thePhoneDeclaresItsTokenOnItsOwnSession() throws Exception {
        Phone phone = login(magazioner);
        putToken(phone, phone.sessionId(), TOKEN).andExpect(status().isNoContent());
        assertThat(row(phone).getPushToken()).isEqualTo(TOKEN);
    }

    @Test
    void aTokenCanBeTakenBack() throws Exception {
        Phone phone = login(magazioner);
        putToken(phone, phone.sessionId(), TOKEN).andExpect(status().isNoContent());
        putToken(phone, phone.sessionId(), null).andExpect(status().isNoContent());
        assertThat(row(phone).getPushToken()).isNull();
    }

    /** Un coleg nu-și poate lega telefonul de sesiunea altcuiva, ca să primească alertele lui. */
    @Test
    void anotherPersonsSessionIsRefused() throws Exception {
        Phone mine = login(magazioner);
        Phone theirs = login(colleague);
        putToken(theirs, mine.sessionId(), TOKEN).andExpect(status().isBadRequest());
        assertThat(row(mine).getPushToken()).isNull();
    }

    /** După ieșirea din cont, tokenul de acces mai trăiește câteva ore — dar sesiunea nu mai primește nimic. */
    @Test
    void aRevokedSessionIsRefused() throws Exception {
        Phone phone = login(magazioner);
        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(phone.refreshToken())))
                .andExpect(status().isOk());
        putToken(phone, phone.sessionId(), TOKEN).andExpect(status().isBadRequest());
        assertThat(row(phone).getPushToken()).isNull();
    }

    @Test
    void somethingThatIsNotAnExpoTokenIsNotKept() throws Exception {
        Phone phone = login(magazioner);
        putToken(phone, phone.sessionId(), "https://evil.example/hook").andExpect(status().isBadRequest());
        assertThat(row(phone).getPushToken()).isNull();
    }

    @Test
    void withoutAnAccessTokenNothingIsWritten() throws Exception {
        Phone phone = login(magazioner);
        mockMvc.perform(put("/api/v1/auth/devices/{id}/push-token", phone.sessionId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"%s\"}".formatted(TOKEN)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Aplicația reinstalată fără ieșire din cont: sesiunea veche a magazionerului rămâne vie, iar colegul se
     * loghează pe același telefon. Tokenul trece pe sesiunea colegului și dispare de pe cea veche.
     */
    @Test
    void oneTokenStaysOnOneSession() throws Exception {
        Phone old = login(magazioner);
        putToken(old, old.sessionId(), TOKEN).andExpect(status().isNoContent());

        Phone fresh = login(colleague);
        putToken(fresh, fresh.sessionId(), TOKEN).andExpect(status().isNoContent());

        assertThat(row(old).getPushToken()).isNull();
        assertThat(row(fresh).getPushToken()).isEqualTo(TOKEN);
        assertThat(deviceSessionRepository.findLiveWithPushToken(List.of(magazioner), Instant.now())).isEmpty();
    }

    /** Cui pleacă o notificare: numai sesiunile vii. Ieșirea din cont și cele 60 de zile opresc și push-ul. */
    @Test
    void onlyLiveSessionsReceive() throws Exception {
        Phone revoked = login(magazioner);
        putToken(revoked, revoked.sessionId(), "ExponentPushToken[revocat]").andExpect(status().isNoContent());
        Phone expired = login(magazioner);
        putToken(expired, expired.sessionId(), "ExponentPushToken[expirat]").andExpect(status().isNoContent());
        Phone live = login(magazioner);
        putToken(live, live.sessionId(), "ExponentPushToken[viu]").andExpect(status().isNoContent());

        DeviceSession r = row(revoked);
        r.setRevokedAt(Instant.now());
        deviceSessionRepository.save(r);
        DeviceSession e = row(expired);
        e.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        deviceSessionRepository.save(e);

        assertThat(deviceSessionRepository.findLiveWithPushToken(List.of(magazioner), Instant.now()))
                .extracting(DeviceSession::getPushToken).containsExactly("ExponentPushToken[viu]");
    }

    // ── ajutoare ─────────────────────────────────────────────────────────────

    private record Phone(String accessToken, String refreshToken, UUID sessionId) {}

    private AppUser user(Company company) {
        return appUserRepository.save(AppUser.builder()
                .email("push+" + UUID.randomUUID() + "@proba.ro")
                .password(passwordEncoder.encode(PASSWORD))
                .role(Role.OPERATOR).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private Phone login(AppUser u) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","deviceName":"Pixel 8","devicePlatform":"ANDROID"}"""
                                .formatted(u.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return new Phone(json.get("token").asText(), json.get("refreshToken").asText(),
                UUID.fromString(json.get("deviceSessionId").asText()));
    }

    private ResultActions putToken(Phone as, UUID sessionId, String token) throws Exception {
        return mockMvc.perform(put("/api/v1/auth/devices/{id}/push-token", sessionId)
                .header("Authorization", "Bearer " + as.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(token == null ? "{\"token\":null}" : "{\"token\":\"%s\"}".formatted(token)));
    }

    private DeviceSession row(Phone phone) {
        return deviceSessionRepository.findById(phone.sessionId()).orElseThrow();
    }
}
