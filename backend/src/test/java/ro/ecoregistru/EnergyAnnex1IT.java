package ro.ecoregistru;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Anexa 1 (consum sub 1000 tep) as the .xlsx, read cell by cell off the downloaded file. Invented figures only. */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class EnergyAnnex1IT {

    private static final int YEAR = 2025;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired ObjectMapper objectMapper;

    private String admin;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Exemplu Energie " + suffix).cui("RO" + suffix)
                .type(CompanyType.GENERATOR)
                .address("Cluj-Napoca, str. Exemplu nr. 1")
                .caenCode("3811")
                .contactPhone("0264 000 000").contactEmail("office@exemplu.ro")
                .contactName("Ion Popescu").contactRole("Manager Mediu")
                .active(true).createdAt(Instant.now()).build());
        admin = jwtService.generateToken(appUserRepository.save(AppUser.builder()
                .email("adm+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true)
                .createdAt(Instant.now()).build()));
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    void sheetsAndTitlesAreThoseOfTheAnnex() throws Exception {
        try (XSSFWorkbook wb = annex()) {
            assertThat(List.of(wb.getSheetName(0), wb.getSheetName(1), wb.getSheetName(2)))
                    .containsExactly("Date generale", "Date statistice", "Incadrare OE");
            assertThat(wb.getNumberOfSheets()).isEqualTo(3);
            assertThat(text(wb, "Date generale", "A1")).isEqualTo("CAP. I – DATE DE CONTACT ALE CONSUMATORULUI DE "
                    + "ENERGIE. CHESTIONAR DE ANALIZĂ ENERGETICĂ pentru anul 2025\npentru operatori economici cu "
                    + "consum anual mai mic de 1000 tep");
            assertThat(text(wb, "Date statistice", "A1"))
                    .isEqualTo("CAP. II – DATE STATISTICE DE CONSUM DE ENERGIE LA NIVELUL ANULUI DE RAPORTARE")
                    .doesNotContain("ANTERIOR");
            assertThat(cell(wb, "Date statistice", "G1").getNumericCellValue()).isEqualTo(2025.0);
            assertThat(text(wb, "Incadrare OE", "A1")).isEqualTo("CAP III. Încadrarea operatorului economic în "
                    + "categoria întreprinderilor mici și mijlocii (IMM)*");
            for (int i = 0; i < 3; i++) {
                assertThat(wb.getSheetAt(i).getProtect()).as("sheet %d protected", i).isFalse();
            }
        }
        mockMvc.perform(get("/api/v1/energy/anexa1?year=" + YEAR).header("Authorization", "Bearer " + admin))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"anexa1-energie-2025.xlsx\""))
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void coefficientsArePrintedAsText() throws Exception {
        try (XSSFWorkbook wb = annex()) {
            Sheet sh = wb.getSheet("Date statistice");
            assertThat(cell(wb, "Date statistice", "A14").getCellType()).isEqualTo(CellType.STRING);
            assertThat(Golden.cells(sh, 13, 0, 6))
                    .containsExactly("(0,086)", "(0,95)", "(0,97)", "(1,05)", "(1,015)", "(fcţ. de tip)", "(fcţ. de tip)");
        }
    }

    @Test
    void dieselFormulaMultipliesTheQuantity() throws Exception {
        tick("DIESEL");
        twelve("DIESEL", "1", null);
        try (XSSFWorkbook wb = annex()) {
            Cell e18 = cell(wb, "Date statistice", "E18");
            assertThat(e18.getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(e18.getCellFormula()).isEqualTo("E16*1.015").isNotEqualTo("0*1.015");
            assertThat(cell(wb, "Date statistice", "E16").getNumericCellValue()).isEqualTo(12.0);
            assertThat(evaluate(wb, e18)).isCloseTo(12.18, within(1e-9));
        }
    }

    @Test
    void unusedIsZeroAndIncompleteIsBlank() throws Exception {
        tick("ELECTRICITY");
        for (int m = 1; m <= 11; m++) {
            cell("ELECTRICITY", m, "10", null);
        }
        try (XSSFWorkbook wb = annex()) {
            Cell gas = cell(wb, "Date statistice", "A16");
            assertThat(gas.getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(gas.getNumericCellValue()).isEqualTo(0.0);
            assertThat(gas.getCellStyle().getDataFormatString()).isEqualTo("0.000");
            assertThat(((org.apache.poi.xssf.usermodel.XSSFCellStyle) gas.getCellStyle())
                    .getFillForegroundXSSFColor().getARGBHex()).isEqualTo("FFFFFF00");
            assertThat(((org.apache.poi.xssf.usermodel.XSSFCellStyle) gas.getCellStyle())
                    .getFont().getXSSFColor().getARGBHex()).isEqualTo("FFFF0000");
            assertThat(cell(wb, "Date statistice", "G6").getCellType()).isEqualTo(CellType.BLANK);
            assertThat(Golden.cells(wb.getSheet("Date statistice"), 15, 0, 6))
                    .containsExactly(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
            // Coal and other fuels (tep), heat, the two renewables: unused, so 0 — never blank.
            for (String ref : List.of("F18", "G18", "G9", "G20", "G23")) {
                Cell c = cell(wb, "Date statistice", ref);
                assertThat(c.getCellType()).as(ref).isEqualTo(CellType.NUMERIC);
                assertThat(c.getNumericCellValue()).as(ref).isEqualTo(0.0);
            }
        }
    }

    @Test
    void coalWithAMonthWithoutTepLeavesF18Blank() throws Exception {
        tick("COAL");
        for (int m = 1; m <= 12; m++) {
            cell("COAL", m, "2", m == 6 ? null : "1.5");
        }
        try (XSSFWorkbook wb = annex()) {
            // June has a quantity but no tep: the year's coal is unknown, so neither 0 nor a partial sum.
            assertThat(cell(wb, "Date statistice", "F18").getCellType()).isEqualTo(CellType.BLANK);
            assertThat(cell(wb, "Date statistice", "F16").getCellType()).isEqualTo(CellType.BLANK);
        }
        cell("COAL", 6, "2", "1.5");
        try (XSSFWorkbook wb = annex()) {
            assertThat(cell(wb, "Date statistice", "F18").getNumericCellValue()).isEqualTo(18.0);
        }
    }

    @Test
    void coalTepIsTheHandWrittenSum() throws Exception {
        tick("COAL", "OTHER_FUEL");
        twelve("COAL", "2", "1.5");
        for (int m = 1; m <= 12; m++) {
            cell("OTHER_FUEL", m, "1", m == 12 ? null : "0.25");
        }
        try (XSSFWorkbook wb = annex()) {
            assertThat(cell(wb, "Date statistice", "F16").getNumericCellValue()).isEqualTo(24.0);
            Cell f18 = cell(wb, "Date statistice", "F18");
            assertThat(f18.getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(f18.getNumericCellValue()).isEqualTo(18.0);
            // Other fuels with one month's tep missing: neither the quantity nor the tep is printed, not even 0
            assertThat(cell(wb, "Date statistice", "G16").getCellType()).isEqualTo(CellType.BLANK);
            assertThat(cell(wb, "Date statistice", "G18").getCellType()).isEqualTo(CellType.BLANK);
        }
    }

    @Test
    void totalFormulaEvaluatesToTheSheetTotal() throws Exception {
        tick("ELECTRICITY", "HEAT", "NATURAL_GAS", "PETROL", "DIESEL", "COAL",
                "RENEWABLE_ELECTRICITY", "RENEWABLE_HEAT");
        twelve("ELECTRICITY", "12.5", null);
        twelve("HEAT", "3.5", null);
        twelve("NATURAL_GAS", "40.125", null);
        twelve("PETROL", "0.2", null);
        twelve("DIESEL", "1.25", null);
        twelve("COAL", "0.5", "0.3333");
        twelve("RENEWABLE_ELECTRICITY", "2", null);
        twelve("RENEWABLE_HEAT", "1", null);
        double totalTep = objectMapper.readTree(mockMvc.perform(get("/api/v1/energy?year=" + YEAR)
                        .header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsByteArray()).get("totalTep").asDouble();
        // 12.9 + 4.2 + 41.409 + 2.52 + 15.225 + 3.9996 + 2.064 + 1.2: every tep exact at four decimals
        assertThat(totalTep).isCloseTo(83.5176, within(1e-9));
        try (XSSFWorkbook wb = annex()) {
            Cell g3 = cell(wb, "Date statistice", "G3");
            assertThat(g3.getCellFormula()).isEqualTo("G5+G8+G11+G21+G24");
            assertThat(g3.getCellStyle().getDataFormatString()).isEqualTo("0.000");
            assertThat(evaluate(wb, g3)).isCloseTo(totalTep, within(1e-6));
            assertThat(cell(wb, "Date statistice", "H3").getCellFormula()).isEqualTo("H5+H8+H11+H20+H23");
            assertThat(cell(wb, "Date statistice", "G11").getCellFormula()).isEqualTo("A18+B18+C18+D18+E18+F18+G18");
            assertThat(cell(wb, "Incadrare OE", "E33").getCellFormula()).isEqualTo("E31/'Date statistice'!G3");
        }
    }

    @Test
    void smeYesPrintsDaAndDashes() throws Exception {
        declaration("{\"year\":2025,\"sme\":true}");
        try (XSSFWorkbook wb = annex()) {
            assertThat(text(wb, "Incadrare OE", "G2")).isEqualTo("DA");
            assertThat(text(wb, "Incadrare OE", "G3")).isEqualTo("-");
            for (String ref : List.of("C4", "C5", "C6", "C7")) {
                assertThat(text(wb, "Incadrare OE", ref)).as(ref).isEqualTo("-");
            }
        }
    }

    @Test
    void smeNoPrintsNuTheAuditAndTheMeasures() throws Exception {
        declaration("{\"year\":2025,\"sme\":false,\"auditDate\":\"2024-03-15\",\"auditor\":\"Auditor Exemplu SRL\","
                + "\"auditScope\":\"Hala de producție\",\"auditSharePct\":62.5,"
                + "\"measures\":[{\"name\":\"Iluminat LED\",\"costEstimated\":10,\"costActual\":9.5,"
                + "\"savingsTepEstimated\":1.2,\"savingsCostEstimated\":4}]}");
        try (XSSFWorkbook wb = annex()) {
            Sheet sh = wb.getSheet("Incadrare OE");
            assertThat(text(wb, "Incadrare OE", "G2")).isEqualTo("-");
            assertThat(text(wb, "Incadrare OE", "G3")).isEqualTo("NU");
            assertThat(text(wb, "Incadrare OE", "C4")).isEqualTo("15.03.2024");
            assertThat(text(wb, "Incadrare OE", "C5")).isEqualTo("Auditor Exemplu SRL");
            assertThat(text(wb, "Incadrare OE", "C6")).isEqualTo("Hala de producție");
            Cell c7 = cell(wb, "Incadrare OE", "C7");
            assertThat(c7.getNumericCellValue()).isEqualTo(62.5);
            assertThat(c7.getCellStyle().getDataFormatString()).isEqualTo("0.00\"%\"");
            assertThat(Golden.cells(sh, 10, 0, 8))
                    .containsExactly(1.0, "Iluminat LED", 10.0, 9.5, 1.2, "", "", 4.0, "");
            assertThat(Golden.cells(sh, 11, 0, 8)).containsExactly(2.0, "", "", "", "", "", "", "", "");
            assertThat(cell(wb, "Incadrare OE", "A30").getNumericCellValue()).isEqualTo(20.0);
            assertThat(cell(wb, "Incadrare OE", "C31").getCellFormula()).isEqualTo("SUM(C11:C30)");
            assertThat(cell(wb, "Incadrare OE", "C32").getCellFormula()).isEqualTo("1000*C31/E31/11.63");
        }
    }

    @Test
    void smeUnknownLeavesBlanks() throws Exception {
        try (XSSFWorkbook wb = annex()) {
            for (String ref : List.of("G2", "G3", "C4", "C5", "C6", "C7", "B11", "C11", "I11")) {
                assertThat(cell(wb, "Incadrare OE", ref).getCellType()).as(ref).isEqualTo(CellType.BLANK);
            }
        }
    }

    @Test
    void poimRowsArePrintedAndBlankUntilAnswered() throws Exception {
        try (XSSFWorkbook wb = annex()) {
            assertThat(text(wb, "Incadrare OE", "A34")).isEqualTo("Exista interes pentru programul de finantare "
                    + "nerambursabila pentru sisteme de cogenerare de inalta eficienta prin POIM 6.4?");
            assertThat(text(wb, "Incadrare OE", "A35")).isEqualTo("Daca da, s-a depus sau se intentioneaza "
                    + "depunerea unui proiect de accesare finantare nerambursabila pana la 80%?");
            assertThat(cell(wb, "Incadrare OE", "I34").getCellType()).isEqualTo(CellType.BLANK);
            assertThat(cell(wb, "Incadrare OE", "I35").getCellType()).isEqualTo(CellType.BLANK);
            assertThat(text(wb, "Incadrare OE", "A37")).startsWith("Notă:\t*  Se evidențiază");
            assertThat(wb.getSheet("Incadrare OE").getMergedRegions().stream().map(r -> r.formatAsString()))
                    .contains("A34:H34", "A35:H35", "A37:I44")
                    .doesNotContain("A35:I42");
        }
        declaration("{\"year\":2025,\"poimInterest\":true,\"poimProject\":false}");
        try (XSSFWorkbook wb = annex()) {
            assertThat(text(wb, "Incadrare OE", "I34")).isEqualTo("Da");
            assertThat(text(wb, "Incadrare OE", "I35")).isEqualTo("Nu");
        }
    }

    @Test
    void attestationRowIsSingleWithDash() throws Exception {
        contact("{\"fax\":\"0264 000 001\",\"website\":\"exemplu.ro\",\"activitySector\":\"Comerț cu ridicata\","
                + "\"name\":\"Maria Ionescu\",\"email\":\"maria@exemplu.ro\",\"phone\":\"0264 000 002\","
                + "\"mobile\":\"0722 000 000\"}");
        try (XSSFWorkbook wb = annex()) {
            Sheet sh = wb.getSheet("Date generale");
            assertThat(text(wb, "Date generale", "A9")).isEqualTo("Manager energetic sau Persoana de contact ");
            assertThat(text(wb, "Date generale", "G9")).isEqualTo("deține Atestat * eliberat de ANRE la data de:");
            assertThat(text(wb, "Date generale", "H9")).isEqualTo("-");
            assertThat(sh.getLastRowNum()).isEqualTo(10);
            assertThat((Object) sh.getRow(11)).isNull();
            assertThat(Golden.cells(sh, 9, 0, 6)).containsExactly(
                    "NUME, PRENUME", "Maria Ionescu", "", "", "", "E-mail", "maria@exemplu.ro");
            assertThat(Golden.cells(sh, 10, 0, 6)).containsExactly(
                    "Telefon fix", "0264 000 002", "", "", "", "Tel. mobil", "0722 000 000");
            assertThat(text(wb, "Date generale", "C4")).startsWith("RO");
            assertThat(text(wb, "Date generale", "C3")).isEqualTo("Cluj-Napoca, str. Exemplu nr. 1");
            assertThat(Golden.cells(sh, 4, 0, 4)).containsExactly("Telefon", "0264 000 000", "", "Pag. Internet",
                    "exemplu.ro");
            assertThat(Golden.cells(sh, 5, 0, 4)).containsExactly("Fax", "0264 000 001", "", "E-mail",
                    "office@exemplu.ro");
            assertThat(Golden.cells(sh, 6, 0, 2)).containsExactly("Profil de activitate", "Cod CAEN             ",
                    "3811");
            assertThat(text(wb, "Date generale", "D8")).isEqualTo("Comerț cu ridicata");
            assertThat(sh.getMergedRegions().stream().map(r -> r.formatAsString()))
                    .contains("A9:F9", "B10:E10", "G10:H10", "B11:E11", "G11:H11")
                    .doesNotContain("A9:F10", "B12:E12");
        }
        contact("{\"name\":\"Maria Ionescu\",\"attestedOn\":\"2026-10-01\"}");
        try (XSSFWorkbook wb = annex()) {
            assertThat(text(wb, "Date generale", "H9")).isEqualTo("01.10.2026");
            assertThat(cell(wb, "Date generale", "E5").getCellType()).isEqualTo(CellType.BLANK);
        }
    }

    @Test
    void overThresholdIs422() throws Exception {
        tick("COAL");
        for (int m = 1; m <= 12; m++) {
            cell("COAL", m, "1", m == 12 ? "120" : "80");
        }
        mockMvc.perform(get("/api/v1/energy/anexa1?year=" + YEAR).header("Authorization", "Bearer " + admin))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("energy.over.threshold")));
        cell("COAL", 12, "1", "119.999");
        try (XSSFWorkbook wb = annex()) {
            assertThat(cell(wb, "Date statistice", "F18").getNumericCellValue()).isCloseTo(999.999, within(1e-9));
        }
    }

    @Test
    void overThresholdWithMonthsMissingIs422() throws Exception {
        // Eleven months of other fuels at 100 tep, December not entered: already 1100 known, so over the line.
        tick("OTHER_FUEL");
        for (int m = 1; m <= 11; m++) {
            cell("OTHER_FUEL", m, "1", "100");
        }
        mockMvc.perform(get("/api/v1/energy?year=" + YEAR).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.monthsComplete", is(11)))
                .andExpect(jsonPath("$.totalTep", is(0)))
                .andExpect(jsonPath("$.knownTep", is(1100.0)))
                .andExpect(jsonPath("$.overThreshold", is(true)));
        mockMvc.perform(get("/api/v1/energy/anexa1?year=" + YEAR).header("Authorization", "Bearer " + admin))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("energy.over.threshold")));
    }

    // ---------------------------------------------------------------------------------------------

    private XSSFWorkbook annex() throws Exception {
        byte[] body = mockMvc.perform(get("/api/v1/energy/anexa1?year=" + YEAR)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return new XSSFWorkbook(new ByteArrayInputStream(body));
    }

    private static Cell cell(XSSFWorkbook wb, String sheet, String ref) {
        CellReference r = new CellReference(ref);
        org.apache.poi.ss.usermodel.Row row = wb.getSheet(sheet).getRow(r.getRow());
        assertThat((Object) row).as(sheet + "!" + ref + " row").isNotNull();
        Cell c = row.getCell(r.getCol());
        assertThat(c).as(sheet + "!" + ref).isNotNull();
        return c;
    }

    private static String text(XSSFWorkbook wb, String sheet, String ref) {
        Cell c = cell(wb, sheet, ref);
        assertThat(c.getCellType()).as(sheet + "!" + ref).isEqualTo(CellType.STRING);
        return c.getStringCellValue();
    }

    private static double evaluate(XSSFWorkbook wb, Cell c) {
        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
        return evaluator.evaluate(c).getNumberValue();
    }

    private ResultActions putTo(String path, String body) throws Exception {
        return mockMvc.perform(put("/api/v1/energy" + path)
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void tick(String... carriers) throws Exception {
        putTo("/carriers", "{\"year\":%d,\"carriers\":[\"%s\"]}".formatted(YEAR, String.join("\",\"", carriers)))
                .andExpect(status().isOk());
    }

    private void cell(String carrier, int month, String quantity, String tep) throws Exception {
        putTo("/consumption", "{\"year\":%d,\"carrier\":\"%s\",\"month\":%d,\"quantity\":%s,\"tep\":%s}"
                .formatted(YEAR, carrier, month, quantity, tep)).andExpect(status().isOk());
    }

    private void twelve(String carrier, String quantity, String tep) throws Exception {
        for (int m = 1; m <= 12; m++) {
            cell(carrier, m, quantity, tep);
        }
    }

    private void declaration(String body) throws Exception {
        putTo("/declaration", body).andExpect(status().isOk());
    }

    private void contact(String body) throws Exception {
        putTo("/contact", body).andExpect(status().isOk());
    }
}
