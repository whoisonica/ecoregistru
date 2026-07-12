package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FAZA EVID / E2-generic: the evidence export endpoint. Exercises the full HTTP stack
 * (auth -> tenant scoping -> exporter) on Zonky. Covers content type + Content-Disposition,
 * that the produced file is non-empty and reopenable, that a viewer may export (read-only),
 * that an unknown format is a clean 400, and that exports are strictly tenant-scoped.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class EvidenceExportIT {

    private static final String XLSX_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    /** Header block above the data rows: company, title, subtitle, (skipped spacer), column headers. */
    private static final int FIRST_DATA_ROW = 5;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;

    private String adminToken;
    private String viewerToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtService.generateToken(appUserRepository.findByEmail("admin@demo.ro").orElseThrow());
        viewerToken = jwtService.generateToken(appUserRepository.findByEmail("viewer@demo.ro").orElseThrow());
    }

    @Test
    void exportsXlsxWithHeadersAndOneRowPerEvidenceLine() throws Exception {
        regenerate(adminToken, 2026);
        int lineCount = evidenceLineCount(adminToken, 2026);
        assertThat(lineCount).isGreaterThan(0); // the rich demo seed produces lines for 2026

        MockHttpServletResponse res = mockMvc.perform(get("/api/v1/evidences/export")
                        .param("year", "2026").param("format", "xlsx")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString(XLSX_TYPE)))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("evidenta-2026.xlsx")))
                .andReturn().getResponse();

        byte[] body = res.getContentAsByteArray();
        assertThat(body).isNotEmpty();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(dataRowCount(sheet)).isEqualTo(lineCount);
        }
    }

    @Test
    void exportsPdfWithCorrectHeaders() throws Exception {
        regenerate(adminToken, 2026);

        MockHttpServletResponse res = mockMvc.perform(get("/api/v1/evidences/export")
                        .param("year", "2026").param("format", "pdf")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/pdf")))
                .andExpect(header().string("Content-Disposition", containsString("evidenta-2026.pdf")))
                .andReturn().getResponse();

        byte[] body = res.getContentAsByteArray();
        assertThat(body).isNotEmpty();
        assertThat(new String(body, 0, 4)).isEqualTo("%PDF"); // valid PDF magic bytes
    }

    @Test
    void unknownFormatIsCleanBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/evidences/export")
                        .param("year", "2026").param("format", "csv")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("export.format.unsupported")));
    }

    @Test
    void viewerMayExportBecauseItIsReadOnly() throws Exception {
        regenerate(adminToken, 2026);
        mockMvc.perform(get("/api/v1/evidences/export")
                        .param("year", "2026").param("format", "xlsx")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString(XLSX_TYPE)));
    }

    @Test
    void exportIsTenantScoped() throws Exception {
        // A second tenant with a single movement in 2026. Its export must contain only its own
        // line, never the demo tenant's — proving scoping holds through the export path too.
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company other = companyRepository.save(Company.builder()
                .name("Other Tenant SRL").cui("ROX" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser otherUser = appUserRepository.save(AppUser.builder()
                .email("other+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(other).enabled(true).createdAt(Instant.now()).build());
        WorkPoint wp = workPointRepository.save(WorkPoint.builder()
                .company(other).name("PL Izolat").active(true).createdAt(Instant.now()).build());
        WasteCode code = wasteCodeRepository.findAll().get(0);
        movementRepository.save(WasteMovement.builder()
                .company(other).workPoint(wp).date(LocalDate.of(2026, 3, 10)).wasteCode(code)
                .quantity(new BigDecimal("100.000")).unit(Unit.KG).operation(WasteOperation.GENERATED)
                .deleted(false).createdBy(otherUser.getId()).build());

        String otherToken = jwtService.generateToken(otherUser);
        regenerate(adminToken, 2026);   // demo tenant: many lines
        regenerate(otherToken, 2026);   // other tenant: exactly one line

        MockHttpServletResponse res = mockMvc.perform(get("/api/v1/evidences/export")
                        .param("year", "2026").param("format", "xlsx")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        List<String> workPointNames = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(res.getContentAsByteArray()))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(dataRowCount(sheet)).isEqualTo(1);
            for (int r = FIRST_DATA_ROW; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row != null && row.getCell(0) != null) {
                    workPointNames.add(row.getCell(0).getStringCellValue());
                }
            }
        }
        assertThat(workPointNames).containsOnly("PL Izolat");
    }

    private void regenerate(String token, int year) throws Exception {
        mockMvc.perform(post("/api/v1/evidences/regenerate")
                        .param("year", String.valueOf(year))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    /** Number of evidence lines the demo tenant currently has for a year (via the list endpoint). */
    private int evidenceLineCount(String token, int year) throws Exception {
        String json = mockMvc.perform(get("/api/v1/evidences")
                        .param("year", String.valueOf(year))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).size();
    }

    /** Counts populated data rows below the header block. */
    private int dataRowCount(Sheet sheet) {
        int count = 0;
        for (int r = FIRST_DATA_ROW; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row != null && row.getCell(0) != null
                    && !row.getCell(0).getStringCellValue().isBlank()) {
                count++;
            }
        }
        return count;
    }
}
