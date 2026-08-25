package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.PackagingService;
import ro.ecoregistru.service.export.PackagingDeclaration;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The packaging module — <b>Anexa 1 Ambalaje</b> (Ordinul 794/2012, anexa nr. 1), built on
 * 25.08.2026 from the filled copy the specialist sent
 * ({@code documente oficiale/RAPORTARE AMBALAJE 2021_anexa 1_ HRR.xlsx}).
 *
 * <p>What these pin down:
 *
 * <ul>
 *   <li><b>Only 15 01 xx counts.</b> A shop cardboard recorded under 20 01 01 belongs on the
 *       waste-management record and nowhere near this form — the distinction the specialist drew
 *       on 24.08.2026, and the one that decides how large the client declared quantity is;</li>
 *   <li><b>one line per operator</b> in tabelul 2, which nota 1 asks for in writing;</li>
 *   <li><b>tabelul 1 is computed from the movements too</b> (since {@code V26}), split by the
 *       material and the kind of packaging the movement carries;</li>
 *   <li><b>each kilogram is counted once</b>: a company that records both the generation and the
 *       handover of one load declares it once, not twice;</li>
 *   <li>a code the European List does not settle (15 01 04 — aluminium and steel share it) is
 *       <em>reported as unclassified</em> rather than swept into "Altele", which the specialist
 *       says stays empty in practice;</li>
 *   <li>the download is the <b>.xls</b> the act asks for by name (art. 6), two sheets, in
 *       kilograms (art. 8 alin. (1) lit. a).</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PackagingDeclarationIT {

    private static final int YEAR = 2026;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PackagingService packagingService;
    @Autowired ro.ecoregistru.repository.PartnerWorkPointRepository workPointRepositoryForPartner;

    private String token;
    private UUID tenantId;
    private UUID workPointId;
    private Partner collector;
    private Partner recycler;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Ambalaje " + suffix).cui("ROA" + suffix)
                .type(ro.ecoregistru.enums.CompanyType.GENERATOR)
                .address("Cluj-Napoca, str. Exemplu nr. 1")
                .caenCode("4677")
                .contactName("Ion Popescu").contactRole("Manager Mediu")
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("amb+" + suffix + "@demo.ro").password("x")
                .role(ro.ecoregistru.enums.Role.ADMIN).company(company).enabled(true)
                .createdAt(Instant.now()).build());
        token = jwtService.generateToken(admin);
        workPointId = workPointRepository.save(ro.ecoregistru.entity.WorkPoint.builder()
                .company(company).name("Sediu").active(true).createdAt(Instant.now()).build()).getId();

        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector Ambalaje SRL").cui("RO111" + suffix.substring(0, 3))
                .type(PartnerType.COLLECTOR).client(true).active(true)
                .createdAt(Instant.now()).build());
        // Un singur punct de lucru: atunci el e cel scris pe formular, fără să aleagă nimeni.
        workPointRepositoryForPartner.save(ro.ecoregistru.entity.PartnerWorkPoint.builder()
                .partner(collector).name("P.L. Ilfov").address("Şos. de Centură 2-8")
                .active(true).createdAt(Instant.now()).build());

        recycler = partnerRepository.save(Partner.builder()
                .company(company).name("Reciclator Hârtie SA").cui("RO222" + suffix.substring(0, 3))
                .type(PartnerType.RECOVERER).client(true).active(true)
                .createdAt(Instant.now()).build());
    }

    /**
     * Two operators took cardboard packaging in the same year, so tabelul 2 has two lines — nota 1:
     * "câte o rubrică distinctă pentru fiecare dintre operatorii care au preluat".
     */
    @Test
    void oneLinePerOperatorAndOnlyPackagingCodes() throws Exception {
        handover("15 01 01", "300", collector.getId(), "R13", "SECONDARY", null);
        handover("15 01 01", "200", recycler.getId(), "R3", "SECONDARY", null);
        // Not packaging: a shop's cardboard under the municipal code stays out of this form.
        handover("20 01 01", "900", collector.getId(), "R3", null, null);

        List<PackagingDeclaration.HandoverRow> rows = handovers();

        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(r ->
                assertThat(r.material()).isEqualTo(PackagingMaterial.HARTIE_CARTON));
        assertThat(rows).extracting(PackagingDeclaration.HandoverRow::operatorName)
                .containsExactlyInAnyOrder("Colector Ambalaje SRL", "Reciclator Hârtie SA");
        assertThat(rows).extracting(PackagingDeclaration.HandoverRow::operation)
                .containsExactlyInAnyOrder("R13", "R3");
        // The recipient's work point, not its head office — that is what the form asks for.
        assertThat(rows).anySatisfy(r ->
                assertThat(r.operatorAddress()).contains("Şos. de Centură"));
    }

    /**
     * Tabelul 1 is summed from the movements: the material row comes from the code or the client's
     * choice, and the column from the kind of packaging.
     */
    @Test
    void theMarketTableIsSummedFromTheMovements() throws Exception {
        handover("15 01 01", "300", collector.getId(), "R13", "SECONDARY", null);
        handover("15 01 01", "200", recycler.getId(), "R3", "SECONDARY", null);
        handover("15 01 07", "80", collector.getId(), "R5", "PRIMARY", null);

        PackagingDeclaration d = declaration();

        assertThat(row(d, PackagingMaterial.HARTIE_CARTON)).satisfies(r -> {
            assertThat(r.secondaryTotal()).isEqualByComparingTo("500");
            assertThat(r.packagedGoodsTotal()).isEqualByComparingTo("500");
            // Nimeni n-a spus "ambalaj de desfacere", deci coloana 1 rămâne goală, nu zero.
            assertThat(r.salesPackaging()).isNull();
            assertThat(r.primaryTotal()).isNull();
        });
        assertThat(row(d, PackagingMaterial.STICLA).primaryTotal()).isEqualByComparingTo("80");
        // Un material fără nicio mişcare se tipăreşte gol de la un capăt la altul.
        assertThat(row(d, PackagingMaterial.LEMN).isEmpty()).isTrue();
    }

    /**
     * The reusable and hazardous ticks are sub-columns, not separate quantities: the same 200 kg
     * count in "Total" and in "din care: reutilizabil". Nota 3 says as much for the hazardous
     * column — "sunt tot ambalaje primare şi se regăsesc şi în coloana 3".
     */
    @Test
    void reusableAndHazardousAreSubColumnsOfTheSameKilograms() throws Exception {
        handover("15 01 07", "200", collector.getId(), "R5", "PRIMARY", "reusableAndHazardous");

        PackagingDeclaration.MarketRow r = row(declaration(), PackagingMaterial.STICLA);

        assertThat(r.primaryTotal()).isEqualByComparingTo("200");
        assertThat(r.primaryReusable()).isEqualByComparingTo("200");
        assertThat(r.hazardousContent()).isEqualByComparingTo("200");
    }

    /**
     * A company that records the generation <em>and</em> the handover of the same load has two
     * movements for one physical quantity. The declaration counts it once — the generations win,
     * and the exits stand in only where no generation was recorded, which is the same substitution
     * the evidence engine makes for implied generation (V24).
     */
    @Test
    void oneLoadRecordedTwiceIsDeclaredOnce() throws Exception {
        generation("15 01 01", "500", "SECONDARY");
        handover("15 01 01", "500", collector.getId(), "R3", "SECONDARY", null);

        PackagingDeclaration d = declaration();

        assertThat(row(d, PackagingMaterial.HARTIE_CARTON).secondaryTotal())
                .isEqualByComparingTo("500");
        // Tabelul 2 rămâne despre predare, deci acolo cifra apare o dată, din ieşire.
        assertThat(d.handoverRows()).singleElement()
                .satisfies(r -> assertThat(r.quantity()).isEqualByComparingTo("500"));
    }

    /**
     * 15 01 04 is "ambalaje metalice": aluminium cans and steel drums share it, and the form has a
     * row for each. Until the client says which, the quantity is reported as unclassified — it is
     * <b>not</b> parked in "Altele", which the specialist says stays empty in practice (25.08.2026).
     */
    @Test
    void metalPackagingIsReportedAsUnclassifiedRatherThanSweptIntoAltele() throws Exception {
        handover("15 01 04", "120", collector.getId(), "R4", "SECONDARY", null);

        PackagingDeclaration d = declaration();

        assertThat(row(d, PackagingMaterial.ALTELE).isEmpty()).isTrue();
        assertThat(d.handoverRows()).isEmpty();
        assertThat(d.unclassified()).singleElement().satisfies(r -> {
            assertThat(r.wasteCode()).isEqualTo("15 01 04");
            assertThat(r.missingMaterial()).isTrue();
            assertThat(r.missingCategory()).isFalse();
            assertThat(r.quantity()).isEqualByComparingTo("120");
        });
    }

    /** Once the client names the material on the movement, both tables place it. */
    @Test
    void choosingTheMaterialOnTheMovementPlacesItOnTheRightRow() throws Exception {
        handover("15 01 04", "120", collector.getId(), "R4", "SECONDARY", "OTEL");

        PackagingDeclaration d = declaration();

        assertThat(d.unclassified()).isEmpty();
        assertThat(row(d, PackagingMaterial.OTEL).secondaryTotal()).isEqualByComparingTo("120");
        assertThat(d.handoverRows()).singleElement()
                .satisfies(r -> assertThat(r.material()).isEqualTo(PackagingMaterial.OTEL));
    }

    /** A packaging movement with no kind of packaging named has no column, and says so. */
    @Test
    void aMovementWithoutTheKindOfPackagingIsReportedTooRatherThanCounted() throws Exception {
        handover("15 01 01", "300", collector.getId(), "R13", null, null);

        PackagingDeclaration d = declaration();

        assertThat(row(d, PackagingMaterial.HARTIE_CARTON).isEmpty()).isTrue();
        assertThat(d.unclassified()).singleElement().satisfies(r -> {
            assertThat(r.missingCategory()).isTrue();
            assertThat(r.missingMaterial()).isFalse();
            assertThat(r.material()).isEqualTo(PackagingMaterial.HARTIE_CARTON);
        });
        // Tabelul 2 nu depinde de felul ambalajului, doar de material — deci linia e acolo.
        assertThat(d.handoverRows()).hasSize(1);
    }

    /**
     * Tabelul 1 is legally about goods put on the market, not about waste, so a company whose
     * market figure differs from what the movements show may state it — and then the form prints
     * what they stated, for that material only.
     */
    @Test
    void aStoredFigureOverridesTheComputedRowForThatMaterialOnly() throws Exception {
        handover("15 01 01", "300", collector.getId(), "R13", "SECONDARY", null);
        handover("15 01 04", "120", collector.getId(), "R4", "SECONDARY", "OTEL");

        mockMvc.perform(put("/api/v1/packaging/market")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"material": "OTEL", "year": %d, "salesPackaging": 5192,
                                 "secondaryTotal": 5192}
                                """.formatted(YEAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesPackaging", is(5192)));

        PackagingDeclaration d = declaration();

        assertThat(row(d, PackagingMaterial.OTEL)).satisfies(r -> {
            assertThat(r.overridden()).isTrue();
            assertThat(r.salesPackaging()).isEqualByComparingTo("5192");
            assertThat(r.secondaryTotal()).isEqualByComparingTo("5192");
            assertThat(r.packagedGoodsTotal()).isEqualByComparingTo("5192");
        });
        // Celălalt material rămâne calculat din mişcări.
        assertThat(row(d, PackagingMaterial.HARTIE_CARTON)).satisfies(r -> {
            assertThat(r.overridden()).isFalse();
            assertThat(r.secondaryTotal()).isEqualByComparingTo("300");
        });
    }

    /** An all-empty override is a request to go back to the movements, not a row of zeroes. */
    @Test
    void clearingTheOverrideGivesTheComputedRowBack() throws Exception {
        handover("15 01 01", "300", collector.getId(), "R13", "SECONDARY", null);
        override("HARTIE_CARTON", "\"secondaryTotal\": 999");
        assertThat(row(declaration(), PackagingMaterial.HARTIE_CARTON).secondaryTotal())
                .isEqualByComparingTo("999");

        override("HARTIE_CARTON", null);

        assertThat(row(declaration(), PackagingMaterial.HARTIE_CARTON)).satisfies(r -> {
            assertThat(r.overridden()).isFalse();
            assertThat(r.secondaryTotal()).isEqualByComparingTo("300");
        });
    }

    /**
     * The tick, not the code, decides what reaches the declaration.
     *
     * <p>A shop throwing out the boxes its stock arrived in records {@code 15 01 01} like anyone
     * else — but its supplier put that packaging on the market, and this form reports what the
     * declarant introduced. So the quantity stays in the waste record and out of Anexa 1.
     */
    @Test
    void packagingSomebodyElsePutOnTheMarketStaysOutOfTheDeclaration() throws Exception {
        handover("15 01 01", "300", collector.getId(), "R13", "SECONDARY", null);
        notOnMarket("15 01 01", "900", collector.getId());

        PackagingDeclaration d = declaration();

        // Numai cele 300 kg bifate, nu 1200.
        assertThat(row(d, PackagingMaterial.HARTIE_CARTON).secondaryTotal())
                .isEqualByComparingTo("300");
        assertThat(d.handoverRows()).singleElement()
                .satisfies(r -> assertThat(r.quantity()).isEqualByComparingTo("300"));
        assertThat(d.unclassified()).isEmpty();
        // Dar rândul se vede în registrul tabului, marcat ca fiind în afara declaraţiei.
        mockMvc.perform(get("/api/v1/packaging/movements?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[?(@.quantity == 900)].countsForAnexa1Packaging",
                        is(List.of(false))));
    }

    /** The register the tab lists: every movement on a packaging code, and nothing else. */
    @Test
    void theTabListsThePackagingMovements() throws Exception {
        handover("15 01 01", "300", collector.getId(), "R13", "SECONDARY", null);
        handover("20 01 01", "900", collector.getId(), "R3", null, null);

        mockMvc.perform(get("/api/v1/packaging/movements?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].wasteCode", is("15 01 01")))
                .andExpect(jsonPath("$[0].packagingCode", is(true)))
                .andExpect(jsonPath("$[0].effectivePackagingMaterial", is("HARTIE_CARTON")))
                .andExpect(jsonPath("$[0].packagingCategory", is("SECONDARY")));
    }

    /**
     * Art. 6 of the order asks for the report "în format electronic «.xls»", so that is the
     * default download: two sheets named as the model names them, in kilograms.
     */
    @Test
    void theDeclarationDownloadsAsTheTwoSheetSpreadsheetTheActAsksFor() throws Exception {
        handover("15 01 01", "300", collector.getId(), "R13", "SECONDARY", null);

        byte[] xlsx = mockMvc.perform(get("/api/v1/packaging/anexa1?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheetName(0)).isEqualTo("Tabelul nr. 1");
            assertThat(wb.getSheetName(1)).isEqualTo("Tabelul nr. 2");
            assertThat(wb.getSheetAt(0).getRow(1).getCell(1).getStringCellValue())
                    .startsWith("ANEXA Nr. 1");
            assertThat(wb.getSheetAt(0).getRow(9).getCell(1).getStringCellValue())
                    .contains(String.valueOf(YEAR));
            assertThat(wb.getSheetAt(0).getRow(14).getCell(8).getStringCellValue())
                    .isEqualTo("[kilograme]");
            // Art. 6: „în format electronic «.xls» protejat împotriva modificării datelor".
            assertThat(wb.getSheetAt(0).getProtect()).isTrue();
            assertThat(wb.getSheetAt(1).getProtect()).isTrue();

            // Hârtie carton e al cincilea rând de material şi poartă cele 300 kg pe col. 5.
            assertThat(cell(wb, 0, 23, 1)).isEqualTo("Hârtie carton");
            assertThat(wb.getSheetAt(0).getRow(23).getCell(6).getNumericCellValue())
                    .isEqualTo(300d);
            // Sticla n-are mişcări, deci celula rămâne goală — nu 0.
            assertThat(wb.getSheetAt(0).getRow(19).getCell(6).getCellType())
                    .isEqualTo(org.apache.poi.ss.usermodel.CellType.BLANK);
            // Tabelul 2 numeşte operatorul şi operaţiunea.
            assertThat(sheetText(wb, 1)).contains("Colector Ambalaje SRL").contains("R13");
        }
    }

    /** The PDF stays available for the control file — same content, one page. */
    @Test
    void thePdfIsStillThereForTheControlFile() throws Exception {
        handover("15 01 01", "300", collector.getId(), "R13", "SECONDARY", null);

        byte[] pdf = mockMvc.perform(get("/api/v1/packaging/anexa1?year=" + YEAR + "&format=pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdf);
        String text = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1);
        int pages = reader.getNumberOfPages();
        reader.close();

        // Only what the extractor can see: OpenPDF's text extractor walks the page content but
        // not the cells of a PdfPTable, so the tables are checked on the model above and on a
        // rendered page by eye (docs/status.md), not by grepping bytes here.
        assertThat(text).contains("ANEXA Nr. 1");
        assertThat(text).contains("Tabel 1.");
        assertThat(text).contains("Se completeaz");   // nota 1 of tabelul 2
        assertThat(pages).isEqualTo(1);
    }

    // ---------- helpers ----------

    private PackagingDeclaration.MarketRow row(PackagingDeclaration d, PackagingMaterial material) {
        return d.marketRows().stream()
                .filter(r -> r.material() == material)
                .findFirst().orElseThrow();
    }

    private String cell(Workbook wb, int sheet, int row, int col) {
        return wb.getSheetAt(sheet).getRow(row).getCell(col).getStringCellValue();
    }

    private String sheetText(Workbook wb, int sheet) {
        StringBuilder sb = new StringBuilder();
        wb.getSheetAt(sheet).forEach(r -> r.forEach(c -> {
            if (c.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                sb.append(c.getStringCellValue()).append('\n');
            }
        }));
        return sb.toString();
    }

    private List<PackagingDeclaration.HandoverRow> handovers() {
        TenantContext.set(tenantId);
        try {
            return packagingService.handovers(YEAR);
        } finally {
            TenantContext.clear();
        }
    }

    private PackagingDeclaration declaration() {
        TenantContext.set(tenantId);
        try {
            return packagingService.declaration(YEAR);
        } finally {
            TenantContext.clear();
        }
    }

    private void override(String material, String figures) throws Exception {
        String body = figures == null
                ? """
                  {"material": "%s", "year": %d}
                  """.formatted(material, YEAR)
                : """
                  {"material": "%s", "year": %d, %s}
                  """.formatted(material, YEAR, figures);
        mockMvc.perform(put("/api/v1/packaging/market")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private void generation(String code, String quantity, String category) throws Exception {
        UUID codeId = wasteCodeRepository.findByCode(code).orElseThrow().getId();
        createMovement("""
                {
                  "workPointId": "%s", "date": "%d-05-10", "wasteCodeId": "%s",
                  "unit": "KG", "quantity": %s, "operation": "GENERATED",
                  "packagingOnMarket": true, "packagingCategory": "%s"
                }
                """.formatted(workPointId, YEAR, codeId, quantity, category));
    }

    private void handover(String code, String quantity, UUID partnerId, String operationCode,
                          String category, String materialOrFlags) throws Exception {
        UUID codeId = wasteCodeRepository.findByCode(code).orElseThrow().getId();
        StringBuilder extra = new StringBuilder();
        if (category != null) {
            extra.append(", \"packagingCategory\": \"").append(category).append('"');
        }
        if ("reusableAndHazardous".equals(materialOrFlags)) {
            extra.append(", \"packagingReusable\": true, \"packagingHazardousContent\": true");
        } else if (materialOrFlags != null) {
            extra.append(", \"packagingMaterial\": \"").append(materialOrFlags).append('"');
        }
        createMovement("""
                {
                  "workPointId": "%s", "date": "%d-05-12", "wasteCodeId": "%s",
                  "unit": "KG", "quantity": %s,
                  "operation": "RECOVERED", "operationCode": "%s", "partnerId": "%s",
                  "packagingOnMarket": true%s
                }
                """.formatted(workPointId, YEAR, codeId, quantity, operationCode, partnerId, extra));
    }

    /** O mişcare de ambalaj pe care firma nu l-a pus ea pe piaţă. */
    private void notOnMarket(String code, String quantity, UUID partnerId) throws Exception {
        UUID codeId = wasteCodeRepository.findByCode(code).orElseThrow().getId();
        createMovement("""
                {
                  "workPointId": "%s", "date": "%d-06-12", "wasteCodeId": "%s",
                  "unit": "KG", "quantity": %s,
                  "operation": "RECOVERED", "operationCode": "R3", "partnerId": "%s",
                  "packagingOnMarket": false
                }
                """.formatted(workPointId, YEAR, codeId, quantity, partnerId));
    }

    private void createMovement(String body) throws Exception {
        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @SuppressWarnings("unused")
    private static BigDecimal kg(String value) {
        return new BigDecimal(value);
    }
}
