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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * G1 din {@code todo-mobil.md} — sesiunea pe dispozitiv.
 *
 * <p><b>De ce există felia.</b> Tokenul de acces ține opt ore și nu se reînnoiește. Pe web e bine;
 * pe telefon, magazionerul de la rampă nu-și ține parola minte, deci fără asta aplicația mobilă e
 * o aplicație care cere parola la fiecare tură.
 *
 * <p><b>Ce probează clasa.</b> Nu că reîmprospătarea merge — aia e o linie —, ci fiecare motiv
 * pentru care <em>nu</em> trebuie să meargă. Fiecare are proba negativă lui: scoți regula din
 * {@code DeviceSessionService} și testul cade.
 *
 * <p><b>Ce nu se schimbă.</b> Webul nu trimite nume de dispozitiv, deci nu primește token de
 * reîmprospătare; sesiunea de browser rămâne exact ce era (P0.4).
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@org.springframework.test.context.TestPropertySource(properties = "app.demo-password=ProbaTelefon1")
class DeviceSessionIT {

    private static final String PASSWORD = "ProbaTelefon1";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired DeviceSessionRepository deviceSessionRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ro.ecoregistru.config.JwtService jwtService;

    @MockitoBean EmailService emailService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        Company company = companyRepository.findAll().getFirst();
        user = appUserRepository.save(AppUser.builder()
                .email("magazioner+" + UUID.randomUUID() + "@proba.ro")
                .password(passwordEncoder.encode(PASSWORD))
                .role(Role.OPERATOR)
                .company(company)
                .enabled(true)
                .createdAt(Instant.now())
                .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ce primește fiecare
    // ─────────────────────────────────────────────────────────────────────────

    /** Telefonul spune cum îl cheamă și primește dreptul la încă opt ore, de câte ori vrea. */
    @Test
    void aPhoneThatNamesItselfGetsALongSession() throws Exception {
        JsonNode body = loginAsPhone();
        assertThat(body.get("refreshToken").asText()).isNotBlank();
        assertThat(body.get("deviceSessionId").asText()).isNotBlank();
        assertThat(body.get("token").asText()).isNotBlank();
    }

    /**
     * Proba negativă a aceleiași reguli, din partea cealaltă: webul se loghează la fel de la
     * început și nu capătă nimic nou. Dacă cineva ar scoate condiția pe {@code deviceName}, aici
     * ar apărea un token lung în răspunsul unui browser — exact ce a scos P0.4 din `localStorage`.
     */
    @Test
    void theWebGetsNoLongSession() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(user.getEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", nullValue()))
                .andExpect(jsonPath("$.deviceSessionId", nullValue()));
    }

    /** Tokenul nu e ținut în bază — doar hash-ul lui. O bază scursă nu dă nimănui o sesiune. */
    @Test
    void theDatabaseNeverHoldsTheTokenItself() throws Exception {
        String refresh = loginAsPhone().get("refreshToken").asText();
        DeviceSession row = deviceSessionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(user.getId())).findFirst().orElseThrow();
        assertThat(row.getTokenHash()).isNotEqualTo(refresh).hasSize(64);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rotirea
    // ─────────────────────────────────────────────────────────────────────────

    /** Fiecare folosire dă un token nou <b>și</b> îl stinge pe cel folosit. */
    @Test
    void aRefreshTokenWorksExactlyOnce() throws Exception {
        String first = loginAsPhone().get("refreshToken").asText();

        String second = refresh(first).get("refreshToken").asText();
        assertThat(second).isNotEqualTo(first);

        // Al doilea deschide; primul, refolosit, nu mai deschide nimic.
        refreshRefused(first);
        refresh(second);
    }

    /** Sesiunea e a omului: tokenul nou aduce rolul și firma lui, nu doar un șir. */
    @Test
    void theRefreshedSessionIsStillTheSamePerson() throws Exception {
        JsonNode refreshed = refresh(loginAsPhone().get("refreshToken").asText());
        assertThat(refreshed.get("email").asText()).isEqualTo(user.getEmail());
        assertThat(refreshed.get("role").asText()).isEqualTo("OPERATOR");
        assertThat(refreshed.get("tenantId").asText()).isEqualTo(user.getCompany().getId().toString());
    }

    /** Tokenul de acces primit la reîmprospătare chiar deschide API-ul, nu e doar bine format. */
    @Test
    void theAccessTokenFromARefreshOpensTheApi() throws Exception {
        String access = refresh(loginAsPhone().get("refreshToken").asText()).get("token").asText();
        mockMvc.perform(get("/api/v1/companies/current").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fiecare motiv de refuz
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void anUnknownTokenIsRefused() throws Exception {
        refreshRefused("nici-macar-nu-e-un-token");
    }

    /** Ieșirea din cont de pe telefon stinge chiar sesiunea aia, și numai pe ea. */
    @Test
    void loggingOutOnThePhoneEndsThatSession() throws Exception {
        String phone = loginAsPhone("iPhone 17").get("refreshToken").asText();
        String tablet = loginAsPhone("Pixel 8").get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(phone)))
                .andExpect(status().isOk());

        refreshRefused(phone);
        refresh(tablet);
    }

    /** Șaizeci de zile de nefolosire și telefonul cere parola din nou. */
    @Test
    void aSessionLeftInADrawerExpires() throws Exception {
        String token = loginAsPhone().get("refreshToken").asText();
        DeviceSession row = liveRow();
        row.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        deviceSessionRepository.save(row);

        refreshRefused(token);
    }

    /**
     * Parola schimbată ia cu ea și telefoanele. Fără linia din {@code resetPassword},
     * {@code tokenVersion} scotea din cont fiecare sesiune de browser și o lăsa vie exact pe cea
     * lungă — adică pe singura care contează.
     */
    @Test
    void aPasswordResetEndsEveryPhone() throws Exception {
        String token = loginAsPhone().get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/request-reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}""".formatted(user.getEmail())))
                .andExpect(status().isOk());
        String code = resetCodeOf(user);
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","password":"AltaParola1","confirmPassword":"AltaParola1"}"""
                                .formatted(code)))
                .andExpect(status().isOk());

        refreshRefused(token);
    }

    /**
     * Contul dezactivat e scos din aplicație la următoarea cerere — aceeași clasă de bug ca
     * „Parolă uitată” pe 15.09: o cale care ocolea decizia administratorului.
     *
     * <p><b>Numai {@code deactivatedAt}, cu {@code enabled} lăsat pe true</b>, deși dezactivarea
     * adevărată le pune pe amândouă. Altfel regula asta n-ar fi probată deloc: verificarea de
     * dedesubt, pe {@code enabled}, ar prinde cazul și scoaterea ei din cod n-ar face niciun test
     * să cadă. Perechea (enabled, deactivatedAt) e și motivul pentru care loginul întreabă în
     * ordinea asta — vezi comentariul din {@code AuthenticationService.login}.
     */
    @Test
    void aDeactivatedAccountCannotRefresh() throws Exception {
        String token = loginAsPhone().get("refreshToken").asText();

        user.setDeactivatedAt(Instant.now());
        appUserRepository.save(user);

        refreshRefused(token);
    }

    /**
     * Și pe drumul adevărat: administratorul apasă „Dezactivează”, iar telefonul colegului iese din
     * cont. Cele două linii din {@code CompanyUserService} și {@code ConsultancyService} sunt ce
     * face lista „Dispozitive conectate” sinceră — fără ele sesiunea ar fi arătat vie până când
     * telefonul s-ar fi întors singur să ceară un token.
     */
    @Test
    void anAdminDeactivatingAColleagueEndsTheirPhone() throws Exception {
        String token = loginAsPhone().get("refreshToken").asText();

        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("admin+" + UUID.randomUUID() + "@proba.ro")
                .password(passwordEncoder.encode(PASSWORD))
                .role(Role.ADMIN).company(user.getCompany())
                .enabled(true).createdAt(Instant.now()).build());

        mockMvc.perform(delete("/api/v1/users/" + user.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .header("X-Tenant-Id", user.getCompany().getId().toString()))
                .andExpect(status().isNoContent());

        assertThat(deviceSessionRepository.findByUserAndRevokedAtIsNullOrderByLastUsedAtDesc(user)).isEmpty();
        refreshRefused(token);
    }

    /** Și un cont doar neactivat (invitat, fără parolă aleasă) — altă stare, același refuz. */
    @Test
    void anAccountThatWasNeverActivatedCannotRefresh() throws Exception {
        String token = loginAsPhone().get("refreshToken").asText();

        user.setEnabled(false);
        appUserRepository.save(user);

        refreshRefused(token);
    }

    /** Un refuz stinge rândul, ca telefonul respins să nu mai poată încerca la nesfârșit. */
    @Test
    void aRefusedRefreshAlsoBurnsTheRow() throws Exception {
        String token = loginAsPhone().get("refreshToken").asText();
        user.setEnabled(false);
        user.setDeactivatedAt(Instant.now());
        appUserRepository.save(user);

        refreshRefused(token);

        user.setEnabled(true);
        user.setDeactivatedAt(null);
        appUserRepository.save(user);
        // Contul e din nou în regulă, dar sesiunea aia s-a stins când a fost refuzată.
        refreshRefused(token);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // „Dispozitive conectate”
    // ─────────────────────────────────────────────────────────────────────────

    /** Lista e a contului, cere token, și nu arată nici hash, nici token. */
    @Test
    void theDeviceListNeedsASessionAndLeaksNothing() throws Exception {
        JsonNode phone = loginAsPhone("iPhone 17");

        mockMvc.perform(get("/api/v1/auth/devices")).andExpect(status().isUnauthorized());

        MvcResult res = mockMvc.perform(get("/api/v1/auth/devices")
                        .header("Authorization", "Bearer " + phone.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceName", is("iPhone 17")))
                .andExpect(jsonPath("$[0].platform", is("IOS")))
                .andReturn();
        String body = res.getResponse().getContentAsString();
        assertThat(body).doesNotContain(phone.get("refreshToken").asText()).doesNotContain("tokenHash");
    }

    /** „Scoate telefonul ăsta” din Setări: sesiunea moare, și cu ea dreptul la un token nou. */
    @Test
    void removingADeviceFromTheListEndsIt() throws Exception {
        JsonNode phone = loginAsPhone();
        mockMvc.perform(delete("/api/v1/auth/devices/" + phone.get("deviceSessionId").asText())
                        .header("Authorization", "Bearer " + phone.get("token").asText()))
                .andExpect(status().isNoContent());

        refreshRefused(phone.get("refreshToken").asText());
    }

    /** Și nu de pe contul altuia: id-ul altui om e refuzat, nu executat în tăcere. */
    @Test
    void nobodyRemovesSomeoneElsesDevice() throws Exception {
        JsonNode mine = loginAsPhone();

        AppUser other = appUserRepository.save(AppUser.builder()
                .email("altcineva+" + UUID.randomUUID() + "@proba.ro")
                .password(passwordEncoder.encode(PASSWORD))
                .role(Role.OPERATOR).company(user.getCompany())
                .enabled(true).createdAt(Instant.now()).build());
        String otherPhone = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","deviceName":"Pixel 8","devicePlatform":"ANDROID"}"""
                                .formatted(other.getEmail(), PASSWORD)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("refreshToken").asText();

        DeviceSession otherRow = deviceSessionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(other.getId())).findFirst().orElseThrow();

        mockMvc.perform(delete("/api/v1/auth/devices/" + otherRow.getId())
                        .header("Authorization", "Bearer " + mine.get("token").asText()))
                .andExpect(status().isBadRequest());

        // Și telefonul lui merge mai departe, netulburat.
        refresh(otherPhone);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ajutoare
    // ─────────────────────────────────────────────────────────────────────────

    private JsonNode loginAsPhone() throws Exception {
        return loginAsPhone("iPhone 17");
    }

    private JsonNode loginAsPhone(String device) throws Exception {
        String platform = device.startsWith("iPhone") ? "IOS" : "ANDROID";
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","deviceName":"%s","devicePlatform":"%s"}"""
                                .formatted(user.getEmail(), PASSWORD, device, platform)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString());
    }

    private JsonNode refresh(String token) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(token)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString());
    }

    private void refreshRefused(String token) throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(token)))
                .andExpect(status().isBadRequest());
    }

    private DeviceSession liveRow() {
        return deviceSessionRepository.findByUserAndRevokedAtIsNullOrderByLastUsedAtDesc(user).getFirst();
    }

    private String resetCodeOf(AppUser u) {
        return verificationRecordRepository.findAll().stream()
                .filter(r -> r.getUser().getId().equals(u.getId()) && !r.isConfirmed())
                .findFirst().orElseThrow().getCode();
    }

    @Autowired ro.ecoregistru.repository.VerificationRecordRepository verificationRecordRepository;
}
