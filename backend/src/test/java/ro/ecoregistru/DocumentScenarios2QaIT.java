package ro.ecoregistru;

import com.jayway.jsonpath.JsonPath;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import ro.ecoregistru.service.importer.ImportTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA după BUG-031…044 — a doua serie de scenarii deterministe: import și retragere, cântărire venită în
 * alt an, mutare combinată (dată + cod + punct peste an), cod periculos pe toate documentele, tone cu
 * volum. Oracolul e de mână, în javadoc.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DocumentScenarios2QaIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EvidenceCalculator evidenceCalculator;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired ImportTemplate template;

    private UUID tenantId;
    private String token;
    private String platformToken;
    private UUID pointA;
    private UUID pointB;
    private String pointAName;
    private String pointBName;
    private UUID collector;
    private String collectorCui;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Scenarii Doi " + suffix + " SRL").cui(TestCui.random()).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        tenantId = company.getId();
        token = jwtService.generateToken(user(company, Role.ADMIN));
        platformToken = jwtService.generateToken(user(null, Role.PLATFORM_ADMIN));
        pointAName = "PL Est " + suffix;
        pointBName = "PL Vest " + suffix;
        pointA = workPointRepository.save(WorkPoint.builder()
                .company(company).name(pointAName).active(true).createdAt(Instant.now()).build()).getId();
        pointB = workPointRepository.save(WorkPoint.builder()
                .company(company).name(pointBName).active(true).createdAt(Instant.now()).build()).getId();
        collectorCui = TestCui.random();
        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector Doi SRL").authorizationNumber("AM 12/2024").cui(collectorCui)
                .type(PartnerType.COLLECTOR).supplier(true).active(true)
                .createdAt(Instant.now()).build()).getId();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---------- import, apoi retragere ----------

    /**
     * De mână: importul aduce două predări pe PL Est, 20 01 01: 1200 kg D5 pe 10.03.2025 și 1,2 t R3 pe
     * 20.03.2025 → martie 2025: generat 2400, valorificat 1200, eliminat 1200. Fișa și centralizata se
     * citesc. Retragerea importului le șterge pe amândouă → pe 2025 nu mai rămâne nimic: nicio fișă, nicio
     * linie, niciun rând în centralizată, iar dosarul 2025 are fișa goală.
     */
    @Test
    void anImportThenItsRetractionLeaveEveryDocumentEmpty() throws Exception {
        byte[] xlsx = importFile(List.of(
                new Object[]{LocalDate.of(2025, 3, 10), pointAName, "20 01 01", "Eliminare", 1200, "kg",
                        "D5", "Deșeu propriu", collectorCui, "Fișa 3", "Solid", "CT", null, "AN", "DO", null},
                new Object[]{"20.03.2025", pointAName, "200101", "valorificare", "1,2", "tone",
                        "R3", "Deseu propriu", collectorCui, "Aviz 7", "Solid", "CT", null, "AN", "Vr", null}));
        mockMvc.perform(multipart("/api/v1/import")
                        .file(new MockMultipartFile("file", "import.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx))
                        .header("Authorization", "Bearer " + platformToken).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());

        Anexa1Sheet sheet = onlySheet(2025, pointA);
        assertKg(sheet.rows().get(2).generated(), "2400");
        assertKg(sheet.rows().get(2).recovered(), "1200");
        assertKg(sheet.rows().get(2).disposed(), "1200");
        assertKg(onlyDeclarationRow(2025, pointA).generated(), "2400");

        String history = mockMvc.perform(get("/api/v1/import/istoric").header("Authorization", "Bearer " + platformToken)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String batch = JsonPath.read(history, "$[0].id");
        mockMvc.perform(post("/api/v1/import/" + batch + "/anulare")
                        .header("Authorization", "Bearer " + platformToken).header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk());

        assertThat(sheets(2025, null)).isEmpty();
        TenantContext.set(tenantId);
        assertThat(evidenceCalculator.annualDeclaration(2025, null)).isEmpty();
        String json = mockMvc.perform(get("/api/v1/evidences?year=2025").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<Object>>read(json, "$")).isEmpty();
        Map<String, byte[]> zip = dossier(2025, 1);
        assertThat(Golden.pdfText(zip.get("rapoarte/evidenta-gestiunii-deseurilor-2025.pdf"))).doesNotContain("20 01 01");
    }

    // ---------- de cântărit în decembrie, cântărit în ianuarie ----------

    /**
     * De mână: 500 kg R13 pe 05.12.2025; o încărcătură de cântărit pe 20.12.2025. 2025 și 2026 citite, dosarul
     * pe doi ani descărcat (provizoriu: „(**)” pe centralizata 2025). În ianuarie vine tichetul: 0,75 t.
     * Decembrie 2025 devine: generat 1250, valorificat 1250; centralizata 2025 fără „(**)”; 2026 neatins.
     */
    @Test
    void aWeightThatArrivesNextYearLandsOnTheOldYearEverywhere() throws Exception {
        handover(pointA, code("20 01 01"), "2025-12-05", "500", "KG");
        String pending = unweighed(pointA, code("20 01 01"), "2025-12-20");
        readEvidence(2025);
        readEvidence(2026);
        String before = Golden.flat(Golden.pdfText(dossier(2026, 2).get("2025/rapoarte/evidenta-centralizata-2025.pdf")));
        assertThat(before).contains("(**)");

        mockMvc.perform(post("/api/v1/movements/" + pending + "/weight")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 0.75, \"unit\": \"TONS\"}"))
                .andExpect(status().isOk());
        readEvidence(2026);

        assertKg(onlySheet(2025, pointA).rows().get(11).generated(), "1250");
        AnnualDeclaration.Row row = onlyDeclarationRow(2025, pointA);
        assertKg(row.recovered(), "1250");
        assertThat(row.awaitingWeighing()).isFalse();
        Map<String, byte[]> zip = dossier(2026, 2);
        String central = Golden.flat(Golden.pdfText(zip.get("2025/rapoarte/evidenta-centralizata-2025.pdf")));
        assertThat(central).doesNotContain("(**)").contains(Golden.flat("20 01 01" + name("20 01 01")
                + "0.000" + "1250.000" + "1250.000" + "0.000" + "0.000"));
        assertThat(sheets(2026, pointA)).isEmpty();
    }

    // ---------- mutare combinată peste an, apoi ștergere ----------

    /**
     * De mână: PL Est 20 01 01: 100 kg pe 10.06.2025 și 300 kg pe 15.12.2025; PL Vest 20 01 02: 50 kg pe
     * 05.02.2026. Toate trei anii citiți. Corectura: cele 300 kg erau de fapt sticlă, la PL Vest, pe
     * 08.01.2026. Citit întâi 2025, apoi 2026:
     * 2025 — PL Est hârtie: 100 (iunie), nimic în decembrie; PL Vest: nimic.
     * 2026 — PL Vest sticlă: 300 în ianuarie, 50 în februarie → 350; PL Est: nimic.
     * Apoi mișcarea din iunie 2025 se șterge → 2025 fără nicio fișă. Dosarul pe trei ani spune la fel.
     */
    @Test
    void aMoveAcrossYearCodeAndWorkPointThenADeleteAgreeEverywhere() throws Exception {
        UUID paper = code("20 01 01");
        UUID glass = code("20 01 02");
        String june = handover(pointA, paper, "2025-06-10", "100", "KG");
        String december = handover(pointA, paper, "2025-12-15", "300", "KG");
        handover(pointB, glass, "2026-02-05", "50", "KG");
        readEvidence(2024);
        readEvidence(2025);
        readEvidence(2026);

        mockMvc.perform(put("/api/v1/movements/" + december)
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(handoverJson(pointB, glass, "2026-01-08", "300", "KG")))
                .andExpect(status().isOk());
        readEvidence(2025);
        readEvidence(2026);

        Anexa1Sheet a2025 = onlySheet(2025, pointA);
        assertKg(a2025.rows().get(5).recovered(), "100");
        assertKg(a2025.rows().get(11).recovered(), "0");
        assertThat(sheets(2025, pointB)).isEmpty();
        assertThat(sheets(2026, pointA)).isEmpty();
        Anexa1Sheet b2026 = onlySheet(2026, pointB);
        assertThat(b2026.wasteCode()).isEqualTo("20 01 02");
        assertKg(b2026.rows().get(0).recovered(), "300");
        assertKg(onlyDeclarationRow(2026, pointB).recovered(), "350");

        mockMvc.perform(delete("/api/v1/movements/" + june).header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful());
        readEvidence(2025);
        assertThat(sheets(2025, null)).isEmpty();

        Map<String, byte[]> zip = dossier(2026, 3);
        assertThat(Golden.pdfText(zip.get("2025/rapoarte/evidenta-gestiunii-deseurilor-2025.pdf"))).doesNotContain("20 01 01");
        String c2026 = Golden.flat(Golden.pdfText(zip.get("2026/rapoarte/evidenta-centralizata-2026.pdf")));
        assertThat(c2026).contains(Golden.flat("20 01 02" + name("20 01 02") + "0.000" + "350.000" + "350.000" + "0.000" + "0.000"));
        assertThat(c2026).doesNotContain("20 01 01");
    }

    // ---------- periculos, tone, volum ----------

    /**
     * 13 02 08 (periculos): 2,5 t R9 cu 3,2 m³, pe 10.04.2026. Asteriscul pe fișă, pe centralizată, pe aviz,
     * în lista autorizațiilor din dosar. Cantitatea: 2500 kg pe fișă și centralizată, 2,500 t pe aviz.
     */
    @Test
    void aHazardousLoadInTonnesIsSpelledTheSameOnEveryDocument() throws Exception {
        UUID oil = code("13 02 08");
        String id = JsonPath.read(mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workPointId": "%s", "date": "2026-04-10", "wasteCodeId": "%s", "quantity": 2.5,
                                 "unit": "TONS", "volumeM3": 3.2, "physicalState": "LIQUID", "operation": "RECOVERED",
                                 "register": "ANEXA_1", "physicalState": "SOLID", "storageType": "CT", "transportMeans": "AN", "packagingCategory": "SECONDARY", "wasteDestination": "Vr", "operationCode": "R9", "partnerId": "%s"}
                                """.formatted(pointA, oil, collector)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.id");

        String sheet = pdf("/api/v1/evidences/anexa1?year=2026&workPointId=" + pointA);
        assertThat(sheet).contains("13 02 08*").contains("2500.000");
        String central = Golden.flat(pdf("/api/v1/evidences/declaratie-anuala?year=2026&workPointId=" + pointA));
        assertThat(central).contains(Golden.flat("13 02 08*" + name("13 02 08") + "0.000" + "2500.000" + "2500.000"));
        String aviz = pdf("/api/v1/movements/" + id + "/aviz");
        assertThat(aviz).contains("13 02 08*").contains("2,500");
        Map<String, byte[]> zip = dossier(2026, 1);
        assertThat(Golden.pdfText(zip.get("autorizatii-parteneri.pdf"))).contains("13 02 08*");
    }

    // ---------- helpers ----------

    private AppUser user(Company company, Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + UUID.randomUUID().toString().substring(0, 8) + "@scen2.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private UUID code(String code) {
        return wasteCodeRepository.findByCode(code).orElseThrow().getId();
    }

    private String name(String code) {
        return wasteCodeRepository.findByCode(code).orElseThrow().getName();
    }

    private String handover(UUID workPoint, UUID code, String date, String qty, String unit) throws Exception {
        String json = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content(handoverJson(workPoint, code, date, qty, unit)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String unweighed(UUID workPoint, UUID code, String date) throws Exception {
        String json = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workPointId": "%s", "date": "%s", "wasteCodeId": "%s",
                                 "unit": "KG", "physicalState": "SOLID", "weighedAtUnloading": true,
                                 "operation": "RECOVERED", "register": "ANEXA_1", "physicalState": "SOLID", "storageType": "CT", "transportMeans": "AN", "packagingCategory": "SECONDARY", "wasteDestination": "Vr",
                                 "operationCode": "R13", "partnerId": "%s"}
                                """.formatted(workPoint, date, code, collector)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String handoverJson(UUID workPoint, UUID code, String date, String qty, String unit) {
        return """
                {"workPointId": "%s", "date": "%s", "wasteCodeId": "%s", "quantity": %s,
                 "unit": "%s", "physicalState": "SOLID", "operation": "RECOVERED", "register": "ANEXA_1",
                 "physicalState": "SOLID", "storageType": "CT", "transportMeans": "AN", "packagingCategory": "SECONDARY", "wasteDestination": "Vr", "operationCode": "R13", "partnerId": "%s"}
                """.formatted(workPoint, date, code, qty, unit, collector);
    }

    private void readEvidence(int year) throws Exception {
        mockMvc.perform(get("/api/v1/evidences?year=" + year).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String pdf(String url) throws Exception {
        return Golden.pdfText(mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
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

    private byte[] importFile(List<Object[]> movements) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(template.render()));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var date = wb.createCellStyle();
            date.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy"));
            Sheet sheet = wb.getSheet("Mișcări");
            for (int i = 0; i < movements.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Object[] values = movements.get(i);
                for (int j = 0; j < values.length; j++) {
                    if (values[j] == null) continue;
                    Cell cell = row.createCell(j);
                    if (values[j] instanceof LocalDate d) {
                        cell.setCellValue(d);
                        cell.setCellStyle(date);
                    } else if (values[j] instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(values[j].toString());
                    }
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
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

    @SuppressWarnings("unused")
    private static String fold(String text) {
        return Normalizer.normalize(text.replace('ª', 'a'), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
