package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.controller.request.WasteMovementRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.StockResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.MovementDirection;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.Art48RegisterService;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.MovementQueryService;
import ro.ecoregistru.service.PackagingService;
import ro.ecoregistru.service.StockService;
import ro.ecoregistru.service.WasteMovementService;
import ro.ecoregistru.service.WeighingOperationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * D3.5 — liniile care mișcă doar stocul ({@code OPENING_BALANCE}, {@code INVENTORY_SURPLUS/SHORTAGE}) nu sunt intrări
 * sau ieșiri. Testul fotografiază tot ce citește mișcări ca registru (listele Intrări/Ieșiri și totalurile lor, rezumatul
 * lunii de pe Acasă, ambalajele, intrările cronologice și Cap. 2 art. 48, interogările termenelor), scrie cele trei
 * linii și cere aceeași fotografie. Stocul, în schimb, le numără.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class InventoryInvariantIT {

    @Autowired StockService stock;
    @Autowired WeighingOperationService operations;
    @Autowired MovementQueryService movementQuery;
    @Autowired WasteMovementService movementService;
    @Autowired PackagingService packaging;
    @Autowired Art48RegisterService art48;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired ro.ecoregistru.service.StockSettingsService stockSettings;

    LocalDate today;
    Company company;
    AppUser admin;
    WorkPoint depot;
    Partner partner;
    WasteCode cardboardCode;
    WasteArticle cardboard;

    @BeforeEach
    void setUp() {
        today = DeadlineService.today();
        company = companyRepository.save(Company.builder()
                .name("Inventar " + suffix() + " SRL").cui("ROI" + suffix()).type(CompanyType.COLLECTOR)
                .environmentalAuthNumber("AM-" + suffix()).active(true).createdAt(Instant.now()).build());
        admin = appUserRepository.save(AppUser.builder()
                .email("inv+" + suffix() + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Baciu " + suffix()).active(true).createdAt(Instant.now()).build());
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin " + suffix() + " SRL").cui("RO" + suffix())
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        cardboardCode = wasteCodeRepository.findAll().stream()
                .filter(c -> c.getCode().startsWith("15 01 01")).findFirst().orElseThrow();
        cardboard = articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(cardboardCode).name("Carton").active(true).createdAt(Instant.now()).build());
        TenantContext.set(company.getId());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                appUserRepository.findById(admin.getId()).orElseThrow(), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void registersUntouchedByStockOnlyLines() {
        operations.finalizeOperation(weighed(WeighingOperationType.IN, "1000", null));
        operations.finalizeOperation(weighed(WeighingOperationType.OUT, "400", WasteOperationCode.R3));
        stockSettings.addLimit(new ro.ecoregistru.controller.request.AuthorizedLimitRequest(depot.getId(), "OUTPUT",
                null, new BigDecimal("100"), "T", "YEAR", null, false, null));
        List<String> before = snapshot();

        writeStockOnlyLines();

        assertThat(snapshot()).containsExactlyElementsOf(before);
    }

    @Test
    void stockOnlyLinesMoveStock() {
        operations.finalizeOperation(weighed(WeighingOperationType.IN, "1000", null));
        operations.finalizeOperation(weighed(WeighingOperationType.OUT, "400", WasteOperationCode.R3));

        writeStockOnlyLines();

        // 1000 − 400 + 500 (preluat) + 30 (plus) − 20 (minus)
        assertThat(kg(stock.stock(depot.getId(), today))).isEqualByComparingTo("1110");
        assertThat(kg(stock.stock(null, today))).isEqualByComparingTo("1110");
    }

    @Test
    void fifoTreatsOpeningAsLot() {
        UUID opening = openingStub();
        line("OPENING_BALANCE", "500", today.minusDays(40), opening, null);

        StockResponse.Row row = stock.stock(depot.getId(), today).rows().stream()
                .filter(r -> cardboardCode.getId().equals(r.wasteCodeId())).findFirst().orElseThrow();
        assertThat(row.stockKg()).isEqualByComparingTo("500");
        assertThat(row.oldestDays()).isEqualTo(40);
    }

    @Test
    void adjustmentLineNotEditable() {
        UUID id = line("INVENTORY_SURPLUS", "30", today, null, inventoryStub());

        assertThatThrownBy(() -> movementService.delete(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorMessageEnum.STOCK_ADJUSTMENT_THROUGH_DOCUMENT);
        assertThatThrownBy(() -> movementService.update(id, (WasteMovementRequest) null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorMessageEnum.STOCK_ADJUSTMENT_THROUGH_DOCUMENT);
    }

    // --- the snapshot: everything that reads movements as a register ---

    private List<String> snapshot() {
        int year = today.getYear();
        List<String> s = new ArrayList<>();
        for (UUID wp : new UUID[]{null, depot.getId()}) {
            for (MovementDirection direction : new MovementDirection[]{null, MovementDirection.IN, MovementDirection.OUT}) {
                s.add("list " + wp + " " + direction + " " + movementQuery.list(year, null, wp, null, false, false,
                        false, WasteRegister.ART_48, direction, null, null, 0, 100, "date", true).totalElements());
                s.add("totals " + wp + " " + direction + " " + movementQuery.totals(year, null, wp, null, false,
                        false, false, WasteRegister.ART_48, direction, null));
            }
            var register = art48.build(year, wp);
            s.add("art48 entries " + wp + " " + register.entries());
            s.add("art48 recovery " + wp + " " + register.recovery() + " " + register.disposal());
        }
        s.add("summary " + movementQuery.summary(year, today.getMonthValue()));
        // Limita de ieșiri pe an (D3.4): o lipsă la inventar nu e o ieșire din instalație (art. 34 alin. (2) lit. d)).
        s.add("output limit " + stock.stock(depot.getId(), today).limits().stream()
                .filter(l -> "OUTPUT".equals(l.kind())).map(StockResponse.Limit::usedKg).toList());
        s.add("packaging movements " + packaging.movements(year).size());
        s.add("packaging handovers " + packaging.handovers(year));
        s.add("packaging unclassified " + packaging.unclassified(year));
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        s.add("counted " + movementRepository.countCountedBetween(company.getId(), from, to));
        s.add("counted rows " + movementRepository.findCountedBetween(company.getId(), from, to).size());
        s.add("counted all " + movementRepository.findCounted(company.getId()).size());
        var totals = movementRepository.summarise(company.getId(), from, to);
        s.add("summarise " + totals.getMovements() + " " + totals.getQuantityKg());
        s.add("codes " + movementRepository.findDistinctWasteCodes(company.getId(), from, to));
        s.add("mirror " + movementRepository.countUnprovenMirrorClassifications(company.getId(), from, to));
        return s;
    }

    // --- helpers ---

    private void writeStockOnlyLines() {
        line("OPENING_BALANCE", "500", today.minusDays(3), openingStub(), null);
        UUID inventory = inventoryStub();
        line("INVENTORY_SURPLUS", "30", today, null, inventory);
        line("INVENTORY_SHORTAGE", "20", today, null, inventory);
    }

    private UUID weighed(WeighingOperationType type, String kg, WasteOperationCode code) {
        UUID id = operations.create(new WeighingOperationRequest(type, depot.getId(), today.minusDays(5),
                partner.getId(), null, null, null, null, null, null, null, null, null, null, null, null, null)).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(new WeighingLinesRequest.Line(
                cardboard.getId(), null, null, new BigDecimal(kg), null, null, code, null))));
        return id;
    }

    private BigDecimal kg(StockResponse response) {
        return response.rows().stream().filter(r -> cardboardCode.getId().equals(r.wasteCodeId()))
                .map(StockResponse.Row::stockKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private UUID openingStub() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into stock_openings (id, company_id, work_point_id, cut_off_date, source, status,
                                            created_at, updated_at, version)
                values (?, ?, ?, current_date, 'STOCK_CARDS', 'DRAFT', now(), now(), 0)
                """, id, company.getId(), depot.getId());
        return id;
    }

    private UUID inventoryStub() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into inventories (id, company_id, work_point_id, number, kind, starts_on, ends_on, keeper_name,
                                         status, closed_on, approved_on, created_at, updated_at, version)
                values (?, ?, ?, (select coalesce(max(number), 0) + 1 from inventories where company_id = ?), 'ANNUAL',
                        current_date, current_date, 'Gestionar', 'APPROVED', current_date, current_date, now(), now(), 0)
                """, id, company.getId(), depot.getId(), company.getId());
        return id;
    }

    private UUID line(String operation, String kg, LocalDate date, UUID opening, UUID inventory) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into waste_movements
                    (id, company_id, work_point_id, date, waste_code_id, article_id, quantity, weighed_at_unloading,
                     unit, operation, register, deleted, created_by, created_at, updated_at, stock_opening_id,
                     inventory_id, version)
                values (?, ?, ?, ?, ?, ?, ?::numeric, false, 'KG', ?, 'ART_48', false, ?, now(), now(), ?, ?, 0)
                """, id, company.getId(), depot.getId(), date, cardboardCode.getId(), cardboard.getId(), kg, operation,
                admin.getId(), opening, inventory);
        return id;
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
