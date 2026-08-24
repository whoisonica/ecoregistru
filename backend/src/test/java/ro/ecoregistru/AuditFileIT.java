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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FAZA DOSAR / D1: the control-dossier ZIP over the real HTTP stack. Verifies the archive is a
 * valid, non-empty ZIP with the expected entries, that a read-only viewer may download it, and
 * that it is tenant-scoped (a fresh tenant's dossier never contains another tenant's evidence).
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class AuditFileIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
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
    void downloadsZipWithExpectedEntries() throws Exception {
        MockHttpServletResponse res = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/zip")))
                .andExpect(header().string("Content-Disposition", containsString("dosar-control-2026.zip")))
                .andReturn().getResponse();

        List<String> entries = zipEntryNames(res.getContentAsByteArray());
        assertThat(entries).contains(
                "README.txt",
                // The regulated document: four chapters per waste code, asked for by the
                // specialist on 23.08.2026 ("când dă print la dosar control să respecte
                // structura de 4 tabele pe care o am de la Andreea").
                "evidenta-gestiunii-deseurilor-2026.pdf",
                "evidenta-2026.xlsx",
                "evidenta-2026.pdf",
                "autorizatii-parteneri.pdf",
                "atasamente/index.txt");
    }

    @Test
    void theAnexa1SheetInTheDossierIsARealPdf() throws Exception {
        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        byte[] pdf = readEntryBytes(zip, "evidenta-gestiunii-deseurilor-2026.pdf");
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void theReadmeNamesTheSheetAndItsDeadline() throws Exception {
        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String readme = new String(readEntryBytes(zip, "README.txt"), StandardCharsets.UTF_8);
        assertThat(readme)
                .contains("Evidența gestiunii deșeurilor generate 2026")
                .contains("HG 856/2002, anexa 1")
                // 15 March of the FOLLOWING year — the term of OUG 92/2021 art. 48 alin. (1).
                .contains("Termen de depunere: 15 martie 2027");
    }

    // --- Etapa 6: the dossier sized to the retention period ---

    /**
     * OUG 92/2021 art. 48 alin. (5) keeps the evidence "cel putin 3 ani", so a dossier may reach
     * three years back. Several years cannot share the flat layout - the file names repeat - so
     * each year gets its own folder, and the archive is named after the range.
     */
    @Test
    void threeYearsAreFoldered_oneYearStaysFlat() throws Exception {
        MockHttpServletResponse res = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .param("years", "3")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("dosar-control-2024-2026.zip")))
                .andReturn().getResponse();

        List<String> entries = zipEntryNames(res.getContentAsByteArray());
        assertThat(entries).contains(
                "README.txt",
                "2024/evidenta-gestiunii-deseurilor-2024.pdf",
                "2025/evidenta-gestiunii-deseurilor-2025.pdf",
                "2026/evidenta-gestiunii-deseurilor-2026.pdf",
                "2026/declaratie-anuala-2026.pdf",
                "2026/evidenta-2026.xlsx",
                "2026/atasamente/index.txt",
                // One snapshot for the whole dossier: the status is read against today, not
                // against a reporting year.
                "autorizatii-parteneri.pdf");
        assertThat(entries).doesNotContain("evidenta-gestiunii-deseurilor-2026.pdf");
    }

    @Test
    void theReadmeOfAMultiYearDossierNamesTheRetentionRule() throws Exception {
        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .param("years", "3")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String readme = new String(readEntryBytes(zip, "README.txt"), StandardCharsets.UTF_8);
        assertThat(readme)
                .contains("Anii de raportare: 2024\u20132026")
                .contains("OUG 92/2021, art. 48")
                .contains("Termen de depunere: 15 martie 2027");
    }

    /**
     * A year nobody regenerated prints blank sheets. The dossier says so rather than handing over
     * an empty official form that looks like lost data - the demo tenant has no 2024 evidence.
     */
    @Test
    void aYearWithoutEvidenceLinesIsCalledOutInTheReadme() throws Exception {
        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .param("years", "3")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String readme = new String(readEntryBytes(zip, "README.txt"), StandardCharsets.UTF_8);
        assertThat(readme).contains("nu exist\u0103 nicio linie de eviden\u021b\u0103 calculat\u0103");
    }

    /**
     * Five years, not three. The law's floor is three (OUG 92/2021, art. 48 alin. (5)); the
     * specialist asked for the margin on 24.08.2026 — "sunt 3 în lege dar de safety". A year the
     * application never kept comes out empty and named as such in README.txt, so the margin
     * cannot quietly ship a blank official sheet.
     */
    @Test
    void fiveYearsIsAllowedAndSixIsRefused() throws Exception {
        mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .param("years", "5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("dosar-control-2022-2026.zip")));

        mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .param("years", "6")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void viewerMayDownloadBecauseItIsReadOnly() throws Exception {
        mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/zip")));
    }

    @Test
    void dossierIsTenantScoped() throws Exception {
        // Fresh tenant with a single 2026 movement whose work point name is unique.
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company other = companyRepository.save(Company.builder()
                .name("Izolat SRL").cui("ROZ" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        AppUser otherUser = appUserRepository.save(AppUser.builder()
                .email("izolat+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(other).enabled(true).createdAt(Instant.now()).build());
        WorkPoint wp = workPointRepository.save(WorkPoint.builder()
                .company(other).name("PL-UNIC-" + suffix).active(true).createdAt(Instant.now()).build());
        WasteCode code = wasteCodeRepository.findAll().get(0);
        movementRepository.save(WasteMovement.builder()
                .company(other).workPoint(wp).date(LocalDate.of(2026, 3, 10)).wasteCode(code)
                .quantity(new BigDecimal("100.000")).unit(Unit.KG).operation(WasteOperation.GENERATED)
                .deleted(false).createdBy(otherUser.getId()).build());

        String otherToken = jwtService.generateToken(otherUser);
        // Evidence is a regenerable cache; compute this tenant's so its xlsx has a data row.
        mockMvc.perform(post("/api/v1/evidences/regenerate").param("year", "2026")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk());

        byte[] otherZip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        // Open the evidence xlsx from inside the ZIP; its work-point column must contain only
        // this tenant's work point, never the demo tenant's — proving the dossier is scoped.
        byte[] xlsx = readEntryBytes(otherZip, "evidenta-2026.xlsx");
        List<String> workPointNames = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 5; r <= sheet.getLastRowNum(); r++) { // header block is 5 rows
                Row row = sheet.getRow(r);
                if (row != null && row.getCell(0) != null
                        && !row.getCell(0).getStringCellValue().isBlank()) {
                    workPointNames.add(row.getCell(0).getStringCellValue());
                }
            }
        }
        assertThat(workPointNames).containsOnly("PL-UNIC-" + suffix);
    }

    private List<String> zipEntryNames(byte[] zipBytes) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                names.add(e.getName());
            }
        }
        assertThat(zipBytes).isNotEmpty();
        return names;
    }

    /** Extracts a single entry's (decompressed) bytes from the outer ZIP. */
    private byte[] readEntryBytes(byte[] zipBytes, String entryName) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().equals(entryName)) {
                    return zis.readAllBytes();
                }
            }
        }
        throw new AssertionError("Entry not found: " + entryName);
    }
}
