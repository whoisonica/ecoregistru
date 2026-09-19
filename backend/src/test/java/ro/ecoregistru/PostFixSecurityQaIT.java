package ro.ecoregistru;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.exception.EmailException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.service.CloudinaryStorageService;
import ro.ecoregistru.service.EmailService;
import ro.ecoregistru.service.ReportBrandingService;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * QA de securitate după reparațiile BUG-031…044 (19.09.2026): izolare, roluri, date personale și intrări, pe
 * suprafața generatorului. Fiecare test spune comportamentul <b>sigur</b>; cele care pică sunt constatări.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PostFixSecurityQaIT {

    private static final String CNP = "1900101123457";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired ConsultancyRepository consultancyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired VerificationRecordRepository verificationRecordRepository;
    @MockitoBean EmailService emailService;
    @MockitoBean CloudinaryStorageService storageService;

    private Company a;
    private String adminA;
    private String operatorA;
    private String viewerA;
    private UUID workPointA;
    private UUID partnerA;

    @BeforeEach
    void setUp() {
        a = company("Alfa QA SRL", null);
        adminA = jwtService.generateToken(user(a, Role.ADMIN));
        operatorA = jwtService.generateToken(user(a, Role.OPERATOR));
        viewerA = jwtService.generateToken(user(a, Role.CLIENT_VIEWER));
        workPointA = workPointRepository.save(WorkPoint.builder()
                .company(a).name("PL Alfa").active(true).createdAt(Instant.now()).build()).getId();
        partnerA = partnerRepository.save(Partner.builder()
                .company(a).name("Colector Alfa").authorizationNumber("AM 6/2025")
                .type(PartnerType.COLLECTOR).supplier(true).active(true).createdAt(Instant.now()).build()).getId();
    }

    // ───────────────────────── rate limit: calea codată în procente ─────────────────────────

    /**
     * {@code RateLimitFilter} caută {@code request.getRequestURI()} (brut, necodat) într-o hartă exactă, dar Spring
     * MVC potrivește ruta pe calea decodată. {@code /api/v1/auth/%6Cogin} ajunge la login fără să treacă prin
     * limita pe IP.
     */
    @Test
    void aPercentEncodedLoginPathReachesTheControllerAndIsStillLimitedPerIp() throws Exception {
        String ip = "198.51.100." + ThreadLocalRandom.current().nextInt(1, 250) + "-" + UUID.randomUUID();
        URI encoded = URI.create("/api/v1/auth/%6Cogin");
        int last = 0;
        for (int i = 0; i < 61; i++) {
            last = mockMvc.perform(post(encoded)
                            .with(r -> { r.setRemoteAddr(ip); return r; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"x" + UUID.randomUUID() + "@example.ro\",\"password\":\"Gresita123\"}"))
                    .andReturn().getResponse().getStatus();
            if (i == 0) {
                assertThat(last).as("calea codată ajunge la controllerul de login (400 = parolă greșită)").isEqualTo(400);
            }
        }
        assertThat(last).as("a 61-a încercare de pe același IP").isEqualTo(429);
    }

    /** Aceeași ocolire pe singura scriere publică: cererea de cont (10 pe oră de pe un IP). */
    @Test
    void aPercentEncodedAccountRequestPathIsStillLimitedPerIp() throws Exception {
        String ip = "198.51.100.7-" + UUID.randomUUID();
        URI encoded = URI.create("/api/v1/account-%72equests");
        int last = 0;
        for (int i = 0; i < 11; i++) {
            last = mockMvc.perform(post(encoded)
                            .with(r -> { r.setRemoteAddr(ip); return r; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "companyName", "Spam " + i, "cui", TestCui.random(), "companyType", "GENERATOR",
                                    "contactEmail", "spam" + i + "@example.ro"))))
                    .andReturn().getResponse().getStatus();
            if (i == 0) {
                assertThat(last).as("calea codată ajunge la controller").isBetween(200, 299);
            }
        }
        assertThat(last).as("a 11-a cerere de cont de pe același IP").isEqualTo(429);
    }

    // ───────────────────────── BUG-043: CNP-ul pe aviz ─────────────────────────

    /**
     * BUG-043 a mascat CNP-ul în {@code GET /drivers} și {@code GET /movements} pentru „Vizualizare”, dar
     * {@code GET /movements/{id}/aviz} e deschis oricărui rol și tipărește CNP-ul întreg.
     */
    @Test
    @Disabled("QA-SEC-2: BUG-043 incomplet, avizul tipărește CNP-ul întreg pentru CLIENT_VIEWER")
    void aViewerDoesNotGetTheWholeCnpThroughTheAviz() throws Exception {
        String id = createMovement(operatorA, handover("2026-07-05", ",\"driverName\":\"Ion Sofer\",\"driverCnp\":\"" + CNP + "\""));

        String listForViewer = mockMvc.perform(get("/api/v1/movements/" + id).header("Authorization", "Bearer " + viewerA))
                .andReturn().getResponse().getContentAsString();
        assertThat(listForViewer).as("controlul: JSON-ul e mascat").doesNotContain(CNP);

        assertThat(pdfText("/api/v1/movements/" + id + "/aviz", viewerA)).as("avizul descărcat de „Vizualizare”")
                .doesNotContain(CNP);
    }

    // ───────────────────────── ani fără margine ─────────────────────────

    /**
     * „Regenerează” cascadează de la anul cerut până la ultimul an din cache, fără margine jos. Un OPERATOR care
     * cere anul 1 pune serverul să reconstruiască ~2025 de ani într-o singură tranzacție, sub lacătul firmei.
     */
    @Test
    void regenerateRefusesAYearNobodyCanHaveMovementsIn() throws Exception {
        createMovement(operatorA, handover("2026-03-10", ""));
        mockMvc.perform(get("/api/v1/evidences").param("year", "2026").header("Authorization", "Bearer " + operatorA))
                .andReturn();

        long t0 = System.nanoTime();
        MvcResult r = mockMvc.perform(post("/api/v1/evidences/regenerate").param("year", "1")
                .header("Authorization", "Bearer " + operatorA)).andReturn();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        String body = r.getResponse().getContentAsString();
        int cascaded = r.getResponse().getStatus() == 200
                ? objectMapper.readTree(body).path("cascadedYears").size() : -1;
        System.out.println("QA regenerate year=1 → " + r.getResponse().getStatus() + " in " + ms + " ms, cascaded="
                + cascaded + " bodyLen=" + body.length());
        assertThat(r.getResponse().getStatus()).as("an 1 → 4xx, nu " + cascaded + " ani reconstruiți în " + ms + " ms")
                .isBetween(400, 499);
    }

    /** Un an în afara domeniului Postgres (sau al lui LocalDate): 4xx, nu 500. */
    @Test
    void aYearOutsideTheDatabaseRangeIsNotAServerError() throws Exception {
        java.util.List<String> fiveHundreds = new java.util.ArrayList<>();
        for (String year : new String[]{"-300000", "300000000", "2147483647"}) {
            for (String url : new String[]{"/api/v1/evidences", "/api/v1/deadlines", "/api/v1/packaging/market",
                    "/api/v1/packaging/table1", "/api/v1/packaging/anexa3", "/api/v1/packaging/anexa1",
                    "/api/v1/evidences/anexa1", "/api/v1/evidences/declaratie-anuala", "/api/v1/evidences/export",
                    "/api/v1/movements", "/api/v1/movements/totals", "/api/v1/audit-file/size", "/api/v1/audit-file/contents"}) {
                int status = mockMvc.perform(get(url).param("year", year).header("Authorization", "Bearer " + viewerA))
                        .andReturn().getResponse().getStatus();
                if (status >= 500) fiveHundreds.add("GET " + url + "?year=" + year + " → " + status);
            }
            int summary = mockMvc.perform(get("/api/v1/movements/summary").param("year", year).param("month", "13")
                    .header("Authorization", "Bearer " + viewerA)).andReturn().getResponse().getStatus();
            if (summary >= 500) fiveHundreds.add("GET /movements/summary?year=" + year + "&month=13 → " + summary);
            int regen = mockMvc.perform(post("/api/v1/evidences/regenerate").param("year", year)
                    .header("Authorization", "Bearer " + operatorA)).andReturn().getResponse().getStatus();
            if (regen >= 500) fiveHundreds.add("POST /evidences/regenerate?year=" + year + " → " + regen);
        }
        System.out.println("QA year range 5xx: " + fiveHundreds);
        assertThat(fiveHundreds).as("ani absurzi → 500").isEmpty();
    }

    /** Data mișcării n-are margini: anul 1 și anul 9999 se primesc, iar 9999 duce cache-ul până acolo. */
    @Test
    void aMovementDatedInYearOneOrYear9999IsRefused() throws Exception {
        for (String date : new String[]{"0001-06-01", "9999-12-31"}) {
            int status = mockMvc.perform(post("/api/v1/movements").header("Authorization", "Bearer " + operatorA)
                            .contentType(MediaType.APPLICATION_JSON).content(handover(date, "")))
                    .andReturn().getResponse().getStatus();
            assertThat(status).as("mișcare pe " + date).isBetween(400, 499);
        }
    }

    // ───────────────────────── intrări numerice și text rămase ─────────────────────────

    /** Același tipar ca BUG-039, pe „pus pe piață” la ambalaje: NUMERIC(14,3). */
    @Test
    void aPackagingMarketQuantityThatDoesNotFitIsNotAServerError() throws Exception {
        int status = mockMvc.perform(put("/api/v1/packaging/market").header("Authorization", "Bearer " + operatorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"material\":\"PET\",\"year\":2026,\"salesPackaging\":1e15}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).as("1e15 kg pus pe piață").isBetween(400, 499);
    }

    /** Și rotunjirea tăcută din BUG-039: 0.0001 se păstrează 0.000. */
    @Test
    void aPackagingMarketQuantityWithFourDecimalsIsRefusedOrKept() throws Exception {
        String body = mockMvc.perform(put("/api/v1/packaging/market").header("Authorization", "Bearer " + operatorA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"material\":\"STICLA\",\"year\":2026,\"primaryTotal\":0.0001}"))
                .andReturn().getResponse().getContentAsString();
        String stored = mockMvc.perform(get("/api/v1/packaging/market").param("year", "2026")
                .header("Authorization", "Bearer " + operatorA)).andReturn().getResponse().getContentAsString();
        System.out.println("QA packaging 0.0001 → " + body + " / stored " + stored);
        JsonNode rows = objectMapper.readTree(stored);
        for (JsonNode row : rows) {
            if ("STICLA".equals(row.path("material").asText()) && !row.path("primaryTotal").isNull()) {
                assertThat(row.path("primaryTotal").decimalValue().signum()).as("0.0001 salvat ca zero").isPositive();
            }
        }
    }

    /** Invitația de coleg în cabinet: câmpurile n-au {@code @Size} (coloanele sunt VARCHAR(128)/(255)). */
    @Test
    void aConsultantInvitingAColleagueWithALongNameIsNotAServerError() throws Exception {
        Consultancy cabinet = consultancyRepository.save(Consultancy.builder()
                .name("Cabinet QA").cui("RO" + ThreadLocalRandom.current().nextLong(10_000_000L, 9_999_999_999L))
                .createdAt(Instant.now()).build());
        String consultant = jwtService.generateToken(consultant(cabinet));
        int status = mockMvc.perform(post("/api/v1/consultancy/users").header("Authorization", "Bearer " + consultant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "coleg" + UUID.randomUUID().toString().substring(0, 6) + "@cabinet.ro",
                                "firstName", "x".repeat(300), "lastName", "y"))))
                .andReturn().getResponse().getStatus();
        assertThat(status).as("prenume de 300 de semne").isBetween(400, 499);
    }

    /**
     * „Client nou” (consultant sau platformă): adminul invitat vine în {@code OnboardClientRequest.Admin}, fără
     * {@code @Size}, iar serviciul construiește singur {@code InviteUserRequest}, deci limitele lui nu se aplică.
     */
    @Test
    void onboardingAClientWithALongAdminNameIsNotAServerError() throws Exception {
        Consultancy cabinet = consultancyRepository.save(Consultancy.builder()
                .name("Cabinet Onboard QA").cui("RO" + ThreadLocalRandom.current().nextLong(10_000_000L, 9_999_999_999L))
                .createdAt(Instant.now()).build());
        String consultant = jwtService.generateToken(consultant(cabinet));
        String body = objectMapper.writeValueAsString(Map.of(
                "company", Map.of("name", "Client Lung SRL", "cui", TestCui.random(), "type", "GENERATOR"),
                "admin", Map.of("email", "admin" + UUID.randomUUID().toString().substring(0, 6) + "@client.ro",
                        "firstName", "x".repeat(300), "lastName", "Y")));
        MvcResult r = mockMvc.perform(post("/api/v1/companies/onboard").header("Authorization", "Bearer " + consultant)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
        System.out.println("QA onboard long admin name → " + r.getResponse().getStatus() + " " + r.getResponse().getContentAsString());
        assertThat(r.getResponse().getStatus()).as("prenumele adminului de 300 de semne").isBetween(400, 499);
    }

    /** Parola aleasă din link (invitație sau reset): 73–100 de semne trec de {@code @Size(max = 100)}. */
    @Test
    void aLongPassphraseOnTheInvitationLinkIsNotAServerError() throws Exception {
        String email = "lunga+" + UUID.randomUUID().toString().substring(0, 8) + "@client.ro";
        mockMvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + adminA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "role", "OPERATOR",
                        "firstName", "Ana", "lastName", "Lunga")))).andReturn();
        String code = lastInviteCode();
        String pass = "Aa1" + "o parolă lungă, o propoziție întreagă ".repeat(3).substring(0, 90);
        int status = mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", code, "password", pass, "confirmPassword", pass))))
                .andReturn().getResponse().getStatus();
        System.out.println("QA reset with " + pass.getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                + "-byte password → " + status);
        assertThat(status).as("parolă de " + pass.length() + " semne").isLessThan(500);
    }

    /** Login cu o parolă foarte lungă (fără {@code @Size} pe LoginRequest.password). */
    @Test
    void aVeryLongLoginPasswordIsNotAServerError() throws Exception {
        int status = mockMvc.perform(post("/api/v1/auth/login")
                        .with(r -> { r.setRemoteAddr("198.51.100.9-" + UUID.randomUUID()); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "admin@demo.ro", "password", "Aa1" + "x".repeat(200)))))
                .andReturn().getResponse().getStatus();
        assertThat(status).as("login cu parolă de 203 semne").isLessThan(500);
    }

    // ───────────────────────── fișiere ─────────────────────────

    /**
     * Logoul de cabinet: 500 KB comprimați, dar niciun prag pe pixeli. ImageIO decodează imaginea întreagă la
     * încărcare și la fiecare raport cu antet; un PNG uniform de 8000×8000 are ~70 KB și cere 64 MB de heap.
     */
    @Test
    void aLogoThatDecodesToTensOfMegabytesIsRefused() throws Exception {
        byte[] bomb = grayPng(8000, 8000);
        byte[] big = grayPng(20000, 20000);
        System.out.println("QA logo 8000² = " + bomb.length + " B (decodat 64 MB); 20000² = " + big.length
                + " B (decodat 400 MB), limita = " + ReportBrandingService.MAX_LOGO_BYTES);
        assertThat(big.length).as("20000×20000 încape sub limita de octeți").isLessThan(ReportBrandingService.MAX_LOGO_BYTES);

        Consultancy cabinet = consultancyRepository.save(Consultancy.builder()
                .name("Cabinet Logo QA").cui("RO" + ThreadLocalRandom.current().nextLong(10_000_000L, 9_999_999_999L))
                .createdAt(Instant.now()).build());
        String consultant = jwtService.generateToken(consultant(cabinet));
        int status = mockMvc.perform(multipart("/api/v1/consultancy/branding/logo")
                        .file(new MockMultipartFile("file", "logo.png", "image/png", bomb))
                        .header("Authorization", "Bearer " + consultant))
                .andReturn().getResponse().getStatus();
        assertThat(status).as("logo 8000×8000 (64 MB decodat)").isEqualTo(400);
    }

    /** Tipul declarat al atașamentului intră în {@code content_type VARCHAR(128)} după urcarea la Cloudinary. */
    @Test
    void anAttachmentWithAnOverlongContentTypeIsRefusedBeforeItIsUploaded() throws Exception {
        org.mockito.Mockito.when(storageService.upload(any(), anyString())).thenReturn(
                new CloudinaryStorageService.StoredFile("https://x", "movements/x/y", "raw", "authenticated", null));
        String id = createMovement(operatorA, handover("2026-07-06", ""));
        String longType = "application/" + "x".repeat(200);
        int status = mockMvc.perform(multipart("/api/v1/movements/" + id + "/attachments")
                        .file(new MockMultipartFile("file", "aviz.pdf", longType, "%PDF-1.4".getBytes()))
                        .header("Authorization", "Bearer " + operatorA))
                .andReturn().getResponse().getStatus();
        System.out.println("QA attachment content-type 212 chars → " + status + "; uploaded to storage first: "
                + !org.mockito.Mockito.mockingDetails(storageService).getInvocations().stream()
                        .filter(i -> i.getMethod().getName().equals("upload")).toList().isEmpty()
                + ", deleted after: " + !org.mockito.Mockito.mockingDetails(storageService).getInvocations().stream()
                        .filter(i -> i.getMethod().getName().equals("delete")).toList().isEmpty());
        assertThat(status).as("content-type de 212 semne").isBetween(400, 499);
    }

    // ───────────────────────── fluxuri de autentificare ─────────────────────────

    /** Retrimiterea cu mailul căzut dă eroare; linkul vechi trebuie să meargă în continuare (rollback). */
    @Test
    void aFailedResendLeavesTheOldInvitationLinkWorking() throws Exception {
        String email = "retrimis+" + UUID.randomUUID().toString().substring(0, 8) + "@client.ro";
        String id = objectMapper.readTree(mockMvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + adminA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "role", "OPERATOR",
                        "firstName", "Ana", "lastName", "Retrimis")))).andReturn().getResponse().getContentAsString())
                .get("id").asText();
        String oldCode = lastInviteCode();
        doThrow(new EmailException(ErrorMessageEnum.EMAIL_SEND_FAILED))
                .when(emailService).sendInviteEmail(any(), anyString(), anyInt());

        int resend = mockMvc.perform(post("/api/v1/users/" + id + "/resend-invite").header("Authorization", "Bearer " + adminA))
                .andReturn().getResponse().getStatus();
        assertThat(resend).isEqualTo(400);

        int chosen = mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", oldCode,
                                "password", "Parola2026sigura", "confirmPassword", "Parola2026sigura"))))
                .andReturn().getResponse().getStatus();
        assertThat(chosen).as("linkul vechi după o retrimitere eșuată").isEqualTo(200);
    }

    /**
     * Login pe un cont dezactivat sau o invitație nefolosită, cu o parolă greșită: răspunsul spune starea contului
     * înainte să verifice parola, deci oricine află că adresa are cont și în ce stare e.
     */
    @Test
    void aWrongPasswordOnADeactivatedAccountAnswersLikeAnyWrongPassword() throws Exception {
        AppUser off = user(a, Role.OPERATOR);
        off.setEnabled(false);
        off.setDeactivatedAt(Instant.now());
        appUserRepository.save(off);
        AppUser pending = user(a, Role.OPERATOR);
        pending.setEnabled(false);
        appUserRepository.save(pending);

        String unknown = loginError("nimeni+" + UUID.randomUUID() + "@example.ro");
        String deactivated = loginError(off.getEmail());
        String invited = loginError(pending.getEmail());
        System.out.println("QA login wrong password: unknown=" + unknown + " deactivated=" + deactivated + " pending=" + invited);
        assertThat(deactivated).as("cont dezactivat, parolă greșită").isEqualTo(unknown);
        assertThat(invited).as("invitație nefolosită, parolă greșită").isEqualTo(unknown);
    }

    /**
     * „Parolă uitată” pe adresa unei invitații nefolosite șterge linkul de 7 zile din invitație: oricine știe adresa
     * îl poate anula fără cont (omul primește în schimb un link de 30 de minute).
     */
    @Test
    void anAnonymousResetRequestDoesNotKillAPendingInvitationLink() throws Exception {
        String email = "anulat+" + UUID.randomUUID().toString().substring(0, 8) + "@client.ro";
        mockMvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + adminA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "role", "OPERATOR",
                        "firstName", "Ana", "lastName", "Anulat")))).andReturn();
        String inviteCode = lastInviteCode();

        mockMvc.perform(post("/api/v1/auth/request-reset-password")
                .with(r -> { r.setRemoteAddr("198.51.100.11-" + UUID.randomUUID()); return r; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email)))).andReturn();

        int chosen = mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", inviteCode,
                                "password", "Parola2026sigura", "confirmPassword", "Parola2026sigura"))))
                .andReturn().getResponse().getStatus();
        assertThat(chosen).as("linkul din invitație după un „Parolă uitată” anonim").isEqualTo(200);
    }

    /** Controlul: invitația unei adrese care are deja cont în ALTĂ firmă spune „există deja” (enumerare între firme). */
    @Test
    void characterizes_anAdminLearnsThatAnAddressHasAnAccountInAnotherFirm() throws Exception {
        Company b = company("Beta QA SRL", null);
        AppUser inB = user(b, Role.OPERATOR);
        MvcResult r = mockMvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + adminA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", inB.getEmail(), "role", "OPERATOR",
                        "firstName", "X", "lastName", "Y")))).andReturn();
        System.out.println("QA invite other-tenant email → " + r.getResponse().getStatus() + " " + r.getResponse().getContentAsString());
        assertThat(r.getResponse().getContentAsString()).contains("account");
        verify(emailService, never()).sendInviteEmail(org.mockito.ArgumentMatchers.argThat(u -> u.getEmail().equals(inB.getEmail())), anyString(), anyInt());
    }

    // ───────────────────────── izolare pe rutele din afara matricei ─────────────────────────

    /** Vehicule și sortimente (depozit): un id din firma B nu se citește, nu se scrie, nu se șterge din A. */
    @Test
    void depotRowsOfAnotherFirmCannotBeTouched() throws Exception {
        Company b = company("Beta Depozit SRL", null);
        String adminB = jwtService.generateToken(user(b, Role.ADMIN));
        String vehicle = objectMapper.readTree(mockMvc.perform(post("/api/v1/vehicles").header("Authorization", "Bearer " + adminB)
                .contentType(MediaType.APPLICATION_JSON).content("{\"registration\":\"BH 01 QAA\",\"kind\":\"Camion\"}"))
                .andReturn().getResponse().getContentAsString()).path("id").asText();
        String person = objectMapper.readTree(mockMvc.perform(post("/api/v1/natural-persons").header("Authorization", "Bearer " + adminB)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Pers B\",\"cnp\":\"" + CNP + "\"}"))
                .andReturn().getResponse().getContentAsString()).path("id").asText();
        assertThat(vehicle).isNotBlank();
        assertThat(person).isNotBlank();

        assertThat(status(put("/api/v1/vehicles/" + vehicle), adminA, "{\"registration\":\"BH 99 HAK\"}")).isEqualTo(404);
        assertThat(status(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/vehicles/" + vehicle + "/definitiv"), adminA, null)).isEqualTo(404);
        assertThat(status(get("/api/v1/natural-persons/" + person), adminA, null)).isEqualTo(404);
        assertThat(status(put("/api/v1/natural-persons/" + person), adminA, "{\"name\":\"Furat\"}")).isEqualTo(404);
        String listA = mockMvc.perform(get("/api/v1/natural-persons").header("Authorization", "Bearer " + adminA))
                .andReturn().getResponse().getContentAsString();
        assertThat(listA).doesNotContain(person);
    }

    // ───────────────────────── helpers ─────────────────────────

    private int status(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b, String token, String json) throws Exception {
        b.header("Authorization", "Bearer " + token);
        if (json != null) {
            b.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(b).andReturn().getResponse().getStatus();
    }

    private String loginError(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .with(r -> { r.setRemoteAddr("198.51.100.12-" + UUID.randomUUID()); return r; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "Gresita1234"))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("error-code").asText();
    }

    private String createMovement(String token, String json) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/movements").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    private String handover(String date, String extra) {
        return """
                {"workPointId": "%s", "date": "%s", "wasteCodeId": "%s", "quantity": 5,
                 "unit": "KG", "physicalState": "SOLID", "operation": "RECOVERED",
                 "physicalState": "SOLID", "storageType": "CT", "transportMeans": "AN", "packagingCategory": "SECONDARY", "wasteDestination": "Vr", "operationCode": "R13", "partnerId": "%s"%s}
                """.formatted(workPointA, date, wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId(), partnerA, extra);
    }

    private String pdfText(String url, String token) throws Exception {
        MvcResult r = mockMvc.perform(get(url).header("Authorization", "Bearer " + token)).andReturn();
        assertThat(r.getResponse().getStatus()).as(url).isEqualTo(200);
        PdfReader reader = new PdfReader(r.getResponse().getContentAsByteArray());
        try {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(new PdfTextExtractor(reader).getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    private String lastInviteCode() {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendInviteEmail(any(), code.capture(), anyInt());
        return code.getValue();
    }

    private Company company(String name, Consultancy consultancy) {
        return companyRepository.save(Company.builder()
                .name(name).cui(TestCui.random()).type(CompanyType.GENERATOR).consultancy(consultancy)
                .active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Company c, Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + UUID.randomUUID().toString().substring(0, 8) + "@qa.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(role).company(c).enabled(true).createdAt(Instant.now()).build());
    }

    private AppUser consultant(Consultancy consultancy) {
        return appUserRepository.save(AppUser.builder()
                .email("consultant+" + UUID.randomUUID().toString().substring(0, 8) + "@cabinet.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.CONSULTANT).consultancy(consultancy).enabled(true).createdAt(Instant.now()).build());
    }

    /** Un PNG gri de 8 biți, uniform, scris în flux: nu ține imaginea în memorie ca s-o producă. */
    private static byte[] grayPng(int width, int height) throws Exception {
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        try (DeflaterOutputStream z = new DeflaterOutputStream(idat, new Deflater(9), 1 << 16)) {
            byte[] row = new byte[width + 1];
            for (int y = 0; y < height; y++) {
                z.write(row);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(out);
        d.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        DataOutputStream h = new DataOutputStream(ihdr);
        h.writeInt(width);
        h.writeInt(height);
        h.writeByte(8);  // bit depth
        h.writeByte(0);  // grayscale
        h.writeByte(0);
        h.writeByte(0);
        h.writeByte(0);
        chunk(d, "IHDR", ihdr.toByteArray());
        chunk(d, "IDAT", idat.toByteArray());
        chunk(d, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void chunk(DataOutputStream d, String type, byte[] data) throws Exception {
        d.writeInt(data.length);
        byte[] t = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        d.write(t);
        d.write(data);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(data);
        d.writeInt((int) crc.getValue());
    }
}
