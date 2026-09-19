package ro.ecoregistru;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import ro.ecoregistru.service.importer.ImportTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Post-fix QA (19.09, după 244837b): surorile BUG-043 (CNP) și BUG-033/039/040 (lungimi/cifre)
 * pe căile care n-au trecut prin reparație. Fiecare test PICĂ pe codul de azi.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PostFixSurfaceProbeIT {

    private static final String CNP = "1900101123457";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired ConsultancyRepository consultancyRepository;
    @Autowired ImportTemplate template;

    private UUID companyId;
    private String adminToken;
    private String viewerToken;
    private String platformToken;
    private UUID point;
    private String pointName;
    private UUID paper;
    private UUID collector;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Suprafata " + suffix + " SRL").cui("ROS" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        companyId = company.getId();
        adminToken = jwtService.generateToken(user(company, Role.ADMIN));
        viewerToken = jwtService.generateToken(user(company, Role.CLIENT_VIEWER));
        platformToken = jwtService.generateToken(user(null, Role.PLATFORM_ADMIN));
        pointName = "PL " + suffix;
        point = workPointRepository.save(WorkPoint.builder()
                .company(company).name(pointName).active(true).createdAt(Instant.now()).build()).getId();
        paper = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector Suprafata SRL").authorizationNumber("AS 1/2025").cui("RO" + suffix)
                .type(PartnerType.COLLECTOR).supplier(true).active(true)
                .createdAt(Instant.now()).build()).getId();
    }

    // ---------- BUG-043: avizul tipărește CNP-ul întreg pentru „Vizualizare" ----------

    /**
     * {@code GET /movements/{id}/aviz} n-are {@code CAN_WRITE} (e o citire), iar {@code AvizGenerator}
     * tipărește {@code m.getDriverCnp()} direct, fără {@code SecurityUtils.cnpForCurrentUser}. Același
     * cont care primește „190********57" în {@code GET /movements} descarcă PDF-ul cu CNP-ul întreg.
     */
    @Test
    void aViewerCannotReachTheAvizAtAll() throws Exception {
        String body = mockMvc.perform(post("/api/v1/movements").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workPointId": "%s", "date": "2026-03-10", "wasteCodeId": "%s", "quantity": 100,
                                 "unit": "KG", "physicalState": "SOLID", "operation": "RECOVERED", "register": "ANEXA_1",
                                 "physicalState": "SOLID", "storageType": "CT", "transportMeans": "AN", "packagingCategory": "SECONDARY", "wasteDestination": "Vr", "operationCode": "R13", "partnerId": "%s",
                                 "driverName": "Ion Sofer", "driverCnp": "%s", "vehicleRegistration": "CJ 01 ABC"}
                                """.formatted(point, paper, collector, CNP)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        // Premisa: JSON-ul îl maschează pentru același cont.
        String asJson = mockMvc.perform(get("/api/v1/movements/" + id).header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(asJson).doesNotContain(CNP).contains("190********57");

        // Decizia (20.09.2026): avizul trece pe `CAN_WRITE`. Mascarea din liste ar fi fost decorativă
        // cât timp același cont descarcă PDF-ul cu CNP-ul întreg, iar un vizualizator n-are de ce
        // să tipărească un aviz — îl tipărește cel care predă deșeul, adică cel care scrie mișcarea.
        mockMvc.perform(get("/api/v1/movements/" + id + "/aviz").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());

        // Iar pentru cine are dreptul, CNP-ul e tot acolo: legea îl cere pe formular.
        byte[] pdf = mockMvc.perform(get("/api/v1/movements/" + id + "/aviz").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(pdfText(pdf)).contains(CNP);
    }

    // ---------- BUG-033/040: importul lasă să treacă ce coloana nu ține ----------

    /**
     * {@code ExcelImportService.importMovements} citește „Observații" cu {@code c.text(15, 2000)}, dar
     * {@code waste_movements.notes} e VARCHAR(1000) (și {@code WasteMovementRequest.notes} are
     * {@code @Size(max = 1000)}, ocolit de import). 1500 de semne: nu o eroare pe rând, ci 500 pe tot fișierul.
     */
    @Test
    void anImportedNoteLongerThanTheColumnIsA500NotARowError() throws Exception {
        Object[] row = {LocalDate.of(2026, 3, 10), pointName, "20 01 01", "Eliminare", 1200, "kg",
                "D5", "Deșeu propriu", null, "Fișa 3", "Solid", "CT", null, null, "DO", "x".repeat(1500)};
        byte[] xlsx = file(List.of(), List.<Object[]>of(row));
        String verified = importing("/api/v1/import/verificare", xlsx).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        System.out.println("VERIFY " + verified);
        // „Verifică" spune că fișierul e bun; importul adevărat trebuie să spună același lucru, nu 500.
        importing("/api/v1/import", xlsx).andExpect(status().isOk());
    }

    /**
     * Partenerii: „Nr. reg. comerțului" citit cu {@code c.text(9, 255)}, coloana VARCHAR(50);
     * „Autorizație" {@code c.text(6, 255)}, coloana VARCHAR(128). {@code partnerService.create} e
     * chemat direct, fără {@code @Valid}.
     */
    @Test
    void anImportedTradeRegisterLongerThanTheColumnIsA500NotARowError() throws Exception {
        Object[] partner = {"Partener Lung SRL", "RO12345678", "Colector", "Da", "Nu", "Nu",
                "AUT-12", LocalDate.of(2027, 1, 1), "Str. Fabricii 1", "J".repeat(120)};
        byte[] xlsx = file(List.<Object[]>of(partner), List.of());
        String verified = importing("/api/v1/import/verificare", xlsx).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        System.out.println("VERIFY " + verified);
        // „Verifică" spune că fișierul e bun; importul adevărat trebuie să spună același lucru, nu 500.
        importing("/api/v1/import", xlsx).andExpect(status().isOk());
    }

    // ---------- BUG-033/040: DTO-uri rămase în afara inventarului ----------

    /** {@code InviteConsultantRequest.firstName} n-are {@code @Size}; {@code app_users.first_name} e VARCHAR(128). */
    @Test
    void aConsultantInviteWithALongNameIsA500() throws Exception {
        Consultancy consultancy = consultancyRepository.save(Consultancy.builder()
                .name("Cabinet Proba").cui("RO" + UUID.randomUUID().toString().replaceAll("\\D", "").substring(0, 8))
                .createdAt(Instant.now()).build());
        mockMvc.perform(post("/api/v1/consultancies/" + consultancy.getId() + "/users")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"lung+" + UUID.randomUUID().toString().substring(0, 6)
                                + "@cabinet.ro\",\"firstName\":\"" + "A".repeat(200) + "\"}"))
                .andExpect(status().is4xxClientError());
    }

    /** {@code OnboardClientRequest.Admin} n-are {@code @Size}; serviciul construiește {@code InviteUserRequest} fără validare. */
    @Test
    void onboardingAClientWithALongAdminNameIsA500() throws Exception {
        String cui = "RO" + UUID.randomUUID().toString().replaceAll("\\D", "").substring(0, 8);
        mockMvc.perform(post("/api/v1/companies/onboard")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": {"name": "Onboard Lung SRL", "cui": "%s", "type": "GENERATOR"},
                                 "admin": {"email": "admin+%s@lung.ro", "firstName": "%s"}}
                                """.formatted(cui, UUID.randomUUID().toString().substring(0, 6), "B".repeat(200))))
                .andDo(r -> System.out.println("ONBOARD " + r.getResponse().getStatus() + " " + r.getResponse().getContentAsString()))
                .andExpect(status().is4xxClientError());
    }

    /** {@code PackagingMarketRequest}: {@code @PositiveOrZero} fără {@code @Digits}; coloanele sunt NUMERIC(14,3). */
    @Test
    void aPackagingMarketFigureTooLargeForItsColumnIsA500() throws Exception {
        mockMvc.perform(put("/api/v1/packaging/market")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"material\":\"STICLA\",\"year\":2026,\"salesPackaging\":1000000000000}"))
                .andExpect(status().is4xxClientError());
    }

    // --- helpers ---

    private org.springframework.test.web.servlet.ResultActions importing(String path, byte[] xlsx) throws Exception {
        return mockMvc.perform(multipart(path)
                .file(new MockMultipartFile("file", "import.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx))
                .header("Authorization", "Bearer " + platformToken)
                .header("X-Tenant-Id", companyId.toString()));
    }

    private AppUser user(Company company, Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + UUID.randomUUID().toString().substring(0, 8) + "@suprafata.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private static String pdfText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
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

    private byte[] file(List<Object[]> partners, List<Object[]> movements) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(template.render()));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle date = wb.createCellStyle();
            date.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy"));
            fill(wb.getSheet("Parteneri"), partners, date);
            fill(wb.getSheet("Mișcări"), movements, date);
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void fill(Sheet sheet, List<Object[]> rows, CellStyle date) {
        for (int i = 0; i < rows.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Object[] values = rows.get(i);
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
    }
}
