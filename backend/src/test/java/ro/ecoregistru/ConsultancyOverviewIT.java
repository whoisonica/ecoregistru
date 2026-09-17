package ro.ecoregistru;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.service.EmailService;
import ro.ecoregistru.service.notification.NotificationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P2.13, felia 2 — panoul „Toate firmele mele" și ce s-a adăugat echipei cabinetului.
 *
 * <p><b>Scena.</b> Cabinetul Anei are patru firme: „Busy" (termene depășite, blocaje, parteneri care
 * expiră), „Calm" (nimic de făcut), „Silent" (termenele anului negenerate) și „Inactive" (dezactivată).
 * Un cabinet vecin are „Foreign". Numele nu se conțin unul pe altul, ca o scurgere să se vadă în corp.
 *
 * <p>Cifrele din rândul lui Busy sunt construite câte una pe regulă, cu câte un vecin care <em>nu</em>
 * trebuie numărat lângă fiecare: un termen finalizat lângă cele depășite, o mișcare ștearsă lângă
 * codul-oglindă, un partener cu autorizația departe lângă cel care expiră.
 */
@SpringBootTest(properties = "app.deadlines.missed-shown-from=2026-01-01")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ConsultancyOverviewIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired ConsultancyRepository consultancyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired ReportingDeadlineRepository deadlineRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired NotificationService notificationService;
    @Autowired TemplateEngine templateEngine;
    @Autowired VerificationRecordRepository verificationRecordRepository;

    @MockitoBean EmailService emailService;

    private final LocalDate today = LocalDate.now();

    private Consultancy cabinet;
    private Company busy;
    private AppUser ana;
    private String anaToken;
    private String busyAdminToken;
    private String platformToken;

    @BeforeEach
    void setUp() {
        cabinet = consultancy("Cabinet Panou");
        Consultancy neighbour = consultancy("Cabinet Vecin");
        ana = consultant("ana", cabinet, true);
        anaToken = jwtService.generateToken(ana);

        busy = company("Busy SRL", cabinet, true);
        Company calm = company("Calm SRL", cabinet, true);
        company("Silent SRL", cabinet, true);
        Company inactive = company("Inactive SRL", cabinet, false);
        Company foreign = company("Foreign SRL", neighbour, true);

        // Busy — un depășit (cel din anul trecut e de dinainte de regula din 16.09 și nu se numără), unul
        // finalizat care nu contează, următorul peste 5 zile.
        deadline(busy, today.minusDays(10), DeadlineStatus.UPCOMING);
        deadline(busy, LocalDate.of(2025, 12, 1), DeadlineStatus.UPCOMING);
        deadline(busy, today.minusDays(3), DeadlineStatus.DONE);
        deadline(busy, today.plusDays(5), DeadlineStatus.UPCOMING);
        deadline(busy, today.plusDays(9), DeadlineStatus.UPCOMING);

        WorkPoint wp = workPointRepository.save(WorkPoint.builder()
                .company(busy).name("PL Busy").active(true).createdAt(Instant.now()).build());
        Partner collector = partner(busy, "Colector Busy", null, null, true);
        WasteCode plain = wasteCodeRepository.findAll().stream()
                .filter(c -> c.getMirrorOf() == null).findFirst().orElseThrow();
        WasteCode mirror = wasteCodeRepository.findAll().stream()
                .filter(c -> c.getMirrorOf() != null).findFirst().orElseThrow();

        // O ieșire fără cod R/D și una care așteaptă cântarul destinatarului, pe aceeași linie de evidență.
        movement(busy, wp, plain, today, new BigDecimal("100"), WasteOperation.UNCLASSIFIED_OUT, null, collector, false);
        movement(busy, wp, plain, today, null, WasteOperation.RECOVERED, WasteOperationCode.R3, collector, false);
        // Cod-oglindă fără document: una contează; cea ștearsă și cea de anul trecut nu.
        movement(busy, wp, mirror, today, new BigDecimal("50"), WasteOperation.GENERATED, null, null, false);
        movement(busy, wp, mirror, today, new BigDecimal("50"), WasteOperation.GENERATED, null, null, true);
        movement(busy, wp, mirror, LocalDate.of(today.getYear() - 1, 6, 1), new BigDecimal("50"),
                WasteOperation.GENERATED, null, null, false);

        // Parteneri: autorizația peste 30 de zile și viza peste 10 contează; autorizația peste 90 și unul inactiv, nu.
        partner(busy, "Expira Autorizatia", today.plusDays(30), null, true);
        partner(busy, "Expira Viza", today.plusDays(200), today.plusDays(10), true);
        partner(busy, "Departe", today.plusDays(90), null, true);
        partner(busy, "Inactiv Expirat", today.minusDays(5), null, false);

        deadline(calm, today.plusDays(20), DeadlineStatus.UPCOMING);
        deadline(inactive, today.minusDays(1), DeadlineStatus.UPCOMING);
        deadline(foreign, today.minusDays(1), DeadlineStatus.UPCOMING);

        AppUser busyAdmin = appUserRepository.save(AppUser.builder()
                .email("admin+" + suffix() + "@busy.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(busy).enabled(true).createdAt(Instant.now()).build());
        busyAdminToken = jwtService.generateToken(busyAdmin);
        platformToken = jwtService.generateToken(
                appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Panoul
    // ─────────────────────────────────────────────────────────────────────────

    /** Firmele active ale cabinetului, cea cu termene depășite întâi, cea fără nimic de făcut la coadă. */
    @Test
    void theOverviewIsTheActivePortfolioMostUrgentFirst() throws Exception {
        assertThat(names(overview())).containsExactly("Busy SRL", "Silent SRL", "Calm SRL");
    }

    @Test
    void eachFigureIsCountedByTheRuleTheCompanyDashboardUses() throws Exception {
        JsonNode row = row("Busy SRL");

        // Doar cel ratat după regulă (MissedDeadlinePolicy, aici de la 01.01.2026); cel din 2025 nu se arată.
        assertThat(row.get("overdueDeadlines").asInt()).isEqualTo(1);
        assertThat(row.get("nextDeadline").get("dueDate").asText()).isEqualTo(today.plusDays(5).toString());
        assertThat(row.get("deadlinesGenerated").asBoolean()).isTrue();
        assertThat(row.get("linesWithoutOperationCode").asInt()).isEqualTo(1);
        assertThat(row.get("linesAwaitingWeighing").asInt()).isEqualTo(1);
        assertThat(row.get("unprovenMirrorMovements").asInt()).isEqualTo(1);
        assertThat(row.get("partnersExpiring").asInt()).isEqualTo(2);
    }

    /** „0 depășite" la o firmă fără termene generate nu e o veste bună, deci rândul spune de ce. */
    @Test
    void aCompanyWithoutGeneratedDeadlinesSaysSoInsteadOfLookingClear() throws Exception {
        JsonNode silent = row("Silent SRL");
        JsonNode calm = row("Calm SRL");

        assertThat(silent.get("deadlinesGenerated").asBoolean()).isFalse();
        assertThat(silent.get("nextDeadline").isNull()).isTrue();
        assertThat(calm.get("deadlinesGenerated").asBoolean()).isTrue();
        assertThat(calm.get("overdueDeadlines").asInt()).isZero();
        assertThat(calm.get("nextDeadline").get("dueDate").asText()).isEqualTo(today.plusDays(20).toString());
    }

    /** După ultimul termen al anului, următorul e la anul: firma are termene, nu „negenerate”. */
    @Test
    void aCompanyWhoseNextDeadlineIsNextYearHasItsDeadlinesGenerated() throws Exception {
        Company december = company("December SRL", cabinet, true);
        deadline(december, LocalDate.of(today.getYear() + 1, 3, 15), DeadlineStatus.UPCOMING);

        JsonNode row = row("December SRL");

        assertThat(row.get("deadlinesGenerated").asBoolean()).isTrue();
        assertThat(row.get("overdueDeadlines").asInt()).isZero();
        assertThat(row.get("nextDeadline").get("dueDate").asText())
                .isEqualTo(LocalDate.of(today.getYear() + 1, 3, 15).toString());
    }

    @Test
    void onlyAConsultantAsksAndNobodyElsesFirmsLeak() throws Exception {
        String body = mockMvc.perform(get("/api/v1/consultancy/overview").header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("Foreign").doesNotContain("Inactive");

        mockMvc.perform(get("/api/v1/consultancy/overview").header("Authorization", "Bearer " + busyAdminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/consultancy/overview").header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Echipa cabinetului
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void aPendingColleagueGetsTheInvitationAgain() throws Exception {
        AppUser pending = consultant("pending", cabinet, false);

        mockMvc.perform(post("/api/v1/consultancy/users/" + pending.getId() + "/resend-invite")
                        .header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isNoContent());

        verify(emailService).sendInviteEmail(argThat(u -> u.getId().equals(pending.getId())), anyString(), eq(7));
    }

    /**
     * 15.09.2026 — invitația văzută pe producție era mailul de resetare („Dacă nu tu ai făcut cererea,
     * ignoră”), valabil 30 de minute. Acum are mailul ei, iar linkul ține 7 zile.
     */
    @Test
    void theInvitationIsAnInvitationAndLastsAWeek() throws Exception {
        AppUser pending = consultant("invitat", cabinet, false);

        mockMvc.perform(post("/api/v1/consultancy/users/" + pending.getId() + "/resend-invite")
                        .header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isNoContent());

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendInviteEmail(argThat(u -> u.getId().equals(pending.getId())), code.capture(), eq(7));
        verify(emailService, never()).sendPasswordResetEmail(any(), anyString());
        VerificationRecord record = verificationRecordRepository
                .findByCodeAndVerificationRecordType(code.getValue(), VerificationRecordType.RESET_PASSWORD).orElseThrow();
        assertThat(record.getExpiresAt()).isAfter(java.time.LocalDateTime.now().plusDays(6));

        Context ctx = new Context();
        ctx.setVariable("firstName", "Ana");
        ctx.setVariable("organization", cabinet.getName());
        ctx.setVariable("validDays", 7);
        ctx.setVariable("resetUrl", "https://app.wastehouse.ro/reseteaza-parola?code=x");
        String html = templateEngine.process("mail/invite", ctx);
        assertThat(html).contains(cabinet.getName()).contains("7 zile").contains("reseteaza-parola?code=x")
                .doesNotContain("resetare").doesNotContain("ignoră");
    }

    /** Un coleg cu parolă are „Parolă uitată"; o invitație nouă ar fi un link de resetare pentru contul altuia. */
    @Test
    void anActiveColleagueIsNotReinvited() throws Exception {
        AppUser active = consultant("activ", cabinet, true);

        mockMvc.perform(post("/api/v1/consultancy/users/" + active.getId() + "/resend-invite")
                        .header("Authorization", "Bearer " + anaToken))
                .andExpect(status().is4xxClientError());

        verify(emailService, never()).sendInviteEmail(any(), anyString(), org.mockito.ArgumentMatchers.anyInt());
        verify(emailService, never()).sendPasswordResetEmail(any(), anyString());
    }

    @Test
    void cancellingAnInvitationFreesTheAddressButNotAcrossCabinets() throws Exception {
        AppUser pending = consultant("gresit", cabinet, false);
        AppUser neighbours = consultant("vecin", consultancy("Cabinet Trei"), false);

        mockMvc.perform(delete("/api/v1/consultancy/users/" + neighbours.getId() + "/invitation")
                        .header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/consultancy/users/" + pending.getId() + "/invitation")
                        .header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isNoContent());

        assertThat(appUserRepository.findById(pending.getId())).isEmpty();
        assertThat(appUserRepository.findById(neighbours.getId())).as("invitația celuilalt cabinet").isPresent();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mailul de rezumat, randat de-adevăratelea
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void theDigestMailNamesEachFirmAndDeadlineAndLinksToThePanel() {
        List<ReportingDeadline> due = deadlineRepository.findOpenForConsultancy(
                cabinet.getId(), today, today.plusDays(7));

        notificationService.sendConsultantDigest(cabinet.getName(), due, List.of("ana@cabinet.ro"), today);

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(emailService).send(eq("ana@cabinet.ro"), subject.capture(), eq("mail/consultant_digest"), context.capture());
        String html = templateEngine.process("mail/consultant_digest", context.getValue());

        assertThat(due).extracting(d -> d.getCompany().getName()).containsOnly("Busy SRL");
        assertThat(subject.getValue()).isEqualTo("Cabinet Panou: 1 termen în următoarele 7 zile");
        assertThat(html).contains("Busy SRL").contains("scadent în 5 zile").contains("/cabinet")
                .doesNotContain("Calm SRL").doesNotContain("Foreign");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private JsonNode overview() throws Exception {
        String body = mockMvc.perform(get("/api/v1/consultancy/overview").header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private JsonNode row(String name) throws Exception {
        for (JsonNode r : overview()) {
            if (r.get("name").asText().equals(name)) return r;
        }
        throw new AssertionError("lipsește rândul " + name);
    }

    private static List<String> names(JsonNode rows) {
        List<String> names = new ArrayList<>();
        rows.forEach(r -> names.add(r.get("name").asText()));
        return names;
    }

    private Consultancy consultancy(String name) {
        return consultancyRepository.save(Consultancy.builder()
                .name(name).cui(digitsCui()).createdAt(Instant.now()).build());
    }

    private Company company(String name, Consultancy consultancy, boolean active) {
        return companyRepository.save(Company.builder()
                .name(name).cui(digitsCui()).type(CompanyType.GENERATOR).consultancy(consultancy)
                .afmObligation(false).active(active).createdAt(Instant.now()).build());
    }

    private AppUser consultant(String name, Consultancy consultancy, boolean enabled) {
        return appUserRepository.save(AppUser.builder()
                .email(name + "+" + suffix() + "@cabinet.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.CONSULTANT).consultancy(consultancy)
                .enabled(enabled).createdAt(Instant.now()).build());
    }

    private void deadline(Company company, LocalDate dueDate, DeadlineStatus status) {
        deadlineRepository.save(ReportingDeadline.builder()
                .company(company).reportType(ReportType.SIM_ANNUAL).dueDate(dueDate).status(status)
                .warned7Days(false).warned1Day(false).createdAt(Instant.now()).build());
    }

    private Partner partner(Company company, String name, LocalDate expiry, LocalDate visaUntil, boolean active) {
        return partnerRepository.save(Partner.builder()
                .company(company).name(name).cui("RO" + suffix()).client(true).supplier(false).carrier(false)
                .type(PartnerType.COLLECTOR).authorizationExpiry(expiry).visaValidUntil(visaUntil)
                .active(active).createdAt(Instant.now()).build());
    }

    private void movement(Company company, WorkPoint wp, WasteCode code, LocalDate date, BigDecimal qty,
                          WasteOperation op, WasteOperationCode opCode, Partner partner, boolean deleted) {
        movementRepository.save(WasteMovement.builder()
                .company(company).workPoint(wp).date(date).wasteCode(code)
                .quantity(qty).weighedAtUnloading(qty == null).unit(Unit.KG)
                .operation(op).operationCode(opCode).partner(partner)
                .deleted(deleted).createdBy(ana.getId()).build());
    }

    private static String digitsCui() {
        return "RO" + ThreadLocalRandom.current().nextLong(10_000_000L, 9_999_999_999L);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
