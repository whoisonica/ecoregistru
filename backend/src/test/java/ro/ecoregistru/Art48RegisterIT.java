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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
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
 * The art. 48 chronological record (OUG 92/2021 art. 48 alin. (1)) — AD, closed on 15.09.2026 in
 * the act: there is no form to reproduce, so the document is the chronological table plus the
 * year's totals in the shape of the SIM "Colectare/Tratare" questionnaire. See
 * docs/surse-oficiale.md §2.1-bis.
 *
 * <p>What these pin down, each on whole rows so a column moved is a column caught:
 * <ul>
 *   <li>only the {@code ART_48} register: the company's own generated waste stays on anexa 1;</li>
 *   <li>only the year asked, chronologically — but the opening stock is what earlier years left;</li>
 *   <li>the per-code arithmetic of the questionnaire (opening + collected = recovered + disposed +
 *       closing), in tonnes, with a quantity entered in tonnes counted once;</li>
 *   <li>recipients summed per code and operation code, R in table A and D in table B;</li>
 *   <li>a pure generator gets no register.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class Art48RegisterIT {

    private static final String URL = "/api/v1/evidences/registru-cronologic";
    private static final int FIRST_DATA_ROW = 5;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired WeighingOperationRepository weighingOperationRepository;
    @Autowired NaturalPersonRepository naturalPersonRepository;

    private String token;
    private UUID adminId;
    private Company company;
    private UUID workPointId;
    private Partner supplier;
    private Partner recycler;
    private Partner landfill;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        company = companyRepository.save(Company.builder()
                .name("Colector Cronologic " + suffix).cui("ROA" + suffix)
                // BOTH: it also generates, which is what lets the test show generated waste stays out.
                .type(CompanyType.BOTH)
                .active(true).createdAt(Instant.now()).build());
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("art48+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        token = jwtService.generateToken(admin);
        adminId = admin.getId();
        workPointId = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit Nord").active(true).createdAt(Instant.now())
                .build()).getId();
        supplier = partner("Magazin Alfa SRL", "RO100" + suffix.substring(0, 3), PartnerType.GENERATOR, null);
        recycler = partner("Reciclator Beta SA", "RO200" + suffix.substring(0, 3), PartnerType.RECOVERER, "Str. Fabricii 2, Cluj");
        landfill = partner("Depozit Gamma SRL", "RO300" + suffix.substring(0, 3), PartnerType.RECOVERER, null);
    }

    private void fixture() throws Exception {
        // 2025: stock carried into the year.
        movement("2025-11-20", "15 01 01", "KG", "1000", "COLLECTED", null, supplier, null);
        movement("2025-12-10", "15 01 01", "KG", "400", "RECOVERED", "R3", recycler, "ART_48");
        // 2026, out of date order on purpose: the table sorts.
        movement("2026-04-02", "15 01 01", "KG", "500", "RECOVERED", "R3", recycler, "ART_48");
        movement("2026-03-15", "15 01 01", "KG", "700", "COLLECTED", null, supplier, null);
        movement("2026-05-06", "15 01 02", "TONS", "2", "COLLECTED", null, supplier, null);
        movement("2026-06-11", "15 01 02", "KG", "300", "DISPOSED", "D1", landfill, "ART_48");
        movement("2026-06-20", "15 01 02", "KG", "200", "DISPOSED", "D1", landfill, "ART_48");
        // The company's own waste: anexa 1, never here.
        movement("2026-03-01", "20 01 01", "KG", "900", "RECOVERED", "R3", recycler, "ANEXA_1");
        // Next year: outside the document.
        movement("2027-01-05", "15 01 01", "KG", "50", "COLLECTED", null, supplier, null);
    }

    @Test
    void theChronologicalTableHoldsTheYearsArt48MovementsInDateOrder() throws Exception {
        fixture();
        try (Workbook wb = xlsx()) {
            List<List<Object>> rows = dataRows(wb.getSheet("Cronologic"), 15);
            assertThat(rows).containsExactly(
                    List.of("Martie", "15.03.2026", "Depozit Nord", "Preluare", "15 01 01", name("15 01 01"),
                            700.0, 0.7, "Magazin Alfa SRL", supplier.getCui(), "", "", "", "", ""),
                    List.of("Aprilie", "02.04.2026", "Depozit Nord", "Predare la valorificare", "15 01 01", name("15 01 01"),
                            500.0, 0.5, "Reciclator Beta SA", recycler.getCui(), "", "R3", "", "", ""),
                    List.of("Mai", "06.05.2026", "Depozit Nord", "Preluare", "15 01 02", name("15 01 02"),
                            2000.0, 2.0, "Magazin Alfa SRL", supplier.getCui(), "", "", "", "", ""),
                    List.of("Iunie", "11.06.2026", "Depozit Nord", "Predare la eliminare", "15 01 02", name("15 01 02"),
                            300.0, 0.3, "Depozit Gamma SRL", landfill.getCui(), "", "D1", "", "", ""),
                    List.of("Iunie", "20.06.2026", "Depozit Nord", "Predare la eliminare", "15 01 02", name("15 01 02"),
                            200.0, 0.2, "Depozit Gamma SRL", landfill.getCui(), "", "D1", "", "", ""));
        }
    }

    /**
     * D1.12 — „originea” of lit. a), on takeovers only. A depot line reads it from its operation
     * (a natural person is always „populaţie” and has no partner to name); a movement typed directly
     * falls back to the partner's record; an operation's own choice beats the partner's record; an
     * exit and an unclassified takeover print nothing rather than a guess.
     */
    @Test
    void theChronologicalTableNamesTheOriginOfEachTakeover() throws Exception {
        supplier.setPackagingOrigin(PackagingOrigin.COLECTOR);
        partnerRepository.save(supplier);
        // The recycler has an origin on record too: an exit to it must still print none.
        recycler.setPackagingOrigin(PackagingOrigin.COMERCIANT);
        partnerRepository.save(recycler);
        movement("2026-02-01", "15 01 01", "KG", "100", "COLLECTED", null, supplier, null);
        movement("2026-02-02", "15 01 01", "KG", "60", "RECOVERED", "R3", recycler, "ART_48");
        movement("2026-02-03", "15 01 02", "KG", "70", "COLLECTED", null, landfill, null);
        NaturalPerson person = naturalPersonRepository.save(NaturalPerson.builder()
                .company(company).name("Ion Popescu").active(true).createdAt(Instant.now()).build());
        depotLine(depotOperation(1, null, person, PackagingOrigin.POPULATIE), "15 01 02", "20", null);
        // The partner's record says collector; what was chosen when this load came in wins.
        depotLine(depotOperation(2, supplier, null, PackagingOrigin.GENERATOR_PJ), "15 01 01", "30", supplier);

        try (Workbook wb = xlsx()) {
            assertThat(dataRows(wb.getSheet("Cronologic"), 15))
                    .extracting(row -> List.of(row.get(1), row.get(3), row.get(8), row.get(10)))
                    .containsExactly(
                            List.of("01.02.2026", "Preluare", "Magazin Alfa SRL", "colector"),
                            List.of("02.02.2026", "Predare la valorificare", "Reciclator Beta SA", ""),
                            List.of("03.02.2026", "Preluare", "Depozit Gamma SRL", ""),
                            List.of("04.02.2026", "Preluare", "", "populaţie"),
                            List.of("05.02.2026", "Preluare", "Magazin Alfa SRL", "generator persoană juridică"));
        }
    }

    @Test
    void chapterOneBalancesEachCodeFromTheStockEarlierYearsLeft() throws Exception {
        fixture();
        try (Workbook wb = xlsx()) {
            assertThat(dataRows(wb.getSheet("Cap. 1 Colectare"), 10)).containsExactly(
                    // opening 1000 - 400 = 0.6 t; + 0.7 collected - 0.5 recovered = 0.8 t closing
                    List.of("15 01 01", name("15 01 01"), 0.6, 0.7, 0.5, 0.0, 0.0, 0.8, "R3", ""),
                    List.of("15 01 02", name("15 01 02"), 0.0, 2.0, 0.0, 0.5, 0.0, 1.5, "", "D1"));
        }
    }

    @Test
    void chapterTwoSumsEachRecipientPerCodeAndOperation() throws Exception {
        fixture();
        try (Workbook wb = xlsx()) {
            assertThat(dataRows(wb.getSheet("Cap. 2A Valorificare"), 7)).containsExactly(
                    List.of("Reciclator Beta SA", recycler.getCui(), "Str. Fabricii 2, Cluj", "15 01 01",
                            name("15 01 01"), 0.5, "R3"));
            assertThat(dataRows(wb.getSheet("Cap. 2B Eliminare"), 7)).containsExactly(
                    List.of("Depozit Gamma SRL", landfill.getCui(), "", "15 01 02",
                            name("15 01 02"), 0.5, "D1"));
        }
    }

    @Test
    void thePdfNamesTheActAndCarriesTheSameTables() throws Exception {
        fixture();
        byte[] pdf = mockMvc.perform(get(URL).param("year", "2026").param("format", "pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/pdf")))
                .andExpect(header().string("Content-Disposition", containsString("evidenta-cronologica-2026.pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        // flat() drops whitespace, and the PDF writes ş/ţ with a cedilla (Cp1250).
        String text = Golden.flat(Golden.pdfText(pdf));
        assertThat(text)
                .contains("art.48alin.(1)")
                .contains("Iunie2026")
                .contains("ReciclatorBetaSA")
                .contains("DepozitGammaSRL")
                // Cap. 1, 15 01 01 in tonnes: opening, collected, recovered, disposed, no code, closing, R.
                .contains("0,6000,7000,5000,0000,0000,800R3")
                .doesNotContain("200101");
    }

    @Test
    void aGeneratorHasNoArt48Register() throws Exception {
        company.setType(CompanyType.GENERATOR);
        companyRepository.save(company);
        mockMvc.perform(get(URL).param("year", "2026").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("art48.register.collectors.only")));
    }

    // ------------------------------------------------------------------ helpers

    private Workbook xlsx() throws Exception {
        byte[] body = mockMvc.perform(get(URL).param("year", "2026")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("evidenta-cronologica-2026.xlsx")))
                .andReturn().getResponse().getContentAsByteArray();
        return new XSSFWorkbook(new ByteArrayInputStream(body));
    }

    /** Whole rows below the header, blank cells as "", numbers rounded to the gram. */
    private List<List<Object>> dataRows(Sheet sheet, int width) {
        List<List<Object>> rows = new ArrayList<>();
        for (int r = FIRST_DATA_ROW; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || row.getCell(0) == null || row.getCell(0).getStringCellValue().isBlank()) {
                break;
            }
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

    private WeighingOperation depotOperation(int number, Partner partner, NaturalPerson person,
                                             PackagingOrigin origin) {
        return weighingOperationRepository.saveAndFlush(WeighingOperation.builder()
                .company(company).workPoint(workPointRepository.findById(workPointId).orElseThrow())
                .type(WeighingOperationType.IN).number(number)
                .date(LocalDate.of(2026, 2, 3 + number))
                .partner(partner).naturalPerson(person).origin(origin)
                .status(WeighingOperationStatus.FINALIZED).finalizedAt(Instant.now()).finalizedBy(adminId)
                .createdBy(adminId).build());
    }

    private void depotLine(WeighingOperation op, String code, String kg, Partner partner) {
        movementRepository.saveAndFlush(WasteMovement.builder()
                .company(company).workPoint(op.getWorkPoint()).date(op.getDate())
                .wasteCode(wasteCodeRepository.findByCode(code).orElseThrow())
                .quantity(new BigDecimal(kg)).netKg(new BigDecimal(kg)).unit(Unit.KG)
                .operation(WasteOperation.COLLECTED).register(WasteRegister.ART_48)
                .partner(partner).weighingOperation(op).lineNo(1).deleted(false)
                .createdBy(adminId).build());
    }

    private Partner partner(String name, String cui, PartnerType type, String address) {
        return partnerRepository.save(Partner.builder().authorizationNumber("AUT-TEST")
                .company(company).name(name).cui(cui).type(type).address(address)
                .supplier(true).client(true).active(true).createdAt(Instant.now()).build());
    }

    private void movement(String date, String code, String unit, String quantity, String operation,
                          String operationCode, Partner partner, String register) throws Exception {
        StringBuilder extra = new StringBuilder();
        if (operationCode != null && !"ART_48".equals(register)) {
            // Decizia 19.09.2026: o predare de deșeu propriu poartă tot ce tipărește fișa.
            extra.append(", \"physicalState\": \"SOLID\", \"storageType\": \"CT\", \"transportMeans\": \"AN\", \"packagingCategory\": \"SECONDARY\"");
        }
        if (operationCode != null) extra.append(", \"wasteDestination\": \"Vr\", \"operationCode\": \"").append(operationCode).append('"');
        if (partner != null) extra.append(", \"partnerId\": \"").append(partner.getId()).append('"');
        if (register != null) extra.append(", \"register\": \"").append(register).append('"');
        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workPointId": "%s", "date": "%s", "wasteCodeId": "%s",
                                  "unit": "%s", "quantity": %s, "operation": "%s"%s
                                }
                                """.formatted(workPointId, date,
                                wasteCodeRepository.findByCode(code).orElseThrow().getId(),
                                unit, quantity, operation, extra)))
                .andExpect(status().isOk());
    }
}
