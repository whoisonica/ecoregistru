package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.InventoryDeclarationRequest;
import ro.ecoregistru.controller.request.InventoryHeaderRequest;
import ro.ecoregistru.controller.request.InventoryLinesRequest;
import ro.ecoregistru.controller.request.UserWorkPointsRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.InventoryResponse;
import ro.ecoregistru.controller.response.StockResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.KeeperDeclaration;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.CountMethod;
import ro.ecoregistru.enums.InventoryKind;
import ro.ecoregistru.enums.InventoryStatus;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.ShortageNature;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.CompanyUserService;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.InventoryService;
import ro.ecoregistru.service.StockService;
import ro.ecoregistru.service.WeighingOperationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D3.5 — inventarul pe depozit după Normele OMFP 2861/2009: decizia (pct. 6), declarația gestionarului (pct. 8
 * lit. a)), scripticul fotografiat la începutul zilei de început (pct. 1, 8 lit. d)), faptic cu metodă (pct. 15),
 * explicația fiecărei diferențe (pct. 39), natura lipsei, PV-ul închis, aprobarea care scrie ajustările datate la data
 * de referință (OMFP 1802/2014 pct. 95). O operațiune retroactivă după fotografie oprește închiderea și aprobarea.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class InventoryIT {

    @Autowired InventoryService inventories;
    @Autowired ro.ecoregistru.service.StockOpeningService openings;
    @Autowired StockService stock;
    @Autowired WeighingOperationService operations;
    @Autowired CompanyUserService users;
    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;

    LocalDate today;
    LocalDate start;
    Company company;
    AppUser admin;
    WorkPoint depotA;
    WorkPoint depotB;
    Partner partner;
    WasteArticle cardboard;
    WasteCode paper;

    @BeforeEach
    void setUp() {
        today = DeadlineService.today();
        start = today.minusDays(2);
        company = companyRepository.save(Company.builder()
                .name("Inventar " + suffix() + " SRL").cui("ROV" + suffix()).type(CompanyType.COLLECTOR)
                .environmentalAuthNumber("AM-" + suffix()).active(true).createdAt(Instant.now()).build());
        admin = user(Role.ADMIN);
        depotA = depot("Baciu");
        depotB = depot("Turda");
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin " + suffix() + " SRL").cui("RO" + suffix())
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        cardboard = articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(code("15 01 01")).name("Carton").active(true).createdAt(Instant.now()).build());
        paper = code("20 01 01");
        actAs(admin);
        finalizeIn(depotA, start.minusDays(1), "1000");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void openSnapshotsBook() {
        finalizeIn(depotA, start, "200"); // în ziua de început: după numărătoare, nu în scriptic

        InventoryResponse inv = inventories.open(header(depotA, InventoryKind.ANNUAL, null, members(1)));

        assertThat(inv.status()).isEqualTo(InventoryStatus.OPEN);
        assertThat(inv.number()).isEqualTo(1);
        assertThat(inv.lines()).singleElement().satisfies(l -> {
            assertThat(l.articleId()).isEqualTo(cardboard.getId());
            assertThat(l.bookKg()).isEqualByComparingTo("1000");
            assertThat(l.countedKg()).isNull();
        });
        assertThat(inv.lastEntryDoc()).contains("Intrare nr.").contains(start.minusDays(1).format(
                java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")));
    }

    @Test
    void oneActivePerDepot() {
        inventories.open(header(depotA, InventoryKind.ANNUAL, null, members(1)));
        assertBusiness(() -> inventories.open(header(depotA, InventoryKind.ANNUAL, null, members(1))),
                ErrorMessageEnum.INVENTORY_ALREADY_ACTIVE);
        assertThat(inventories.open(header(depotB, InventoryKind.ANNUAL, null, members(1))).number()).isEqualTo(2);
    }

    @Test
    void exactlyOnePresident() {
        List<InventoryHeaderRequest.Member> none = List.of(new InventoryHeaderRequest.Member("Ana", "economist", false));
        List<InventoryHeaderRequest.Member> two = List.of(new InventoryHeaderRequest.Member("Ana", "economist", true),
                new InventoryHeaderRequest.Member("Dan", "șef", true));
        assertBusiness(() -> inventories.open(header(depotA, InventoryKind.ANNUAL, null, none)),
                ErrorMessageEnum.INVENTORY_PRESIDENT_REQUIRED);
        assertBusiness(() -> inventories.open(header(depotA, InventoryKind.ANNUAL, null, two)),
                ErrorMessageEnum.INVENTORY_PRESIDENT_REQUIRED);
    }

    @Test
    void receivingKeeperOnlyOnHandover() {
        assertBusiness(() -> inventories.open(header(depotA, InventoryKind.ANNUAL, "Ion Primitor", members(1))),
                ErrorMessageEnum.INVENTORY_RECEIVING_KEEPER_ONLY_ON_HANDOVER);
        assertThat(inventories.open(header(depotA, InventoryKind.HANDOVER, "Ion Primitor", members(1)))
                .receivingKeeperName()).isEqualTo("Ion Primitor");
    }

    @Test
    void countNeedsDeclaration() {
        InventoryResponse inv = inventories.open(header(depotA, InventoryKind.ANNUAL, null, members(1)));
        assertBusiness(() -> inventories.saveLines(inv.id(), lines(inv, "1000", CountMethod.WEIGHED, null, null, null)),
                ErrorMessageEnum.INVENTORY_DECLARATION_REQUIRED);
    }

    @Test
    void closeNeedsEveryCount() {
        InventoryResponse inv = declared();
        assertBusiness(() -> inventories.close(inv.id()), ErrorMessageEnum.INVENTORY_COUNT_MISSING);
    }

    @Test
    void closeNeedsExplanationOnDifference() {
        InventoryResponse inv = declared();
        inventories.saveLines(inv.id(), lines(inv, "1030", CountMethod.WEIGHED, null, null, null));
        assertBusiness(() -> inventories.close(inv.id()), ErrorMessageEnum.INVENTORY_EXPLANATION_REQUIRED);
    }

    @Test
    void closeNeedsNatureOnShortage() {
        InventoryResponse inv = declared();
        inventories.saveLines(inv.id(), lines(inv, "980", CountMethod.WEIGHED, null, "uscare", null));
        assertBusiness(() -> inventories.close(inv.id()), ErrorMessageEnum.INVENTORY_NATURE_REQUIRED);
    }

    @Test
    void technicalNeedsData() {
        InventoryResponse inv = declared();
        inventories.saveLines(inv.id(), lines(inv, "1000", CountMethod.TECHNICAL, null, null, null));
        assertBusiness(() -> inventories.close(inv.id()), ErrorMessageEnum.INVENTORY_TECHNICAL_DATA_REQUIRED);
        inventories.saveLines(inv.id(), lines(inv, "1000", null, null, null, null));
        assertBusiness(() -> inventories.close(inv.id()), ErrorMessageEnum.INVENTORY_METHOD_REQUIRED);
    }

    @Test
    void closeRefusesWhenBookChanged() {
        InventoryResponse inv = counted("1000", null, null);
        finalizeIn(depotA, start.minusDays(3), "100"); // retroactiv, după fotografie

        assertBusiness(() -> inventories.close(inv.id()), ErrorMessageEnum.INVENTORY_BOOK_CHANGED);
        assertThat(inventories.get(inv.id()).warnings()).contains("BOOK_CHANGED");
    }

    @Test
    void approveRefusesWhenBookChanged() {
        InventoryResponse inv = counted("1000", null, null);
        inventories.close(inv.id());
        finalizeIn(depotA, start.minusDays(3), "100");

        assertBusiness(() -> inventories.approve(inv.id()), ErrorMessageEnum.INVENTORY_BOOK_CHANGED);
    }

    @Test
    void reopenAndRecalculate() {
        InventoryResponse inv = counted("1030", "cântar", null);
        inventories.close(inv.id());
        finalizeIn(depotA, start.minusDays(3), "100");

        assertThat(inventories.reopen(inv.id()).status()).isEqualTo(InventoryStatus.OPEN);
        InventoryResponse recalculated = inventories.recalculate(inv.id());

        assertThat(recalculated.lines()).singleElement().satisfies(l -> {
            assertThat(l.bookKg()).isEqualByComparingTo("1100");
            assertThat(l.explanation()).isNull();
        });
        assertThat(recalculated.warnings()).doesNotContain("BOOK_CHANGED");
    }

    @Test
    void approveWritesAdjustments() {
        finalizeIn(depotA, start, "200");
        InventoryResponse inv = declared();
        List<InventoryLinesRequest.Line> rows = new ArrayList<>(lines(inv, "980", CountMethod.WEIGHED, null,
                "uscare", ShortageNature.NON_IMPUTABLE).lines());
        rows.add(new InventoryLinesRequest.Line(null, null, paper.getId(), new BigDecimal("50"), CountMethod.TECHNICAL,
                "grămadă 2 × 1,5 × 1 m, 16,7 kg/m³", "găsit fără document", null, null, false));
        inventories.saveLines(inv.id(), new InventoryLinesRequest(rows));
        inventories.close(inv.id());

        InventoryResponse approved = inventories.approve(inv.id());

        assertThat(approved.status()).isEqualTo(InventoryStatus.APPROVED);
        assertThat(approved.approvedOn()).isEqualTo(today);
        var adjustments = movementRepository.findStockOnlyBetween(company.getId(), depotA.getId(), start, start);
        assertThat(adjustments).extracting(m -> m.getOperation() + " " + m.getQuantity().stripTrailingZeros().toPlainString())
                .containsExactlyInAnyOrder("INVENTORY_SHORTAGE 20", "INVENTORY_SURPLUS 50");
        assertThat(adjustments).allSatisfy(m -> assertThat(m.getInventory().getId()).isEqualTo(inv.id()));
        assertThat(kg(stock.stock(depotA.getId(), today), cardboard.getWasteCode())).isEqualByComparingTo("1180");
        assertThat(kg(stock.stock(depotA.getId(), today), paper)).isEqualByComparingTo("50");
    }

    @Test
    void approvedIsFinal() {
        InventoryResponse inv = counted("1000", null, null);
        inventories.close(inv.id());
        inventories.approve(inv.id());

        assertBusiness(() -> inventories.cancel(inv.id(), "greșeală"), ErrorMessageEnum.INVENTORY_APPROVED_FINAL);
        assertBusiness(() -> inventories.reopen(inv.id()), ErrorMessageEnum.INVENTORY_APPROVED_FINAL);
        assertBusiness(() -> inventories.saveLines(inv.id(), lines(inv, "1000", CountMethod.WEIGHED, null, null, null)),
                ErrorMessageEnum.INVENTORY_APPROVED_FINAL);
        assertBusiness(() -> inventories.approve(inv.id()), ErrorMessageEnum.INVENTORY_APPROVED_FINAL);
    }

    @Test
    void closedMustBeReopenedBeforeEditing() {
        InventoryResponse inv = counted("1000", null, null);
        inventories.close(inv.id());
        assertBusiness(() -> inventories.saveLines(inv.id(), lines(inv, "990", CountMethod.WEIGHED, null, null, null)),
                ErrorMessageEnum.INVENTORY_NOT_OPEN);
        assertBusiness(() -> inventories.recalculate(inv.id()), ErrorMessageEnum.INVENTORY_NOT_OPEN);
    }

    @Test
    void cancelNeedsReason() {
        InventoryResponse inv = inventories.open(header(depotA, InventoryKind.ANNUAL, null, members(1)));
        assertBusiness(() -> inventories.cancel(inv.id(), " "), ErrorMessageEnum.INVENTORY_CANCEL_REASON_REQUIRED);
        assertThat(inventories.cancel(inv.id(), "decizie greșită").status()).isEqualTo(InventoryStatus.CANCELLED);
        assertThat(inventories.open(header(depotA, InventoryKind.ANNUAL, null, members(1))).status())
                .isEqualTo(InventoryStatus.OPEN);
    }

    @Test
    void keeperInCommissionWarnsAndThirdPartyGoodsWarn() {
        InventoryResponse inv = inventories.open(header(depotA, InventoryKind.ANNUAL, null, List.of(
                new InventoryHeaderRequest.Member(" ion gestionar ", "administrator", true))));
        assertThat(inv.warnings()).contains("KEEPER_IN_COMMISSION");

        List<KeeperDeclaration.Answer> answers = new ArrayList<>(Collections.nCopies(7, new KeeperDeclaration.Answer(false, null)));
        answers.set(KeeperDeclaration.THIRD_PARTY_GOODS, new KeeperDeclaration.Answer(true, "10 t carton în custodie"));
        InventoryResponse declared = inventories.saveDeclaration(inv.id(), new InventoryDeclarationRequest(answers,
                inv.lastEntryDoc(), inv.lastExitDoc(), start));
        assertThat(declared.warnings()).contains("THIRD_PARTY_GOODS");
    }

    @Test
    void declarationNeedsSevenAnswers() {
        InventoryResponse inv = inventories.open(header(depotA, InventoryKind.ANNUAL, null, members(1)));
        assertBusiness(() -> inventories.saveDeclaration(inv.id(), new InventoryDeclarationRequest(
                List.of(new KeeperDeclaration.Answer(false, null)), null, null, start)),
                ErrorMessageEnum.INVENTORY_DECLARATION_INCOMPLETE);
    }

    @Test
    void operatorForbidden() throws Exception {
        AppUser operator = user(Role.OPERATOR);
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        String body = "{\"workPointId\":\"" + depotA.getId() + "\",\"kind\":\"ANNUAL\",\"startsOn\":\"" + start
                + "\",\"endsOn\":\"" + start + "\",\"keeperName\":\"G\",\"commission\":[{\"name\":\"P\",\"president\":true}]}";
        mockMvc.perform(post("/api/v1/inventories").header("Authorization", "Bearer " + jwtService.generateToken(operator))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/inventories").header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void otherDepotIs404() {
        UUID id = inventories.open(header(depotB, InventoryKind.ANNUAL, null, members(1))).id();
        AppUser operator = user(Role.OPERATOR);
        users.changeWorkPoints(operator.getId(), new UserWorkPointsRequest(false, List.of(depotA.getId())));
        actAs(operator);

        assertThatThrownBy(() -> inventories.get(id)).isInstanceOf(NotFoundException.class);
        assertThat(inventories.list(null)).extracting(InventoryResponse::id).doesNotContain(id);
    }

    @Test
    void operationsDuringListed() {
        finalizeIn(depotA, start, "200");
        InventoryResponse inv = inventories.open(header(depotA, InventoryKind.ANNUAL, null, members(1)));

        assertThat(inventories.operationsDuring(inv.id())).singleElement()
                .satisfies(o -> assertThat(o.date()).isEqualTo(start));
    }

    /** C1 — nota de preluare cu tăierea în ziua de început e stocul dimineții: intră în scriptic, nu devine plus. */
    @Test
    void openingOnStartDayIsInTheBook() {
        openings.confirm(openings.create(openingRequest(depotB, start, "500")).id());

        InventoryResponse inv = inventories.open(header(depotB, InventoryKind.ANNUAL, null, members(1)));

        assertThat(inv.lines()).singleElement().satisfies(l -> assertThat(l.bookKg()).isEqualByComparingTo("500"));
    }

    /** C1 — inventarul de corectură din aceeași zi pornește de la faptic-ul celui aprobat, nu îl mai adună o dată. */
    @Test
    void correctionInventoryOnSameDaySeesTheFirstOne() {
        InventoryResponse first = counted("980", "uscare", ShortageNature.NON_IMPUTABLE);
        inventories.close(first.id());
        inventories.approve(first.id());

        InventoryResponse second = inventories.open(header(depotA, InventoryKind.OTHER, null, members(1)));

        assertThat(second.lines()).singleElement().satisfies(l -> assertThat(l.bookKg()).isEqualByComparingTo("980"));
    }

    /** I1 — un inventar datat înaintea unuia aprobat i-ar schimba retroactiv scripticul. */
    @Test
    void inventoryBeforeApprovedRefused() {
        InventoryResponse first = counted("1000", null, null);
        inventories.close(first.id());
        inventories.approve(first.id());
        InventoryHeaderRequest h = header(depotA, InventoryKind.OTHER, null, members(1));

        assertBusiness(() -> inventories.open(new InventoryHeaderRequest(h.workPointId(), h.decisionNumber(),
                h.decisionDate(), h.kind(), false, h.mode(), h.method(), start.minusDays(1), start, h.commission(),
                h.keeperName(), null, null)), ErrorMessageEnum.INVENTORY_BEFORE_APPROVED);
    }

    /** I1 — un inventar datat înaintea tăierii notei de preluare n-ar vedea soldul preluat. */
    @Test
    void inventoryBeforeOpeningRefused() {
        openings.confirm(openings.create(openingRequest(depotB, start, "500")).id());
        InventoryHeaderRequest h = header(depotB, InventoryKind.ANNUAL, null, members(1));

        assertBusiness(() -> inventories.open(new InventoryHeaderRequest(h.workPointId(), h.decisionNumber(),
                h.decisionDate(), h.kind(), false, h.mode(), h.method(), start.minusDays(1), start, h.commission(),
                h.keeperName(), null, null)), ErrorMessageEnum.INVENTORY_BEFORE_OPENING);
    }

    private ro.ecoregistru.controller.request.StockOpeningRequest openingRequest(WorkPoint depot, LocalDate cutOff, String kg) {
        return new ro.ecoregistru.controller.request.StockOpeningRequest(depot.getId(), cutOff,
                ro.ecoregistru.enums.StockOpeningSource.STOCK_CARDS, "Ion Gestionar", "Ana Contabil", null,
                List.of(new ro.ecoregistru.controller.request.StockOpeningRequest.Line(cardboard.getId(), null,
                        new BigDecimal(kg))));
    }

    // --- helpers ---

    private InventoryResponse declared() {
        InventoryResponse inv = inventories.open(header(depotA, InventoryKind.ANNUAL, null, members(1)));
        return inventories.saveDeclaration(inv.id(), new InventoryDeclarationRequest(
                Collections.nCopies(7, new KeeperDeclaration.Answer(false, null)), inv.lastEntryDoc(), inv.lastExitDoc(),
                start));
    }

    private InventoryResponse counted(String kg, String explanation, ShortageNature nature) {
        InventoryResponse inv = declared();
        return inventories.saveLines(inv.id(), lines(inv, kg, CountMethod.WEIGHED, null, explanation, nature));
    }

    private InventoryLinesRequest lines(InventoryResponse inv, String kg, CountMethod method, String technical,
                                        String explanation, ShortageNature nature) {
        return new InventoryLinesRequest(inv.lines().stream().map(l -> new InventoryLinesRequest.Line(l.id(),
                l.articleId(), l.wasteCodeId(), new BigDecimal(kg), method, technical, explanation, nature, null, false))
                .toList());
    }

    private InventoryHeaderRequest header(WorkPoint depot, InventoryKind kind, String receivingKeeper,
                                          List<InventoryHeaderRequest.Member> commission) {
        return new InventoryHeaderRequest(depot.getId(), "D" + suffix().substring(0, 3), start.minusDays(5), kind, false,
                "inventariere integrală", "cântărire", start, start.plusDays(1), commission, "Ion Gestionar",
                receivingKeeper, null);
    }

    private List<InventoryHeaderRequest.Member> members(int n) {
        List<InventoryHeaderRequest.Member> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new InventoryHeaderRequest.Member("Membru " + i, "economist", i == 0));
        }
        return list;
    }

    private void finalizeIn(WorkPoint depot, LocalDate date, String kg) {
        UUID id = operations.create(new WeighingOperationRequest(WeighingOperationType.IN, depot.getId(), date,
                partner.getId(), null, null, null, null, null, null, null, null, null, null, null, null, null)).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(new WeighingLinesRequest.Line(
                cardboard.getId(), null, null, new BigDecimal(kg), null, null, null, null))));
        operations.finalizeOperation(id);
    }

    private BigDecimal kg(StockResponse response, WasteCode code) {
        return response.rows().stream().filter(r -> code.getId().equals(r.wasteCodeId()))
                .map(StockResponse.Row::stockKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private WasteCode code(String prefix) {
        return wasteCodeRepository.findAll().stream().filter(c -> c.getCode().startsWith(prefix)).findFirst().orElseThrow();
    }

    private static void assertBusiness(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorMessageEnum code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }

    private WorkPoint depot(String name) {
        return workPointRepository.save(WorkPoint.builder()
                .company(company).name(name + " " + suffix()).active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Role role) {
        return appUserRepository.save(AppUser.builder()
                .email("inventar+" + suffix() + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private void actAs(AppUser user) {
        TenantContext.set(company.getId());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                appUserRepository.findById(user.getId()).orElseThrow(), null, List.of()));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
