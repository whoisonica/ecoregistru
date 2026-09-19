package ro.ecoregistru;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.persistence.EntityManager;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.WasteMovementRequest;
import ro.ecoregistru.controller.response.MonthlyEvidenceResponse;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.EvidenceCalculator;
import ro.ecoregistru.service.WasteMovementService;
import ro.ecoregistru.service.export.Anexa1Sheet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Post-fix QA (19.09, după 244837b + patch-ul BUG-031 cu EPOCH): căile surori ale reparației
 * cache-ului de evidență. Fiecare test e o probă care PICĂ pe codul de azi; oracolul e socotit de mână.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PostFixEvidenceProbeIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EvidenceCalculator evidenceCalculator;
    @Autowired WasteMovementService movementService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired PlatformTransactionManager txManager;
    @Autowired EntityManager entityManager;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    private UUID tenantId;
    private String token;
    private UUID point;
    private UUID paper;
    private UUID collector;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Proba SRL").cui("ROP" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("proba+" + suffix + "@demo.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        token = jwtService.generateToken(admin);
        adminId = admin.getId();
        point = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Probă").active(true).createdAt(Instant.now()).build()).getId();
        paper = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector Probă SRL").authorizationNumber("AP 1/2025").cui("RO" + suffix)
                .type(PartnerType.COLLECTOR).supplier(true).active(true)
                .createdAt(Instant.now()).build()).getId();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---------- 1. „Recalculează" / dosarul pe anul de după o corectură ----------

    /**
     * {@code regenerateYear(2026)} — butonul „Recalculează" și dosarul de control
     * ({@code AuditFileService.write}, care cheamă {@code regenerateYear(y)} de la primul an al
     * dosarului) — ia soldul de deschidere din liniile lui 2025 așa cum sunt în cache, fără să
     * verifice că 2025 e la zi. După BUG-031, 2025 e marcat EPOCH, dar 2026 reconstruit peste el iese
     * cu {@code generatedAt = acum}, deci proaspăt pe vecie; 2025 se repară la prima citire, 2026 nu.
     *
     * <p>De mână: 1000 kg generate în iunie 2025 (linie veche), 300 predate pe 15.12.2025 și mutate
     * pe 10.01.2026. 2025 se închide cu 1000, deci 2026 se deschide cu 1000 (nu cu 700).
     */
    @Test
    void regeneratingTheNextYearAfterAMoveBuildsItOnTheStaleOpeningBalance() throws Exception {
        legacyGenerated("2025-06-10", "1000");
        String december = handover("2025-12-15", "300");
        readEvidence(2025);
        readEvidence(2026);

        update(december, "2026-01-10", "300");
        mockMvc.perform(post("/api/v1/evidences/regenerate?year=2026")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertKg(onlySheet(2025).rows().get(11).closingStock(), "1000");
        assertKg(onlySheet(2026).openingStock(), "1000");
    }

    /**
     * Aceeași cauză, fără BUG-031: o corectură în același an (2025), apoi dosarul de control pe 2026.
     * Dosarul regenerează 2026 peste liniile lui 2025 rămase în urmă; 2026 iese „proaspăt".
     *
     * <p>De mână: 1000 generate în iunie 2025, predarea de 300 din decembrie ștearsă. 2025 se închide
     * cu 1000, 2026 se deschide cu 1000.
     */
    @Test
    void theDossierOfTheNextYearBuildsOnAStalePreviousYear() throws Exception {
        legacyGenerated("2025-06-10", "1000");
        String december = handover("2025-12-15", "300");
        readEvidence(2025);
        readEvidence(2026);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/movements/" + december)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/api/v1/audit-file?year=2026").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertKg(onlySheet(2025).rows().get(11).closingStock(), "1000");
        assertKg(onlySheet(2026).openingStock(), "1000");
    }

    // ---------- 2. Cursa: o corectură care se comite în timpul unei reconstrucții ----------

    /**
     * {@code update} nu ia {@code lockForRebuild}. O reconstrucție care citește mișcările înainte ca
     * corectura să se comită, dar își ia {@code generatedAt} după {@code updatedAt}-ul corecturii
     * (Hibernate îl pune la flush, înainte de commit), scrie cifrele vechi și le declară proaspete.
     *
     * <p>De mână: predarea din martie 2025 corectată de la 500 la 900 kg. Martie trebuie să arate 900.
     */
    @Test
    void anEditCommittedDuringARebuildIsLostForGood() throws Exception {
        String march = handover("2025-03-10", "500");
        readEvidence(2025);
        handover("2025-05-10", "100"); // 2025 învechit: următoarea citire reconstruiește

        runRace(march, "2025-03-10", "900", 2025, false);

        assertKg(month(2025, 3).totalRecovered(), "900");
    }

    /**
     * Varianta BUG-031: mutarea 2025 → 2026 marchează 2025 cu EPOCH (UPDATE pe liniile lui 2025),
     * iar o reconstrucție a lui 2025 care a citit deja mișcările așteaptă la DELETE, apoi scrie
     * linii noi, cu mișcarea mutată încă în decembrie, și {@code generatedAt} după mutare. Marcajul
     * EPOCH a fost pe rânduri care nu mai există; 2025 citit singur rămâne cu decembrie la 300.
     */
    @Test
    void aMoveIntoTheNextYearCommittedDuringARebuildOfTheOldYearIsLost() throws Exception {
        handover("2025-06-10", "1000");
        String december = handover("2025-12-15", "300");
        readEvidence(2025);
        handover("2025-05-10", "100"); // 2025 învechit

        runRace(december, "2026-01-10", "300", 2025, true);

        assertKg(month(2025, 12).totalRecovered(), "0");
    }

    // ---------- 3. BUG-039, sora: cantitatea admisă în tone umple coloanele evidenței ----------

    /**
     * {@code @Digits(integer = 11)} pe cantitate e lungimea coloanei {@code waste_movements.quantity},
     * dar evidența o înmulțește cu 1000 pentru tone, iar {@code monthly_evidences} are NUMERIC(16,3)
     * ({@code implied_generated} chiar NUMERIC(14,3)). Mișcarea se salvează cu 200; de atunci fiecare
     * citire a anului (Acasă, fișa, declarația, dosarul) cade cu 500 pentru toată firma.
     */
    @Test
    void aTonnageTheFormAcceptsBreaksEveryEvidenceReadOfTheYear() throws Exception {
        mockMvc.perform(post("/api/v1/movements").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("2026-02-10", "99999999999").replace("\"KG\"", "\"TONS\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']").value("movement.quantity.too.large"));

        mockMvc.perform(get("/api/v1/evidences?year=2026").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ---------- 4. BUG-032, sora: fișa Anexa 1 nu spune că o predare așteaptă cântarul ----------

    /**
     * BUG-032 a pus „(**)" și nota pe declarația anuală. Fișa Anexa 1 a aceluiași an, construită din
     * aceleași linii (cap. 1 „Generate" fără încărcătura necântărită, cap. 3 cu celula goală), nu
     * spune nicăieri că e provizorie: cele două documente depuse pe același an nu mai spun același lucru.
     */
    @org.junit.jupiter.api.Disabled("BC — nota „(**)” pe fișa Anexa 1 e întrebare la Andreea")
    @Test
    void theAnexa1SheetDoesNotSayAWeightIsMissingWhileTheDeclarationDoes() throws Exception {
        handover("2025-03-10", "1000");
        mockMvc.perform(post("/api/v1/movements").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workPointId": "%s", "date": "2025-04-10", "wasteCodeId": "%s",
                                 "unit": "KG", "physicalState": "SOLID", "weighedAtUnloading": true,
                                 "operation": "RECOVERED", "register": "ANEXA_1", "physicalState": "SOLID", "storageType": "CT", "transportMeans": "AN", "packagingCategory": "SECONDARY", "wasteDestination": "Vr",
                                 "operationCode": "R13", "partnerId": "%s"}
                                """.formatted(point, paper, collector)))
                .andExpect(status().isOk());

        assertThat(fold(pdfText("/api/v1/evidences/declaratie-anuala?year=2025"))).contains("cantar");
        assertThat(fold(pdfText("/api/v1/evidences/anexa1?year=2025"))).contains("cantar");
    }

    private String pdfText(String url) throws Exception {
        byte[] pdf = mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdf);
        try {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    private static String fold(String text) {
        return java.text.Normalizer.normalize(text.replace('\u00aa', 'a'), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
    }

    // --- cursa ---

    /**
     * A: corectura prin {@link WasteMovementService#update}, cu flush, ținută deschisă.
     * B: citirea anului, care reconstruiește. B se termină (sau se blochează, la {@code blocks}),
     * apoi A se comite.
     */
    private void runRace(String movementId, String newDate, String kg, int year, boolean blocks) throws Exception {
        WasteMovementRequest request = objectMapper.readValue(json(newDate, kg), WasteMovementRequest.class);
        CountDownLatch flushed = new CountDownLatch(1);
        CountDownLatch go = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        TransactionTemplate tx = new TransactionTemplate(txManager);

        Thread editor = new Thread(() -> {
            try {
                TenantContext.set(tenantId);
                tx.executeWithoutResult(s -> {
                    movementService.update(UUID.fromString(movementId), request);
                    entityManager.flush();
                    flushed.countDown();
                    try {
                        go.await(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (Throwable t) {
                failure.set(t);
                flushed.countDown();
            } finally {
                TenantContext.clear();
            }
        });
        editor.start();
        assertThat(flushed.await(30, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(20);

        Thread reader = new Thread(() -> {
            try {
                TenantContext.set(tenantId);
                evidenceCalculator.list(year, null, null);
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                TenantContext.clear();
            }
        });
        reader.start();
        if (blocks) {
            // B trebuie să fi citit mișcările și să aștepte la DELETE pe rândurile ținute de A.
            long deadline = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < deadline && jdbc.queryForObject(
                    "select count(*) from pg_stat_activity where wait_event_type = 'Lock'", Integer.class) == 0) {
                Thread.sleep(20);
            }
            assertThat(jdbc.queryForObject(
                    "select count(*) from pg_stat_activity where wait_event_type = 'Lock'", Integer.class))
                    .as("reconstrucția așteaptă corectura").isPositive();
        } else {
            reader.join(30_000);
        }
        go.countDown();
        editor.join(30_000);
        reader.join(30_000);
        assertThat(failure.get()).isNull();
    }

    // --- helpers ---

    private String json(String date, String kg) {
        return """
                {"workPointId": "%s", "date": "%s", "wasteCodeId": "%s", "quantity": %s,
                 "unit": "KG", "physicalState": "SOLID", "operation": "RECOVERED", "register": "ANEXA_1",
                 "physicalState": "SOLID", "storageType": "CT", "transportMeans": "AN", "packagingCategory": "SECONDARY", "wasteDestination": "Vr", "operationCode": "R13", "partnerId": "%s"}
                """.formatted(point, date, paper, kg, collector);
    }

    private String handover(String date, String kg) throws Exception {
        String body = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(date, kg)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private void update(String id, String date, String kg) throws Exception {
        mockMvc.perform(put("/api/v1/movements/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(date, kg)))
                .andExpect(status().isOk());
    }

    private void legacyGenerated(String date, String kg) {
        movementRepository.saveAndFlush(WasteMovement.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .workPoint(workPointRepository.getReferenceById(point))
                .date(java.time.LocalDate.parse(date)).wasteCode(wasteCodeRepository.getReferenceById(paper))
                .quantity(new BigDecimal(kg)).unit(Unit.KG)
                .operation(WasteOperation.GENERATED).register(WasteRegister.ANEXA_1)
                .deleted(false).createdBy(adminId).build());
    }

    private void readEvidence(int year) throws Exception {
        mockMvc.perform(get("/api/v1/evidences?year=" + year).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private Anexa1Sheet onlySheet(int year) {
        TenantContext.set(tenantId);
        List<Anexa1Sheet> sheets = evidenceCalculator.anexa1(year, point);
        assertThat(sheets).hasSize(1);
        return sheets.get(0);
    }

    private MonthlyEvidenceResponse month(int year, int month) {
        TenantContext.set(tenantId);
        List<MonthlyEvidenceResponse> lines = evidenceCalculator.list(year, month, point);
        assertThat(lines).hasSize(1);
        return lines.get(0);
    }

    private static void assertKg(BigDecimal actual, String expected) {
        assertThat(actual).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal(expected));
    }
}
