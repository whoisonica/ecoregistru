package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.EvidenceCalculator;
import ro.ecoregistru.service.export.Anexa1Sheet;
import ro.ecoregistru.service.export.AnnualDeclaration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1.7 and P1.8 of the pre-launch QA audit: <b>a document cannot print figures older than the
 * movements it is made of</b>, and a write sent twice does not become two.
 *
 * <p>{@code EvidenceCalculatorIT} already proves that {@code list()} rebuilds a stale year. What it
 * does not prove is the half that reaches a client: the <b>printed documents</b> call {@code list()}
 * by self-invocation, and that path has a trap of its own written into its javadoc — under
 * {@code readOnly = true} Hibernate would drop the rebuilt lines at commit and the sheet would print
 * the stale figures <em>silently</em>. Nothing asserted that today, so the tests below go through
 * {@code anexa1()} and {@code annualDeclaration()} rather than through the cache.
 *
 * <p>The idempotency half (P1.8) covers {@code clientGeneratedId}, which the service has honoured
 * since B1 and which <b>no test touched</b> — the API was designed for a phone on bad signal, where
 * a retried POST is the ordinary case, not the exotic one.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class EvidenceFreshnessIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EvidenceCalculator evidenceCalculator;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;

    private UUID tenantId;
    private String token;
    private WorkPoint workPoint;
    private WasteCode code;
    private Partner collector;
    private UUID creatorId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Freshness SRL").cui("ROF" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("fresh+" + suffix + "@demo.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        creatorId = admin.getId();
        token = jwtService.generateToken(admin);
        workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Freshness").active(true).createdAt(Instant.now()).build());
        code = wasteCodeRepository.findAll().get(0);
        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector SRL").cui("RO" + suffix)
                .type(PartnerType.COLLECTOR).supplier(true).active(true)
                .createdAt(Instant.now()).build());
        TenantContext.set(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---------- P1.7: the printed document is never behind ----------

    /**
     * The failure this guards is the expensive one and it already happened once: a client records
     * three handovers, presses "Evidenţa gestiunii deşeurilor", and files a PDF without them at the
     * agency. Nothing on screen says anything is wrong.
     */
    @Test
    void theSheetPrintsAMovementRecordedAfterTheLastRebuild() {
        generate(LocalDate.of(2026, 3, 10), "100.000");
        evidenceCalculator.regenerateYear(2026);

        generate(LocalDate.of(2026, 3, 20), "400.000"); // nobody presses anything

        Anexa1Sheet sheet = onlySheet(2026);
        assertThat(march(sheet).generated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("500.000"));
        assertThat(march(sheet).closingStock()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("500.000"));
    }

    /** A deletion is a change like any other — and ours is soft, so it is the easier one to miss. */
    @Test
    void theSheetDropsAMovementDeletedAfterTheLastRebuild() {
        generate(LocalDate.of(2026, 3, 10), "100.000");
        WasteMovement extra = generate(LocalDate.of(2026, 3, 20), "400.000");
        evidenceCalculator.regenerateYear(2026);

        extra.setDeleted(true);
        extra.setDeletedAt(Instant.now());
        movementRepository.saveAndFlush(extra);

        assertThat(march(onlySheet(2026)).closingStock()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("100.000"));
    }

    /**
     * The two documents of a filing read the same cached lines on purpose (decision 47: two
     * generators that calculate are two chances to differ). This checks they also <b>refresh
     * together</b> — a stale sheet next to a fresh declaration is the same contradiction, arriving
     * by a different road.
     */
    @Test
    void theSheetAndTheAnnualDeclarationAgreeAfterTheSameCorrection() {
        generate(LocalDate.of(2026, 3, 10), "100.000");
        evidenceCalculator.regenerateYear(2026);
        generate(LocalDate.of(2026, 3, 20), "400.000");

        BigDecimal onTheSheet = december(onlySheet(2026)).closingStock();
        List<AnnualDeclaration> declarations = evidenceCalculator.annualDeclaration(2026, workPoint.getId());
        assertThat(declarations).hasSize(1);
        assertThat(declarations.get(0).rows()).hasSize(1);
        assertThat(declarations.get(0).rows().get(0).closingStock())
                .usingComparator(BigDecimal::compareTo).isEqualTo(onTheSheet);
        assertThat(onTheSheet).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("500.000"));
    }

    /**
     * A correction on a closed year has to reach the years after it, because the stock carries.
     * Proved on the <b>printed</b> opening stock — the header rubric an inspector reads first.
     */
    @Test
    void aCorrectionOnAnEarlierYearMovesTheLaterYearsOpeningStock() {
        generate(LocalDate.of(2025, 6, 10), "1000.000");
        evidenceCalculator.regenerateYear(2025);
        assertThat(onlySheet(2026).openingStock()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("1000.000"));

        generate(LocalDate.of(2025, 7, 10), "250.000"); // back-dated, into a closed year

        assertThat(onlySheet(2026).openingStock()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("1250.000"));
    }

    // ---------- P1.8: a write sent twice ----------

    /**
     * The phone-on-bad-signal case the API was designed for: the request arrives, the response does
     * not, the client retries. One movement, not two — and the same one back.
     */
    @Test
    void theSameMovementSentTwiceIsCreatedOnce() throws Exception {
        String clientId = UUID.randomUUID().toString();

        String firstId = postMovement(clientId, "5.000");
        String secondId = postMovement(clientId, "5.000");

        assertThat(secondId).isEqualTo(firstId);
        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(tenantId)).hasSize(1);
    }

    /**
     * And the retry does <b>not</b> become an edit. The service returns the stored movement
     * untouched, so a client that retries with a corrected quantity gets the original back rather
     * than silently rewriting a figure that may already be printed on a filed document. Correcting
     * a movement is {@code PUT}, which is audited; this road is not.
     */
    @Test
    void aRetryWithADifferentQuantityDoesNotRewriteTheFirst() throws Exception {
        String clientId = UUID.randomUUID().toString();
        postMovement(clientId, "5.000");

        postMovement(clientId, "9999.000");

        List<WasteMovement> all = movementRepository.findAllByCompany_IdAndDeletedFalse(tenantId);
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getQuantity()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("5.000"));
    }

    /**
     * The negative control, and the one that makes the two above mean something: without a client
     * id, two identical requests are two movements. A service that deduplicated on content would
     * pass both tests above and quietly swallow the second of two lorries loaded the same day.
     */
    @Test
    void twoIdenticalMovementsWithoutAClientIdAreBothCreated() throws Exception {
        postMovement(null, "5.000");
        postMovement(null, "5.000");

        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(tenantId)).hasSize(2);
    }

    /** Idempotency is scoped to the tenant, like everything else that reads by a client-chosen key. */
    @Test
    void theSameClientIdInAnotherCompanyIsADifferentMovement() throws Exception {
        String clientId = UUID.randomUUID().toString();
        postMovement(clientId, "5.000");

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company other = companyRepository.save(Company.builder()
                .name("Cealaltă SRL").cui("ROX" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser otherAdmin = appUserRepository.save(AppUser.builder()
                .email("alt+" + suffix + "@demo.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(other).enabled(true).createdAt(Instant.now()).build());
        UUID otherWorkPoint = workPointRepository.save(WorkPoint.builder()
                .company(other).name("PL Alt").active(true).createdAt(Instant.now()).build()).getId();

        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + jwtService.generateToken(otherAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(otherWorkPoint, clientId, "5.000")))
                .andExpect(status().isOk());

        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(tenantId)).hasSize(1);
        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(other.getId())).hasSize(1);
    }

    // --- helpers ---

    private String postMovement(String clientGeneratedId, String qty) throws Exception {
        String json = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(workPoint.getId(), clientGeneratedId, qty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        return json.replaceAll("(?s).*?\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    private String body(UUID workPointId, String clientGeneratedId, String qty) {
        return """
                {
                  "workPointId": "%s",
                  "date": "2026-07-05",
                  "wasteCodeId": "%s",
                  "quantity": %s,
                  "unit": "KG",
                  "physicalState": "SOLID",
                  "operation": "GENERATED",
                  "register": "ANEXA_1"%s
                }
                """.formatted(workPointId, code.getId(), qty,
                clientGeneratedId == null ? "" : ",\n  \"clientGeneratedId\": \"" + clientGeneratedId + "\"");
    }

    private WasteMovement generate(LocalDate date, String kg) {
        return movementRepository.saveAndFlush(WasteMovement.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .workPoint(workPoint).date(date).wasteCode(code)
                .quantity(new BigDecimal(kg)).unit(Unit.KG)
                .operation(WasteOperation.GENERATED).register(WasteRegister.ANEXA_1)
                .deleted(false).createdBy(creatorId).build());
    }

    private Anexa1Sheet onlySheet(int year) {
        List<Anexa1Sheet> sheets = evidenceCalculator.anexa1(year, workPoint.getId());
        assertThat(sheets).hasSize(1);
        return sheets.get(0);
    }

    private static Anexa1Sheet.Anexa1MonthRow march(Anexa1Sheet sheet) {
        return sheet.rows().get(2);
    }

    private static Anexa1Sheet.Anexa1MonthRow december(Anexa1Sheet sheet) {
        return sheet.rows().get(11);
    }
}
