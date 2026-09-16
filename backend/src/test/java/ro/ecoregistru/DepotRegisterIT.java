package ro.ecoregistru;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.PriceVisibility;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D1.14 — registrul intrărilor și ieșirilor al depozitului, pe rânduri întregi, ca o coloană mutată
 * să fie o coloană prinsă. Numele din fixture sunt inventate; exportul real al depozitului, după care
 * s-au luat coloanele, nu intră niciodată într-un test.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DepotRegisterIT {

    private static final String URL = "/api/v1/weighing-operations/registru";
    private static final int FIRST_DATA_ROW = 4;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository userRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired NaturalPersonRepository personRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WeighingOperationRepository operationRepository;
    @Autowired WasteMovementRepository movementRepository;

    Company company;
    AppUser admin;
    AppUser operator;
    WorkPoint depot;
    Partner supplier;
    Partner recycler;
    NaturalPerson person;
    WasteArticle paper;
    WasteArticle iron;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        company = companyRepository.save(Company.builder()
                .name("Depozit Registru " + suffix + " SRL").cui("ROR" + suffix)
                .type(CompanyType.COLLECTOR).active(true).createdAt(Instant.now()).build());
        admin = user("admin", Role.ADMIN, suffix, "Maria", "Ionescu");
        operator = user("operator", Role.OPERATOR, suffix, null, null);
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit Sud").active(true).createdAt(Instant.now()).build());
        supplier = partner("Magazin Alfa SRL", PartnerType.GENERATOR);
        recycler = partner("Reciclator Beta SA", PartnerType.RECOVERER);
        person = personRepository.save(NaturalPerson.builder()
                .company(company).name("Ion Popescu").cnp("1900101123457").identification("CJ 123456")
                .address("Cluj, str. Mare 1").active(true).createdAt(Instant.now()).build());
        paper = article("Carton", "15 01 01", false);
        iron = article("Fier vechi", "17 04 05", true);
    }

    private void fixture() {
        // Out of order on purpose: the register sorts by day, then IN before OUT, then number.
        WeighingOperation out = operation(WeighingOperationType.OUT, 7, "2026-03-05", recycler, null,
                WeighingOperationStatus.CANCELLED);
        out.setCancelledAt(Instant.parse("2026-03-06T08:00:00Z"));
        out.setCancelledBy(admin.getId());
        out.setCancelReason("Cântărire greșită");
        out.setOrderNumber("CMD-12");
        operationRepository.saveAndFlush(out);
        line(out, 1, paper, "2000", "1500", "500", "480", "0.40", WasteOperationCode.R3);

        WeighingOperation fromPartner = operation(WeighingOperationType.IN, 2, "2026-03-05", supplier, null,
                WeighingOperationStatus.FINALIZED);
        fromPartner.setGrossKg(new BigDecimal("8000"));
        fromPartner.setTareKg(new BigDecimal("7000"));
        operationRepository.saveAndFlush(fromPartner);
        line(fromPartner, 1, paper, "8000", "7600", "400", "390", "0.50", null);
        line(fromPartner, 2, iron, "7600", "7000", "600", "600", "1.20", null);

        line(operation(WeighingOperationType.IN, 1, "2026-03-02", null, person, WeighingOperationStatus.IN_PROGRESS),
                1, iron, null, null, "35", "35", "1.10", null);

        // Other months, one on each side: not in the March register.
        line(operation(WeighingOperationType.IN, 3, "2026-04-01", supplier, null, WeighingOperationStatus.FINALIZED),
                1, paper, null, null, "10", "10", null, null);
        line(operation(WeighingOperationType.OUT, 4, "2026-02-27", recycler, null, WeighingOperationStatus.FINALIZED),
                1, paper, null, null, "20", "20", null, WasteOperationCode.R3);
    }

    @Test
    void oneRowPerLineWithTheHeadRepeatedInScaleOrder() throws Exception {
        fixture();
        try (Workbook wb = xlsx(admin, "3")) {
            Sheet sheet = wb.getSheet("Registru");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Registrul intrărilor și ieșirilor");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).endsWith("Luna 03.2026");
            assertThat(columnNames(sheet)).containsExactly(
                    "Data", "Tip", "Nr. operațiune", "Nr. comandă", "Depozit", "Rol partener",
                    "Client / generator / destinatar", "Sortiment", "Cod deșeu", "Denumire deșeu",
                    "Cantitate (kg)", "Mașină", "Șofer / delegat",
                    "Brut (kg)", "Tara (kg)", "Neto (kg)", "Final (kg)",
                    "Brut operațiune (kg)", "Tara operațiune (kg)", "Cod R/D",
                    "Stare", "Data finalizării", "Data anulării", "Anulat de", "Motiv anulare",
                    "Preț (lei/kg)", "Valoare (lei)");
            String finalizedOn = LocalDate.now(java.time.ZoneId.of("Europe/Bucharest"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            assertThat(dataRows(sheet, 27)).containsExactly(
                    List.of("02.03.2026", "Intrare", 1.0, "", "Depozit Sud", "Partener PF", "Ion Popescu",
                            "Fier vechi", "17 04 05", name("17 04 05"), 35.0, "B 01 ABC", "Vasile Șofer",
                            "", "", 35.0, 35.0, "", "", "", "În lucru", "", "", "", "", 1.1, 38.5),
                    List.of("05.03.2026", "Intrare", 2.0, "", "Depozit Sud", "Client / generator", "Magazin Alfa SRL",
                            "Carton", "15 01 01", name("15 01 01"), 390.0, "B 01 ABC", "Vasile Șofer",
                            8000.0, 7600.0, 400.0, 390.0, 8000.0, 7000.0, "", "Finalizată", finalizedOn, "", "", "",
                            0.5, 195.0),
                    List.of("05.03.2026", "Intrare", 2.0, "", "Depozit Sud", "Client / generator", "Magazin Alfa SRL",
                            "Fier vechi", "17 04 05", name("17 04 05"), 600.0, "B 01 ABC", "Vasile Șofer",
                            7600.0, 7000.0, 600.0, 600.0, 8000.0, 7000.0, "", "Finalizată", finalizedOn, "", "", "",
                            1.2, 720.0),
                    List.of("05.03.2026", "Ieșire", 7.0, "CMD-12", "Depozit Sud", "Destinatar", "Reciclator Beta SA",
                            "Carton", "15 01 01", name("15 01 01"), 480.0, "B 01 ABC", "Vasile Șofer",
                            2000.0, 1500.0, 500.0, 480.0, "", "", "R3", "Anulată", "", "06.03.2026",
                            "Maria Ionescu", "Cântărire greșită", 0.4, 192.0));
        }
    }

    /** D1.8 — cine nu vede prețurile nu primește nici coloanele lor; restul registrului, da. */
    @Test
    void thePriceColumnsLeaveForWhoeverCannotSeePrices() throws Exception {
        fixture();
        company.setPriceVisibility(PriceVisibility.ADMIN_ONLY);
        companyRepository.save(company);
        try (Workbook wb = xlsx(operator, "3")) {
            Sheet sheet = wb.getSheet("Registru");
            assertThat(columnNames(sheet)).hasSize(25).doesNotContain("Preț (lei/kg)", "Valoare (lei)");
            assertThat(dataRows(sheet, 30)).hasSize(4)
                    .allSatisfy(row -> assertThat(row.subList(25, 30)).containsOnly(""));
        }
    }

    /** Persoana fizică apare cu numele; CNP-ul și actul nu intră în registru. */
    @Test
    void theRegisterCarriesNoPersonalIdentifiers() throws Exception {
        fixture();
        try (Workbook wb = xlsx(admin, null)) {
            Sheet sheet = wb.getSheet("Registru");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).endsWith("Anul 2026");
            List<List<Object>> rows = dataRows(sheet, 27);
            assertThat(rows).hasSize(6);
            assertThat(rows.toString()).contains("Ion Popescu").doesNotContain("1900101123457", "CJ 123456");
        }
    }

    @Test
    void anotherCompanysOperationsStayOut() throws Exception {
        fixture();
        Company other = companyRepository.save(Company.builder()
                .name("Alt Depozit SRL").cui("ROX" + UUID.randomUUID().toString().substring(0, 6))
                .type(CompanyType.COLLECTOR).active(true).createdAt(Instant.now()).build());
        AppUser stranger = userRepository.save(AppUser.builder()
                .email("strain+" + UUID.randomUUID() + "@demo.ro").password("x")
                .role(Role.ADMIN).company(other).enabled(true).createdAt(Instant.now()).build());
        try (Workbook wb = xlsx(stranger, "3")) {
            assertThat(dataRows(wb.getSheet("Registru"), 27)).isEmpty();
        }
    }

    // --- helpers ---

    private Workbook xlsx(AppUser user, String month) throws Exception {
        var request = get(URL).param("year", "2026").header("Authorization", "Bearer " + jwtService.generateToken(user));
        if (month != null) {
            request = request.param("month", month);
        }
        byte[] body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(
                        month == null ? "registru-intrari-iesiri-2026.xlsx" : "registru-intrari-iesiri-2026-03.xlsx")))
                .andReturn().getResponse().getContentAsByteArray();
        return new XSSFWorkbook(new ByteArrayInputStream(body));
    }

    private static List<String> columnNames(Sheet sheet) {
        List<String> names = new ArrayList<>();
        sheet.getRow(3).forEach(c -> names.add(c.getStringCellValue()));
        return names;
    }

    /** Rânduri întregi sub antet: celulă lipsă = "", numere rotunjite la gram. */
    private static List<List<Object>> dataRows(Sheet sheet, int width) {
        List<List<Object>> rows = new ArrayList<>();
        for (int r = FIRST_DATA_ROW; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            List<Object> cells = new ArrayList<>();
            for (int c = 0; c < width; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) {
                    cells.add("");
                } else if (cell.getCellType() == CellType.NUMERIC) {
                    cells.add(Math.round(cell.getNumericCellValue() * 1000) / 1000.0);
                } else {
                    cells.add(cell.getStringCellValue());
                }
            }
            rows.add(cells);
        }
        return rows;
    }

    private String name(String code) {
        return wasteCodeRepository.findByCode(code).orElseThrow().getName();
    }

    private AppUser user(String prefix, Role role, String suffix, String first, String last) {
        return userRepository.save(AppUser.builder()
                .email(prefix + "+registru" + suffix + "@demo.ro").password("x").firstName(first).lastName(last)
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private Partner partner(String name, PartnerType type) {
        return partnerRepository.save(Partner.builder()
                .company(company).name(name).cui("RO" + UUID.randomUUID().toString().substring(0, 6)).type(type)
                .supplier(true).client(true).active(true).createdAt(Instant.now()).build());
    }

    private WasteArticle article(String name, String code, boolean metal) {
        return articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findByCode(code).orElseThrow())
                .name(name).metal(metal).active(true).createdAt(Instant.now()).build());
    }

    private WeighingOperation operation(WeighingOperationType type, int number, String date, Partner partner,
                                        NaturalPerson person, WeighingOperationStatus status) {
        WeighingOperation.WeighingOperationBuilder op = WeighingOperation.builder()
                .company(company).workPoint(depot).type(type).number(number).date(LocalDate.parse(date))
                .partner(partner).naturalPerson(person)
                .origin(person != null ? ro.ecoregistru.enums.PackagingOrigin.POPULATIE : null)
                .driverName("Vasile Șofer").vehicleRegistration("B 01 ABC")
                .status(status).createdBy(admin.getId());
        if (status == WeighingOperationStatus.FINALIZED) {
            op.finalizedAt(Instant.now()).finalizedBy(admin.getId());
        }
        if (status == WeighingOperationStatus.CANCELLED) {
            op.cancelledAt(Instant.now()).cancelledBy(admin.getId()).cancelReason("-");
        }
        return operationRepository.saveAndFlush(op.build());
    }

    private void line(WeighingOperation op, int lineNo, WasteArticle article, String gross, String tare,
                      String net, String finalKg, String price, WasteOperationCode code) {
        BigDecimal quantity = new BigDecimal(finalKg);
        BigDecimal unitPrice = price == null ? null : new BigDecimal(price);
        movementRepository.saveAndFlush(WasteMovement.builder()
                .company(company).workPoint(depot).date(op.getDate()).wasteCode(article.getWasteCode())
                .article(article).weighingOperation(op).lineNo(lineNo)
                .grossKg(gross == null ? null : new BigDecimal(gross))
                .tareKg(tare == null ? null : new BigDecimal(tare))
                .netKg(new BigDecimal(net)).quantity(quantity).unit(Unit.KG)
                .unitPrice(unitPrice).totalValue(unitPrice == null ? null : unitPrice.multiply(quantity))
                .operation(code == null ? WasteOperation.COLLECTED
                        : code.isRecovery() ? WasteOperation.RECOVERED : WasteOperation.DISPOSED)
                .operationCode(code).register(WasteRegister.ART_48)
                .partner(op.getPartner()).deleted(false).createdBy(admin.getId()).build());
    }
}
