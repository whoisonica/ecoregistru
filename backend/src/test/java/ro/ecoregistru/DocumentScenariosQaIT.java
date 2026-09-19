package ro.ecoregistru;

import com.jayway.jsonpath.JsonPath;
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
import ro.ecoregistru.service.PackagingService;
import ro.ecoregistru.service.export.Anexa1Sheet;
import ro.ecoregistru.service.export.AnnualDeclaration;
import ro.ecoregistru.service.export.PackagingAnexa3;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA după reparațiile BUG-031…044 (19.09.2026): scenarii deterministe, pe mai mulți ani, cu oracolul
 * socotit de mână în javadocul fiecărei probe.
 *
 * <p>Partea de stoc are nevoie de rânduri GENERATED vechi (dinainte de V58), singurele care mai pot
 * lăsa stoc la un generator; se scriu direct în bază, cum le-a lăsat istoria. Restul trece prin API.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DocumentScenariosQaIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EvidenceCalculator evidenceCalculator;
    @Autowired PackagingService packagingService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;

    private UUID tenantId;
    private String token;
    private UUID adminId;
    private UUID pointA;
    private UUID paper;
    private UUID collector;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Scenarii " + suffix + " SRL").cui(TestCui.random()).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("scen+" + suffix + "@demo.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        token = jwtService.generateToken(admin);
        adminId = admin.getId();
        pointA = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Centru " + suffix).active(true).createdAt(Instant.now()).build()).getId();
        paper = code("20 01 01");
        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector Scenarii SRL").authorizationNumber("AM 9/2024").cui("RO" + suffix)
                .type(PartnerType.COLLECTOR).supplier(true).active(true)
                .createdAt(Instant.now()).build()).getId();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---------- stoc vechi purtat peste un an fără mișcări ----------

    /**
     * De mână: 1000 kg generate (rând vechi) pe 10.05.2024, 400 predate pe 10.08.2024 → 2024 se închide cu
     * 600. 2025 n-are nicio mișcare: se deschide și se închide cu 600. 2026 se deschide cu 600.
     *
     * <p>Clientul deschide direct 2026 (un an pe care 2025 nu l-a citit nimeni încă).
     */
    @Test
    void aYearNobodyOpenedStillCarriesTheStockIntoTheYearAfter() throws Exception {
        legacyGenerated(pointA, paper, "2024-05-10", "1000");
        handover(pointA, paper, "2024-08-10", "400");
        readEvidence(2024);

        readEvidence(2026);
        readEvidence(2025);
        assertKg(onlySheet(2025, pointA).openingStock(), "600");

        assertThat(sheets(2026, pointA)).as("fișa 2026 a punctului cu 600 kg în stoc").hasSize(1);
        assertKg(onlySheet(2026, pointA).openingStock(), "600");
        assertKg(onlyDeclarationRow(2026, pointA).closingStock(), "600");
    }

    /**
     * Ca mai sus, dar 2026 are și o mișcare proprie: 100 kg predate pe 10.03.2026. De mână: 2026 deschide cu
     * 600, predarea iese din stoc (fără generare dedusă), 2026 se închide cu 500.
     */
    @Test
    void aYearNobodyOpenedCarriesTheStockEvenWhenTheNextYearHasMovements() throws Exception {
        legacyGenerated(pointA, paper, "2024-05-10", "1000");
        handover(pointA, paper, "2024-08-10", "400");
        handover(pointA, paper, "2026-03-10", "100");
        readEvidence(2024);

        readEvidence(2026);
        readEvidence(2025);
        readEvidence(2026);

        assertKg(onlySheet(2026, pointA).openingStock(), "600");
        assertKg(onlyDeclarationRow(2026, pointA).generated(), "0");
        assertKg(onlyDeclarationRow(2026, pointA).closingStock(), "500");
    }

    /** Controlul pozitiv: ani citiți în ordine (2024, 2025, 2026), aceleași cifre. */
    @Test
    void theSameStockArrivesWhenTheYearsAreReadInOrder() throws Exception {
        legacyGenerated(pointA, paper, "2024-05-10", "1000");
        handover(pointA, paper, "2024-08-10", "400");
        readEvidence(2024);
        readEvidence(2025);
        readEvidence(2026);

        assertKg(onlySheet(2026, pointA).openingStock(), "600");
        assertKg(onlyDeclarationRow(2026, pointA).closingStock(), "600");
    }

    // ---------- „Regenerează” și dosarul pe un an, peste un an anterior învechit ----------

    /**
     * De mână: 1000 kg generate (rând vechi) pe 10.03.2025; 2025 și 2026 citite (2026 deschide cu 1000).
     * Apoi se adaugă o predare de 400 kg pe 10.06.2025 → 2025 se închide cu 600, 2026 deschide cu 600.
     * Clientul apasă „Regenerează” pe 2026 (tabul „Totalul anului” al anului 2026).
     */
    @Test
    void regeneratingALaterYearUsesTheCorrectedPreviousYear() throws Exception {
        legacyGenerated(pointA, paper, "2025-03-10", "1000");
        readEvidence(2025);
        readEvidence(2026);
        assertKg(onlySheet(2026, pointA).openingStock(), "1000");

        handover(pointA, paper, "2025-06-10", "400");
        mockMvc.perform(post("/api/v1/evidences/regenerate?year=2026").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        readEvidence(2025);
        readEvidence(2026);

        assertKg(onlySheet(2025, pointA).rows().get(11).closingStock(), "600");
        assertKg(onlySheet(2026, pointA).openingStock(), "600");
    }

    /**
     * Același drum, dar în locul butonului: dosarul de control pe un singur an, 2026. Centralizata din
     * dosar are „Stoc la 01.01” 600, iar după descărcare ecranul 2026 spune tot 600.
     */
    @Test
    void aSingleYearDossierUsesTheCorrectedPreviousYear() throws Exception {
        legacyGenerated(pointA, paper, "2025-03-10", "1000");
        readEvidence(2025);
        readEvidence(2026);

        handover(pointA, paper, "2025-06-10", "400");
        Map<String, byte[]> zip = dossier(2026, 1);
        String central = Golden.flat(Golden.pdfText(zip.get("rapoarte/evidenta-centralizata-2026.pdf")));

        assertThat(central).as("centralizata 2026 din dosar")
                .contains(Golden.flat("20 01 01" + name("20 01 01") + "600.000" + "0.000" + "0.000" + "0.000" + "600.000"));
        readEvidence(2025);
        assertKg(onlySheet(2026, pointA).openingStock(), "600");
    }

    // ---------- anul din mijloc, după ce totul a plecat din el ----------

    /**
     * De mână: 1000 kg generate (rând vechi) pe 10.06.2024; 2024 și 2025 citite (2025 poartă 1000 în stoc).
     * Corectură: rândul era de fapt o predare de 1000 kg pe 10.01.2026. Nu mai rămâne nicio mișcare pe
     * 2024 sau 2025, deci fișa 2025 nu mai are nimic de tipărit. Clientul deschide întâi 2025.
     */
    @Test
    void theMiddleYearIsEmptyAfterItsOnlySourceMovedLater() throws Exception {
        UUID legacy = legacyGenerated(pointA, paper, "2024-06-10", "1000");
        readEvidence(2024);
        readEvidence(2025);
        assertKg(onlySheet(2025, pointA).openingStock(), "1000");

        mockMvc.perform(put("/api/v1/movements/" + legacy)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handoverJson(pointA, paper, "2026-01-10", "1000")))
                .andExpect(status().isOk());

        readEvidence(2025);
        assertThat(sheets(2025, pointA)).as("fișa 2025 după corectură").isEmpty();
    }

    // ---------- Anexa 3 Ambalaje la generator ----------

    /**
     * De mână: pe 2026, 200 kg 15 01 01 (hârtie, materialul decis de cod) și 500 kg 15 01 06 (amestec,
     * materialul neales — formularul doar avertizează), amândouă R3 la colector. Fișa Anexa 1 are 700 kg pe
     * cele două coduri. Anexa 3 a generatorului ori le raportează pe amândouă, ori o numește pe cea care
     * n-a putut intra (cum face cu ieșirile necântărite) — nu o pierde în tăcere.
     */
    @Test
    void anexa3AccountsForAPackagingExitWithNoMaterial() throws Exception {
        UUID mixed = code("15 01 06");
        handover(pointA, code("15 01 01"), "2026-04-10", "200", "R3");
        // Decizia 19.09.2026: prin API nu mai intră fără material…
        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handoverJson(pointA, mixed, "2026-05-10", "500").replace("\"R13\"", "\"R3\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']").value("movement.packaging.material.required"));
        // …dar un rând vechi, de dinainte, nu se pierde în tăcere.
        legacyHandoverWithoutMaterial(mixed, "2026-05-10", "500", "R3");

        TenantContext.set(tenantId);
        PackagingAnexa3 doc = packagingService.anexa3(2026, pointA);
        BigDecimal reported = doc.handovers().stream().map(PackagingAnexa3.HandoverRow::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean named = doc.unclassified().stream().anyMatch(u -> u.wasteCode().startsWith("15 01 06"));
        assertThat(reported.compareTo(new BigDecimal("700")) == 0 || named)
                .as("Anexa 3: raportat %s kg, 15 01 06 numit printre neclasificate: %s", reported, named)
                .isTrue();
    }

    /**
     * Varianta care se vede la control: singurul ambalaj al anului e 15 01 04 (metal, materialul neales).
     * Fișa Anexa 1 are 300 kg pe 15 01 04, dar dosarul n-are nicio Anexa 3 și nimic nu spune de ce.
     */
    @Test
    void theDossierDoesNotDropAnexa3ForAnUnclassifiedMetalExit() throws Exception {
        legacyHandoverWithoutMaterial(code("15 01 04"), "2026-04-10", "300", "R4");
        Map<String, byte[]> zip = dossier(2026, 1);

        assertThat(Golden.pdfText(zip.get("rapoarte/evidenta-gestiunii-deseurilor-2026.pdf"))).contains("15 01 04");
        boolean anexa3 = zip.keySet().stream().anyMatch(n -> n.contains("anexa3-ambalaje"));
        String contents = new String(zip.get("00-cuprins.txt"), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(anexa3).as("Anexa 3 în dosar pentru 300 kg de ambalaje predate; intrări: %s", zip.keySet()).isTrue();
        assertThat(contents).contains("o ieșire de ambalaj fără material nu apare pe Anexa 3");
    }

    // ---------- import din Excel, apoi retras ----------

    // (în DocumentImportRetractQaIT, care are nevoie de șablonul de import și de platformă)

    // ---------- helpers ----------

    private UUID code(String code) {
        return wasteCodeRepository.findByCode(code).orElseThrow().getId();
    }

    private String name(String code) {
        return wasteCodeRepository.findByCode(code).orElseThrow().getName();
    }

    private UUID legacyGenerated(UUID workPoint, UUID code, String date, String kg) {
        return movementRepository.saveAndFlush(WasteMovement.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .workPoint(workPointRepository.getReferenceById(workPoint))
                .date(LocalDate.parse(date)).wasteCode(wasteCodeRepository.getReferenceById(code))
                .quantity(new BigDecimal(kg)).unit(Unit.KG)
                .operation(WasteOperation.GENERATED).register(WasteRegister.ANEXA_1)
                .deleted(false).createdBy(adminId).build()).getId();
    }

    private String handover(UUID workPoint, UUID code, String date, String kg) throws Exception {
        return handover(workPoint, code, date, kg, "R13");
    }

    private String handover(UUID workPoint, UUID code, String date, String kg, String rCode) throws Exception {
        String json = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handoverJson(workPoint, code, date, kg).replace("\"R13\"", "\"" + rCode + "\"")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    /** O predare de dinainte de 19.09.2026, fără material: salvată cu material, golită apoi în bază. */
    private void legacyHandoverWithoutMaterial(UUID code, String date, String kg, String rCode) throws Exception {
        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handoverJson(pointA, code, date, kg).replace("\"R13\"", "\"" + rCode + "\"")
                                .replace("\"packagingCategory\"", "\"packagingMaterial\": \"ALTELE\", \"packagingCategory\"")))
                .andExpect(status().isOk());
        legacyWithout("packaging_material", wasteCodeRepository.findById(code).orElseThrow().getCode());
    }

    private String handoverJson(UUID workPoint, UUID code, String date, String kg) {
        return """
                {"workPointId": "%s", "date": "%s", "wasteCodeId": "%s", "quantity": %s,
                 "unit": "KG", "physicalState": "SOLID", "operation": "RECOVERED", "register": "ANEXA_1",
                 "physicalState": "SOLID", "storageType": "CT", "transportMeans": "AN", "packagingCategory": "SECONDARY", "wasteDestination": "Vr", "operationCode": "R13", "partnerId": "%s"}
                """.formatted(workPoint, date, code, kg, collector);
    }

    private void readEvidence(int year) throws Exception {
        mockMvc.perform(get("/api/v1/evidences?year=" + year).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private Map<String, byte[]> dossier(int year, int years) throws Exception {
        byte[] bytes = mockMvc.perform(get("/api/v1/audit-file?year=" + year + "&years=" + years)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) entries.put(e.getName(), in.readAllBytes());
        }
        return entries;
    }

    private List<Anexa1Sheet> sheets(int year, UUID workPoint) {
        TenantContext.set(tenantId);
        return evidenceCalculator.anexa1(year, workPoint);
    }

    private Anexa1Sheet onlySheet(int year, UUID workPoint) {
        List<Anexa1Sheet> sheets = sheets(year, workPoint);
        assertThat(sheets).as("fișele %s", year).hasSize(1);
        return sheets.get(0);
    }

    private AnnualDeclaration.Row onlyDeclarationRow(int year, UUID workPoint) {
        TenantContext.set(tenantId);
        List<AnnualDeclaration> declarations = evidenceCalculator.annualDeclaration(year, workPoint);
        assertThat(declarations).as("centralizata %s", year).hasSize(1);
        assertThat(declarations.get(0).rows()).hasSize(1);
        return declarations.get(0).rows().get(0);
    }

    private static void assertKg(BigDecimal actual, String expected) {
        assertThat(actual).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal(expected));
    }

    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    /** Un rând de dinainte de 19.09.2026: API-ul nu mai primește rubrica goală, deci o golim direct în bază. */
    private void legacyWithout(String column, String code) {
        jdbc.update("update waste_movements set " + column + " = null where work_point_id = ? "
                + "and waste_code_id = (select id from waste_codes where code = ?)", pointA, code);
    }
}
