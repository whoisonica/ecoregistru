package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.controller.request.BalingRequest;
import ro.ecoregistru.controller.request.TransferReceiptRequest;
import ro.ecoregistru.controller.request.UserWorkPointsRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest.Line;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.DepotReportKind;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.PaymentMethod;
import ro.ecoregistru.enums.PriceVisibility;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.CompanyUserService;
import ro.ecoregistru.service.DepotReportService;
import ro.ecoregistru.service.StockService;
import ro.ecoregistru.service.WeighingOperationService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ro.ecoregistru.enums.WeighingOperationType.IN;
import static ro.ecoregistru.enums.WeighingOperationType.OUT;
import static ro.ecoregistru.enums.WeighingOperationType.TRANSFER;

/**
 * D4.7 — rapoartele fixe ale depozitului, citite înapoi din fișier: fișa de stoc se închide pe soldul de pe ecranul Stoc,
 * seria documentelor își arată golurile, tranzitul și diferențele, anulatele cu motiv și autor, banii (2% AFM, D100/D205,
 * numerarul peste plafon) pe toată firma și numai pentru cine aprobă și vede prețurile, operatorul doar pe depozitele lui.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DepotReportIT {

    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);
    private static final LocalDate DAY = LocalDate.of(2026, 9, 15);

    @Autowired DepotReportService reports;
    @Autowired WeighingOperationService operations;
    @Autowired StockService stock;
    @Autowired CompanyUserService users;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired NaturalPersonRepository personRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WeighingOperationRepository operationRepository;
    @Autowired ro.ecoregistru.service.BalingService balings;

    Company company;
    AppUser admin;
    WorkPoint depotA;
    WorkPoint depotB;
    Partner partner;
    Partner recycler;
    NaturalPerson person;
    WasteArticle cardboard;
    WasteArticle copper;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.builder()
                .name("Rapoarte " + suffix() + " SRL").cui("ROP" + suffix()).type(CompanyType.COLLECTOR)
                .environmentalAuthNumber("AM-" + suffix()).active(true).createdAt(Instant.now()).build());
        admin = user(Role.ADMIN);
        depotA = depot("Baciu");
        depotB = depot("Turda");
        partner = partner("Magazin Alfa", PartnerType.GENERATOR);
        recycler = partner("Reciclare Beta", PartnerType.GENERATOR);
        person = personRepository.save(NaturalPerson.builder()
                .company(company).name("Ion Popescu").cnp("1900101123457")
                .identification("CJ 123456").address("Cluj, str. Mare 1")
                .active(true).createdAt(Instant.now()).build());
        var codes = wasteCodeRepository.findAll().stream().filter(c -> !c.isHazardous()).toList();
        cardboard = article("Carton", codes.get(0), false);
        copper = article("Cupru", codes.get(1), true);
        actAs(admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // --- stoc ---

    /** Soldul final al fișei = stocul de pe ecranul Stoc la „până la”; ce e în lucru sau anulat nu intră. */
    @Test
    void theStockCardClosesOnTheStockOfTheStockScreen() {
        finalized(head(IN, depotA, FROM.minusDays(3), partner, null), line(cardboard, "1000", "0.50"));
        finalized(head(IN, depotA, FROM, partner, null), line(cardboard, "50", "0.50"));
        finalized(head(IN, depotA, DAY, partner, null), line(cardboard, "500", "0.50"));
        finalized(head(OUT, depotA, DAY.plusDays(1), recycler, null), outLine(cardboard, "400"));
        finalized(transfer(DAY.plusDays(2)), moved(cardboard, "200"));
        draft(head(IN, depotA, DAY, partner, null), line(cardboard, "999", "0.50"));
        UUID cancelled = draft(head(IN, depotA, DAY, partner, null), line(cardboard, "777", "0.50"));
        operations.cancel(cancelled, "cântărit greșit");

        List<List<String>> sheet = xlsx(DepotReportKind.STOCK_CARD, depotA.getId(), null, null);

        assertThat(rowStarting(sheet, "Sold inițial")).contains("1000");
        assertThat(rowsContaining(sheet, "Intrare nr.")).as("și cea din ziua „de la”").hasSize(2);
        assertThat(rowsContaining(sheet, "Ieșire nr.")).hasSize(1);
        assertThat(rowsContaining(sheet, "Transfer trimis nr.")).hasSize(1);
        assertThat(rowsContaining(sheet, "777")).isEmpty();
        assertThat(rowsContaining(sheet, "999")).isEmpty();
        BigDecimal onScreen = stock.stock(depotA.getId(), TO).rows().stream()
                .filter(r -> cardboard.getId().equals(r.articleId())).findFirst().orElseThrow().stockKg();
        assertThat(onScreen).isEqualByComparingTo("950");
        assertThat(rowStarting(sheet, "Sold final")).contains("950");
    }

    /** Transferul plecat și nerecepționat e în tranzit; cel recepționat cu lipsă își arată diferența și motivul. */
    @Test
    void transfersShowWhatIsOnTheRoadAndTheDifferenceAtReceipt() {
        finalized(head(IN, depotA, FROM, partner, null), line(cardboard, "1000", "0.50"));
        UUID onTheRoad = finalized(transfer(DAY.minusDays(20)), moved(cardboard, "100"));
        UUID received = finalized(transfer(DAY), moved(cardboard, "300"));
        WeighingOperationResponse sent = operations.get(received);
        operations.receive(received, new TransferReceiptRequest(DAY.plusDays(1), null, null, null, List.of(
                new TransferReceiptRequest.Line(sent.lines().get(0).id(), null, null, new BigDecimal("250"), null)),
                "NIR 7", "lipsă la descărcare", null));

        UUID late = finalized(transfer(TO.minusDays(1)), moved(cardboard, "40"));
        WeighingOperationResponse lateSent = operations.get(late);
        operations.receive(late, new TransferReceiptRequest(TO.plusDays(2), null, null, null, List.of(
                new TransferReceiptRequest.Line(lateSent.lines().get(0).id(), null, null, new BigDecimal("40"), null)),
                null, null, null));

        List<List<String>> sheet = xlsx(DepotReportKind.TRANSFERS, null, null, null);

        List<String> road = rowWith(sheet, number(onTheRoad));
        assertThat(road).contains("În tranzit");
        assertThat(rowWith(sheet, number(late))).as("recepționat după „până la”").contains("În tranzit");
        // Pe Turda, jurnalul arată doar sosirea transferului, nu plecarea din Baciu.
        List<List<String>> atB = xlsx(DepotReportKind.SCALE_LOG, depotB.getId(), null, null);
        assertThat(rowsContaining(atB, "Transfer primit")).isNotEmpty();
        assertThat(rowsContaining(atB, "Transfer trimis")).isEmpty();
        List<String> done = rowWith(sheet, number(received));
        assertThat(done).contains("300", "250", "-50", "NIR 7", "lipsă la descărcare");
    }

    // --- registre ---

    /** Jurnalul de cântar: brut, tara, neto, final și impuritățile pe linie. */
    @Test
    void theScaleLogShowsEveryWeighingWithItsImpurities() {
        UUID id = operations.create(head(IN, depotA, DAY, partner, null)).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(new Line(cardboard.getId(),
                new BigDecimal("1500"), new BigDecimal("500"), null, new BigDecimal("950"), new BigDecimal("0.50"),
                null, null))));
        operations.finalizeOperation(id);
        WasteArticle baled = articleRepository.save(WasteArticle.builder().company(company)
                .wasteCode(cardboard.getWasteCode()).name("Carton balotat").sourceArticle(cardboard)
                .baleWeightKg(new BigDecimal("100")).active(true).createdAt(Instant.now()).build());
        balings.create(new BalingRequest(depotA.getId(), DAY, baled.getId(), 2, null));

        assertThat(rowsContaining(xlsx(DepotReportKind.SCALE_LOG, null, null, null), "Balotare:")).isEmpty();
        List<String> row = rowWith(xlsx(DepotReportKind.SCALE_LOG, null, null, null), "Carton");
        assertThat(row).contains("1500", "500", "1000", "950", "50", "Finalizată");
        // Alt depozit ales: cântărirea din Baciu nu e a lui.
        assertThat(rowsContaining(xlsx(DepotReportKind.SCALE_LOG, depotB.getId(), null, null), "Carton")).isEmpty();
    }

    /** Anulatele: motivul, cine și când. */
    @Test
    void cancelledOperationsCarryTheReasonAndWho() {
        UUID id = draft(head(IN, depotA, DAY, partner, null), line(cardboard, "100", "0.50"));
        operations.cancel(id, "dublură");

        List<String> row = rowWith(xlsx(DepotReportKind.CANCELLED, null, null, null), "dublură");
        assertThat(row).contains(number(id), "Magazin Alfa SRL", admin.getEmail());
    }

    /**
     * Seria documentelor se citește pe toată firma: un număr sărit apare ca lipsă, iar documentul rămas pe o operațiune
     * anulată se vede cu starea ei.
     */
    @Test
    void theIssuedDocumentsShowTheGapsInTheSeries() {
        UUID first = finalized(head(IN, depotA, DAY, null, person), line(cardboard, "100", "0.50"));
        UUID second = finalized(head(IN, depotA, DAY, null, person), line(cardboard, "100", "0.50"));
        var skipped = operationRepository.findById(second).orElseThrow();
        skipped.setBorderouNumber(3);
        operationRepository.saveAndFlush(skipped);
        operations.cancel(second, "greșeală");

        List<List<String>> sheet = xlsx(DepotReportKind.ISSUED_DOCUMENTS, null, null, null);

        assertThat(rowWith(sheet, number(first) + "")).contains("Borderou", "1");
        assertThat(rowWith(sheet, "greșeală")).contains("Borderou", "3", "Anulată");
        assertThat(rowStarting(sheet, "Borderou")).isNotNull();
        assertThat(rowsContaining(sheet, "Lipsește din serie")).singleElement()
                .satisfies(r -> assertThat(r).contains("Borderou", "2"));
    }

    // --- bani ---

    /** 2% AFM: aceeași sumă ca banda de pe ecran, pe toată firma chiar dacă e ales un depozit. */
    @Test
    void theAfmReportIsTheSameMoneyAsTheRetentionsAndCoversTheWholeCompany() {
        finalized(head(IN, depotA, DAY, partner, null), line(cardboard, "1000", "0.50"));
        finalized(head(IN, depotB, DAY, partner, null), line(cardboard, "3000", "0.50"));

        BigDecimal onScreen = operations.retentions(2026, 9).afmContribution();
        assertThat(onScreen).isEqualByComparingTo("40.00");
        assertThat(rowStarting(xlsx(DepotReportKind.AFM, depotB.getId(), null, null), "Total"))
                .contains("2000", "40");
    }

    /** D100 pe luna din interval, D205 cu beneficiarul și CNP-ul întreg. */
    @Test
    void theIncomeTaxReportHasTheMonthAndTheBeneficiaries() {
        finalized(head(IN, depotA, DAY, null, person, true), line(copper, "100", "20"));

        List<List<String>> sheet = xlsx(DepotReportKind.INCOME_TAX, null, null, null);

        assertThat(rowStarting(sheet, "09.2026")).contains("2000", "200", "25.10.2026");
        assertThat(rowWith(sheet, "Ion Popescu")).contains("1900101123457", "2000", "200");
    }

    /** Numerarul pe zi × persoană, cu depășirea plafonului de 10.000 lei marcată. */
    @Test
    void cashPaymentsAboveTheDailyLimitAreFlagged() {
        finalized(cash(head(IN, depotA, DAY, null, person)), line(cardboard, "15000", "0.50"));
        finalized(cash(head(IN, depotB, DAY, null, person)), line(cardboard, "10000", "0.50"));
        finalized(cash(head(IN, depotA, DAY.plusDays(1), null, person)), line(cardboard, "100", "0.50"));

        List<List<String>> sheet = xlsx(DepotReportKind.CASH_PF, null, null, null);

        assertThat(rowWith(sheet, "15.09.2026")).contains("Ion Popescu", "2", "12250", "DA");
        assertThat(rowWith(sheet, "16.09.2026")).contains("49").doesNotContain("DA");
    }

    // --- parteneri ---

    /** Borderourile PF și totalul pe persoană, cu CNP-ul întreg. */
    @Test
    void theIndividualsReportListsTheBorderousAndTheTotalsPerPerson() {
        finalized(head(IN, depotA, DAY, null, person, true), line(copper, "100", "20"));
        finalized(head(IN, depotA, DAY.plusDays(1), null, person), line(cardboard, "1000", "0.50"));

        List<List<String>> sheet = xlsx(DepotReportKind.INDIVIDUALS, null, null, null);

        assertThat(rowsContaining(sheet, "Ion Popescu")).hasSize(3);
        assertThat(rowStarting(sheet, "Ion Popescu")).contains("1900101123457", "2", "1100", "2500");
    }

    /** Fișa partenerului: impuritățile pe linie, iar prețul numai pentru cine îl vede. */
    @Test
    void thePartnerCardHidesThePricesFromWhoeverCannotSeeThem() {
        UUID id = operations.create(head(IN, depotA, DAY, partner, null)).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(new Line(cardboard.getId(),
                null, null, new BigDecimal("1000"), new BigDecimal("900"), new BigDecimal("0.50"), null, null))));
        operations.finalizeOperation(id);
        finalized(head(IN, depotA, DAY, recycler, null), line(cardboard, "5", "0.50"));

        List<List<String>> sheet = xlsx(DepotReportKind.PARTNER_CARD, null, null, partner.getId());
        assertThat(rowWith(sheet, number(id) + "")).contains("1000", "900", "100", "0.5", "450");
        // Totalul pe sortiment e doar al lui Alfa: cele 5 kg de la Beta nu intră.
        assertThat(rowStarting(sheet, "Intrare")).contains("Carton", "900", "100", "450");

        company.setPriceVisibility(PriceVisibility.NO_CONSULTANT);
        companyRepository.saveAndFlush(company);
        actAs(AppUser.builder().id(UUID.randomUUID()).email("consultant@demo.ro").role(Role.CONSULTANT)
                .enabled(true).build());
        List<List<String>> hidden = xlsx(DepotReportKind.PARTNER_CARD, null, null, partner.getId());
        assertThat(hidden.stream().flatMap(List::stream)).doesNotContain("450", "Valoare (lei)");
    }

    // --- acces, perioadă, formate ---

    /**
     * Operatorul restrâns la Turda: alt depozit dă 404, rapoartele cu bani și cel cu CNP-uri dau 403, iar registrele îi
     * arată doar Turda.
     */
    @Test
    void aRestrictedOperatorSeesOnlyItsDepotAndNoMoney() {
        finalized(head(IN, depotA, DAY, partner, null), line(cardboard, "111", "0.50"));
        finalized(head(IN, depotB, DAY, partner, null), line(cardboard, "222", "0.50"));
        AppUser operator = user(Role.OPERATOR);
        users.changeWorkPoints(operator.getId(), new UserWorkPointsRequest(false, List.of(depotB.getId())));
        actAs(operator);

        assertThatThrownBy(() -> render(DepotReportKind.SCALE_LOG, depotA.getId(), null, null, "xlsx"))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessageEnum.WORK_POINT_NOT_FOUND));
        for (DepotReportKind kind : List.of(DepotReportKind.AFM, DepotReportKind.INCOME_TAX,
                DepotReportKind.CASH_PF, DepotReportKind.INDIVIDUALS)) {
            assertThatThrownBy(() -> render(kind, null, null, null, "xlsx"))
                    .as(kind.name()).isInstanceOf(AccessDeniedException.class);
        }
        actAs(admin);
        operations.cancel(draft(head(IN, depotA, DAY, partner, null), line(cardboard, "1", "0.50")), "doar Baciu");
        assertThat(rowsContaining(xlsx(DepotReportKind.CANCELLED, depotB.getId(), null, null), "doar Baciu"))
                .as("depozitul ales").isEmpty();
        actAs(operator);
        assertThat(rowsContaining(xlsx(DepotReportKind.CANCELLED, null, null, null), "doar Baciu"))
                .as("depozitele operatorului").isEmpty();
        List<List<String>> log = xlsx(DepotReportKind.SCALE_LOG, null, null, null);
        assertThat(rowsContaining(log, "222")).isNotEmpty();
        assertThat(rowsContaining(log, "111")).isEmpty();
    }

    @Test
    void thePeriodIsCheckedAndOnlyFourReportsHaveAPdf() {
        assertThatThrownBy(() -> reports.render(DepotReportKind.SCALE_LOG, FROM, FROM.plusDays(366), null, null, null, "xlsx"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorMessageEnum.DEPOT_REPORT_PERIOD_INVALID));
        assertThatThrownBy(() -> reports.render(DepotReportKind.SCALE_LOG, TO, FROM, null, null, null, "xlsx"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> render(DepotReportKind.SCALE_LOG, null, null, null, "pdf"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorMessageEnum.DEPOT_REPORT_FORMAT_UNAVAILABLE));
        assertThatThrownBy(() -> render(DepotReportKind.PARTNER_CARD, null, null, null, "xlsx"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorMessageEnum.DEPOT_REPORT_PARTNER_REQUIRED));
        // Un an întreg încape (scurtătura „Anul”).
        render(DepotReportKind.SCALE_LOG, null, null, null, "xlsx", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
    }

    /** Cele patru cu PDF se deschid, cu titlul și subsolul programului; o perioadă goală dă „Nimic în perioadă”. */
    @Test
    void theFourPdfsOpenAndAnEmptyPeriodSaysSo() throws IOException {
        finalized(head(IN, depotA, DAY, null, person, true), line(copper, "100", "20"));
        for (DepotReportKind kind : List.of(DepotReportKind.STOCK_CARD, DepotReportKind.AFM,
                DepotReportKind.INCOME_TAX, DepotReportKind.INDIVIDUALS)) {
            byte[] pdf = render(kind, null, null, null, "pdf").body();
            assertThat(new String(pdf, 0, 5)).as(kind.name()).isEqualTo("%PDF-");
            var reader = new com.lowagie.text.pdf.PdfReader(pdf);
            String text = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1);
            reader.close();
            assertThat(text).as(kind.name()).contains("WasteHouse");
        }
        assertThat(rowsContaining(xlsx(DepotReportKind.CANCELLED, null, null, null), "Nimic în perioadă")).hasSize(1);
    }

    // ---------------------------------------------------------------------------------------------

    private DepotReportService.Rendered render(DepotReportKind kind, UUID workPointId, UUID articleId, UUID partnerId,
                                               String format) {
        return render(kind, workPointId, articleId, partnerId, format, FROM, TO);
    }

    private DepotReportService.Rendered render(DepotReportKind kind, UUID workPointId, UUID articleId, UUID partnerId,
                                               String format, LocalDate from, LocalDate to) {
        return reports.render(kind, from, to, workPointId, articleId, partnerId, format);
    }

    /** Foaia întâi, ca text: numerele ca în Excel fără separatorul de mii, datele cum sunt scrise. */
    private List<List<String>> xlsx(DepotReportKind kind, UUID workPointId, UUID articleId, UUID partnerId) {
        byte[] body = render(kind, workPointId, articleId, partnerId, "xlsx").body();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            Sheet sheet = wb.getSheetAt(0);
            List<List<String>> rows = new ArrayList<>();
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (Cell cell : row) {
                    cells.add(cell.getCellType() == CellType.NUMERIC
                            ? BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString()
                            : new DataFormatter().formatCellValue(cell));
                }
                rows.add(cells);
            }
            return rows;
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private static List<List<String>> rowsContaining(List<List<String>> sheet, String value) {
        return sheet.stream().filter(r -> r.stream().anyMatch(c -> c.contains(value))).toList();
    }

    private static List<String> rowWith(List<List<String>> sheet, String value) {
        return sheet.stream().filter(r -> r.contains(value)).findFirst()
                .orElseThrow(() -> new AssertionError("niciun rând cu „" + value + "”: " + sheet));
    }

    private static List<String> rowStarting(List<List<String>> sheet, String value) {
        return sheet.stream().filter(r -> !r.isEmpty() && r.get(0).equals(value)).findFirst()
                .orElseThrow(() -> new AssertionError("niciun rând care începe cu „" + value + "”: " + sheet));
    }

    private String number(UUID operationId) {
        return String.valueOf(operationRepository.findById(operationId).orElseThrow().getNumber());
    }

    private UUID finalized(WeighingOperationRequest head, Line... lines) {
        UUID id = draft(head, lines);
        operations.finalizeOperation(id);
        return id;
    }

    private UUID draft(WeighingOperationRequest head, Line... lines) {
        UUID id = operations.create(head).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(lines)));
        return id;
    }

    private WeighingOperationRequest head(WeighingOperationType type, WorkPoint depot, LocalDate date, Partner partner,
                                          NaturalPerson person) {
        return head(type, depot, date, partner, person, null);
    }

    private WeighingOperationRequest head(WeighingOperationType type, WorkPoint depot, LocalDate date, Partner partner,
                                          NaturalPerson person, Boolean ownHousehold) {
        return new WeighingOperationRequest(type, depot.getId(), date, partner == null ? null : partner.getId(),
                person == null ? null : person.getId(), null, null, null, null, null, null, null, null, ownHousehold,
                null, null, null);
    }

    private WeighingOperationRequest transfer(LocalDate date) {
        return new WeighingOperationRequest(TRANSFER, depotA.getId(), date, null, null, null, null, null, null, null,
                null, null, null, null, null, null, depotB.getId());
    }

    private static WeighingOperationRequest cash(WeighingOperationRequest h) {
        return new WeighingOperationRequest(h.type(), h.workPointId(), h.date(), h.partnerId(), h.naturalPersonId(),
                h.origin(), h.driverId(), h.vehicleId(), h.driverName(), h.vehicleRegistration(), h.orderNumber(),
                PaymentMethod.NUMERAR, "CH 1", h.ownHousehold(), h.notes(), h.scaleId(), h.targetWorkPointId());
    }

    private static Line line(WasteArticle article, String kg, String price) {
        return new Line(article.getId(), null, null, new BigDecimal(kg), null, new BigDecimal(price), null, null);
    }

    private static Line outLine(WasteArticle article, String kg) {
        return new Line(article.getId(), null, null, new BigDecimal(kg), null, null, WasteOperationCode.R3, null);
    }

    /** O linie de transfer: fără preț și fără cod R/D (nu e valorificare). */
    private static Line moved(WasteArticle article, String kg) {
        return new Line(article.getId(), null, null, new BigDecimal(kg), null, null, null, null);
    }

    private WasteArticle article(String name, ro.ecoregistru.entity.WasteCode code, boolean metal) {
        return articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(code).name(name).metal(metal).active(true).createdAt(Instant.now()).build());
    }

    private Partner partner(String name, PartnerType type) {
        return partnerRepository.save(Partner.builder()
                .company(company).name(name + " SRL").cui("RO" + suffix())
                .type(type).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
    }

    private WorkPoint depot(String name) {
        return workPointRepository.save(WorkPoint.builder()
                .company(company).name(name + " " + suffix()).active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + suffix() + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private void actAs(AppUser user) {
        TenantContext.set(company.getId());
        AppUser fresh = user.getCompany() == null ? user : appUserRepository.findById(user.getId()).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(fresh, null, List.of()));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
