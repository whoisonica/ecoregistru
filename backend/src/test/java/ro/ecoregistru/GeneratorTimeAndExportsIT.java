package ro.ecoregistru;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.service.CloudinaryStorageService;
import ro.ecoregistru.service.DeadlineCalendarScheduler;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.DriverDataRetentionScheduler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA de lansare, generator — lotul 3: G18, G19, G20, G21, G22, G23
 * (`ecoregistru-docs/qa/LAUNCH-GAP-MAP.md`).
 *
 * <p>Termenele și jobul de dimineață, datele șoferului după termenul de păstrare, entitățile
 * dezactivate și șterse, precizia și injecția în exportul xlsx, atașamentul lipsă din dosar.
 * Testele {@code characterizes_…} fixează comportamentul de azi pe o [DECIZIE] încă nedecisă.
 *
 * <p>Cloudinary e înlocuit: dosarul nu are voie să iasă pe rețea din probe, iar G23 cere exact o
 * descărcare care pică.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class GeneratorTimeAndExportsIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired AttachmentRepository attachmentRepository;
    @Autowired ReportingDeadlineRepository deadlineRepository;
    @Autowired DeadlineCalendarScheduler calendarScheduler;
    @Autowired DriverDataRetentionScheduler retentionScheduler;
    @MockitoSpyBean DeadlineService deadlineService;
    @MockitoBean CloudinaryStorageService storageService;

    private Company company;
    private String token;
    private WorkPoint workPoint;
    private Partner collector;
    private WasteCode paper;

    @BeforeEach
    void setUp() {
        company = newCompany("Timp SRL");
        token = tokenFor(company);
        workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Timp").active(true).createdAt(Instant.now()).build());
        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector Timp SRL").cui("RO7" + suffix())
                .authorizationNumber("AM 3/2025").type(PartnerType.COLLECTOR).supplier(true)
                .active(true).createdAt(Instant.now()).build());
        paper = wasteCodeRepository.findByCode("20 01 01").orElseThrow();
    }

    // ---------- G18: termenele ----------

    /**
     * Jobul de dimineață trece prin toate firmele active. O firmă care aruncă nu are voie să lase
     * restul fără calendar: fiecare are tranzacția ei și excepția e prinsă pe firmă
     * ({@code DeadlineCalendarScheduler.run}).
     */
    @Test
    void oneCompanyFailingDoesNotLeaveTheOthersWithoutDeadlines() {
        Company broken = newCompany("Stricată SRL");
        doThrow(new IllegalStateException("profil corupt"))
                .when(deadlineService).ensureUpcoming(eq(broken.getId()), any(LocalDate.class));

        calendarScheduler.run(LocalDate.of(2026, 9, 16));

        assertThat(deadlinesOf(broken)).isEmpty();
        assertThat(deadlinesOf(company)).extracting(ReportingDeadline::getReportType)
                .contains(ReportType.SIM_ANNUAL);
    }

    /**
     * // DECIZIE: G18. 15 martie 2026 e duminică. Termenul SIM pe 2025 rămâne pe 15.03.2026, nu se
     * mută pe luni, 16.03.
     */
    @Test
    void characterizes_aDeadlineOnASundayStaysOnTheSunday() {
        deadlineService.ensureUpcoming(company.getId(), LocalDate.of(2025, 9, 16));

        ReportingDeadline sim = deadlinesOf(company).stream()
                .filter(d -> d.getReportType() == ReportType.SIM_ANNUAL).findFirst().orElseThrow();
        assertThat(sim.getDueDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(sim.getDueDate().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
    }

    // ---------- G19: datele șoferului după termenul de păstrare ----------

    /**
     * // DECIZIE: G19 (întrebare, nu bug). O predare din 2022, cu Anexa 3 tipărită atunci. Pe
     * 19.09.2026 jobul șterge numele și actul șoferului de pe mișcările de dinainte de 01.01.2023
     * (3 ani, OUG 92/2021 art. 48 alin. (5)). Retipărită, Anexa 3 păstrează numărul și numărul
     * mașinii, dar nu mai are șoferul: nu mai e aceeași foaie cu cea care a mers cu camionul.
     */
    @Test
    void characterizes_aReprintAfterTheRetentionPurgeKeepsTheNumberAndLosesTheDriver() throws Exception {
        String id = handover("2022-05-10", "40", ", \"driverName\": \"Ionescu Petru\", "
                + "\"driverIdentification\": \"CJ 123456\", \"vehicleRegistration\": \"CJ-11-ABC\"");
        String before = pdfText("/api/v1/movements/" + id + "/anexa3");
        assertThat(before).contains("Ionescu Petru").contains("CJ-11-ABC");
        Integer number = movementRepository.findById(UUID.fromString(id)).orElseThrow().getAnexa3Number();

        retentionScheduler.purge(LocalDate.of(2026, 9, 19));

        String after = pdfText("/api/v1/movements/" + id + "/anexa3");
        assertThat(after).doesNotContain("Ionescu Petru").doesNotContain("CJ 123456").contains("CJ-11-ABC");
        assertThat(movementRepository.findById(UUID.fromString(id)).orElseThrow().getAnexa3Number())
                .isEqualTo(number);
    }

    // ---------- G20: entități dezactivate și șterse ----------

    /** // DECIZIE: G20. Punct de lucru și partener dezactivați: o predare nouă pe ei se acceptă. */
    @Test
    void characterizes_aNewMovementOnADeactivatedWorkPointAndPartnerIsAccepted() throws Exception {
        workPoint.setActive(false);
        workPointRepository.save(workPoint);
        collector.setActive(false);
        partnerRepository.save(collector);

        postMovement(handoverJson("2026-07-05", "5", "")).andExpect(status().isOk());
    }

    /**
     * O mișcare ștearsă: Anexa ei nu se mai tipărește, iar numărul ei nu se dă altei mișcări. Două
     * foi de hârtie cu același număr ar exista deja, una în mâna colectorului.
     */
    @Test
    void aDeletedMovementsAnexa3NumberIsNeverGivenAgain() throws Exception {
        String first = handover("2026-07-05", "5", "");
        pdfText("/api/v1/movements/" + first + "/anexa3"); // nr. 1
        mockMvc.perform(delete("/api/v1/movements/" + first).header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/v1/movements/" + first + "/anexa3").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        String second = handover("2026-07-06", "5", "");
        pdfText("/api/v1/movements/" + second + "/anexa3");
        assertThat(movementRepository.findById(UUID.fromString(second)).orElseThrow().getAnexa3Number())
                .isEqualTo(2);
    }

    // ---------- G21 + G22: exportul xlsx ----------

    /**
     * Celula xlsx trece prin {@code double}. Valorile de mână, adunate exact: 0,001 + 1.234.567,891
     * + 3 × 0,1 = 1.234.568,192 kg în iulie. Plus o lună cu maximul coloanei {@code quantity},
     * NUMERIC(14,3): 99.999.999.999,999 kg. Celula citită înapoi trebuie să fie exact valoarea din DB.
     */
    @Test
    void theXlsxCellsCarryTheExactDatabaseValue() throws Exception {
        for (String kg : new String[]{"0.001", "1234567.891", "0.1", "0.1", "0.1"}) {
            handover("2026-07-05", kg, "");
        }
        handover("2026-08-05", "99999999999.999", "");

        List<BigDecimal> numbers = xlsxNumbers(2026);

        assertThat(numbers).anySatisfy(v -> assertThat(v).isEqualByComparingTo("1234568.192"));
        assertThat(numbers).anySatisfy(v -> assertThat(v).isEqualByComparingTo("99999999999.999"));
        assertThat(numbers).noneSatisfy(v -> assertThat(v.scale()).isGreaterThan(3));
    }

    /**
     * Un punct de lucru numit ca o formulă. POI scrie celule text ({@code setCellValue(String)}),
     * deci Excel nu-l evaluează. Proba fixează asta.
     */
    @Test
    void aNameThatLooksLikeAFormulaStaysText() throws Exception {
        workPoint.setName("=HYPERLINK(\"http://x.test\",\"Click\")");
        workPointRepository.save(workPoint);
        handover("2026-07-05", "5", "");

        try (Workbook wb = xlsx(2026)) {
            List<Cell> matches = new ArrayList<>();
            for (Row row : wb.getSheetAt(0)) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.FORMULA
                            || (cell.getCellType() == CellType.STRING
                            && cell.getStringCellValue().startsWith("=HYPERLINK"))) {
                        matches.add(cell);
                    }
                }
            }
            assertThat(matches).isNotEmpty().allSatisfy(c -> assertThat(c.getCellType()).isEqualTo(CellType.STRING));
        }
    }

    // ---------- G23: atașamentul care nu se descarcă ----------

    /** Dosarul se face și fără el, iar lista atașamentelor spune că lipsește. */
    @Test
    void aDossierAttachmentThatFailsToDownloadIsNamedAsMissing() throws Exception {
        when(storageService.fetch(anyString())).thenThrow(new IOException("Cloudinary nu răspunde"));
        String id = handover("2026-07-05", "5", ", \"documentReference\": \"Aviz 77\"");
        attachmentRepository.save(Attachment.builder()
                .movement(movementRepository.findById(UUID.fromString(id)).orElseThrow())
                .url("https://example.test/aviz-77.pdf").publicId("p-" + UUID.randomUUID())
                .fileName("aviz-77.pdf").contentType("application/pdf").sizeBytes(1000L)
                .createdAt(Instant.now()).build());

        byte[] zip = mockMvc.perform(get("/api/v1/audit-file").param("year", "2026")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        String index = zipEntryEndingWith(zip, "index.txt");
        assertThat(index).contains("aviz-77.pdf").contains("inclus în arhivă: NU");
    }

    // --- helpers ---

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Company newCompany(String name) {
        return companyRepository.save(Company.builder()
                .name(name).cui("ROT" + suffix()).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
    }

    private String tokenFor(Company c) {
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("timp+" + suffix() + "@demo.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(c).enabled(true).createdAt(Instant.now()).build());
        return jwtService.generateToken(admin);
    }

    private List<ReportingDeadline> deadlinesOf(Company c) {
        return deadlineRepository.findAll().stream()
                .filter(d -> d.getCompany().getId().equals(c.getId())).toList();
    }

    private String handover(String date, String kg, String extra) throws Exception {
        String json = postMovement(handoverJson(date, kg, extra)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private ResultActions postMovement(String body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/movements").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String handoverJson(String date, String kg, String extra) {
        return """
                {"workPointId": "%s", "date": "%s", "wasteCodeId": "%s", "quantity": %s,
                 "unit": "KG", "physicalState": "SOLID", "operation": "RECOVERED", "register": "ANEXA_1",
                 "wasteDestination": "Vr", "operationCode": "R13", "partnerId": "%s"%s}
                """.formatted(workPoint.getId(), date, paper.getId(), kg, collector.getId(), extra);
    }

    private String pdfText(String url) throws Exception {
        byte[] pdf = mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
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

    private Workbook xlsx(int year) throws Exception {
        byte[] body = mockMvc.perform(get("/api/v1/evidences/export")
                        .param("year", String.valueOf(year)).param("format", "xlsx")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        return new XSSFWorkbook(new ByteArrayInputStream(body));
    }

    private List<BigDecimal> xlsxNumbers(int year) throws Exception {
        List<BigDecimal> numbers = new ArrayList<>();
        try (Workbook wb = xlsx(year)) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.NUMERIC) {
                        // Ce vede Excel: double-ul scris în fișier, cea mai scurtă formă zecimală.
                        numbers.add(new BigDecimal(Double.toString(cell.getNumericCellValue())).stripTrailingZeros());
                    }
                }
            }
        }
        return numbers;
    }

    private static String zipEntryEndingWith(byte[] zip, String suffix) throws IOException {
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (entry.getName().endsWith(suffix)) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("nicio intrare " + suffix + " în arhivă");
    }
}
