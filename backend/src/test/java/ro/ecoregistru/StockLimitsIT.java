package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.AuthorizedLimitRequest;
import ro.ecoregistru.controller.request.StockThresholdRequest;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.service.StockSettingsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.controller.request.TransferReceiptRequest;
import ro.ecoregistru.controller.request.UserWorkPointsRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.StockResponse;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.CompanyUserService;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.StockService;
import ro.ecoregistru.service.WeighingOperationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * F3 (D3.3, D3.4) — pragurile pe sortiment × depozit (sub minim / peste maxim), vechimea stocului pe loturi (FIFO) cu
 * plafonul legal sau durata din autorizație, și limitele din autorizație comparate cu stocul și cu ieșirile anului.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class StockLimitsIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired StockService stock;
    @Autowired StockSettingsService settings;
    @Autowired WeighingOperationService operations;
    @Autowired CompanyUserService users;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;

    LocalDate today;
    Company company;
    AppUser admin;
    WorkPoint depotA;
    WorkPoint depotB;
    Partner partner;
    WasteArticle cardboard;

    @BeforeEach
    void setUp() {
        today = DeadlineService.today();
        company = companyRepository.save(Company.builder()
                .name("Limite " + suffix() + " SRL").cui("ROL" + suffix()).type(CompanyType.COLLECTOR)
                .environmentalAuthNumber("AM-" + suffix()).active(true).createdAt(Instant.now()).build());
        admin = user(Role.ADMIN);
        depotA = depot("Baciu");
        depotB = depot("Turda");
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin " + suffix() + " SRL").cui("RO" + suffix())
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        cardboard = articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findAll().stream().filter(c -> !c.isHazardous())
                        .findFirst().orElseThrow())
                .name("Carton").active(true).createdAt(Instant.now()).build());
        actAs(admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void thresholdsFlagBelowMinimumAndAboveMaximumAndASaveReplacesTheOldOne() {
        operations.finalizeOperation(weighed(WeighingOperationType.IN, depotA, today, "50", null));
        settings.saveThreshold(new StockThresholdRequest(depotA.getId(), cardboard.getId(), new BigDecimal("100"), new BigDecimal("500")));
        assertThat(row(stock.stock(depotA.getId(), today))).satisfies(r -> {
            assertThat(r.belowMin()).isTrue();
            assertThat(r.aboveMax()).isFalse();
        });
        operations.finalizeOperation(weighed(WeighingOperationType.IN, depotA, today, "600", null));
        assertThat(row(stock.stock(depotA.getId(), today)).aboveMax()).isTrue();

        settings.saveThreshold(new StockThresholdRequest(depotA.getId(), cardboard.getId(), null, new BigDecimal("1000")));
        assertThat(settings.thresholds(depotA.getId())).singleElement().satisfies(t -> {
            assertThat(t.minKg()).isNull();
            assertThat(t.maxKg()).isEqualByComparingTo("1000");
        });
        assertThat(row(stock.stock(depotA.getId(), today)).aboveMax()).isFalse();
        // Pragul e al depozitului: pe firmă nu se aplică.
        assertThat(row(stock.stock(null, today)).maxKg()).isNull();

        assertBusiness(() -> settings.saveThreshold(new StockThresholdRequest(depotA.getId(), cardboard.getId(), null, null)),
                ErrorMessageEnum.STOCK_THRESHOLD_EMPTY);
        assertBusiness(() -> settings.saveThreshold(new StockThresholdRequest(depotA.getId(), cardboard.getId(),
                new BigDecimal("10"), new BigDecimal("5"))), ErrorMessageEnum.STOCK_THRESHOLD_INVALID);
    }

    @Test
    void theOldestLotLeavesFirstAndItsAgeIsFlaggedAgainstTheLawOrTheAuthorization() {
        operations.finalizeOperation(weighed(WeighingOperationType.IN, depotA, today.minusDays(400), "100", null));
        operations.finalizeOperation(weighed(WeighingOperationType.IN, depotA, today.minusDays(10), "100", null));
        UUID out = weighed(WeighingOperationType.OUT, depotA, today, "50", WasteOperationCode.R3);
        operations.finalizeOperation(out);
        // 50 din lotul de acum 400 de zile au plecat; restul lui e încă aici.
        assertThat(row(stock.stock(depotA.getId(), today))).satisfies(r -> {
            assertThat(r.oldestDays()).isEqualTo(400);
            assertThat(r.ageFlag()).isEqualTo("ONE_YEAR");
        });
        operations.finalizeOperation(weighed(WeighingOperationType.OUT, depotA, today, "50", WasteOperationCode.R3));
        assertThat(row(stock.stock(depotA.getId(), today))).satisfies(r -> {
            assertThat(r.oldestDays()).as("lotul vechi s-a terminat").isEqualTo(10);
            assertThat(r.ageFlag()).isNull();
        });
        settings.addLimit(new AuthorizedLimitRequest(depotA.getId(), "STORED", null, new BigDecimal("50"), "T", "AT_ONCE",
                7, false, "din autorizație, pct. 3"));
        assertThat(row(stock.stock(depotA.getId(), today)).ageFlag()).isEqualTo("LIMIT");
        // Istoric: acum 5 zile cel mai vechi lot avea 395 de zile.
        assertThat(row(stock.stock(depotA.getId(), today.minusDays(5))).oldestDays()).isEqualTo(395);
    }

    @Test
    void anExitEatsTheOldestLotsFirstEvenWhenTheyAreUneven() {
        operations.finalizeOperation(weighed(WeighingOperationType.IN, depotA, today.minusDays(400), "100", null));
        operations.finalizeOperation(weighed(WeighingOperationType.IN, depotA, today.minusDays(200), "300", null));
        operations.finalizeOperation(weighed(WeighingOperationType.IN, depotA, today.minusDays(10), "20", null));
        operations.finalizeOperation(weighed(WeighingOperationType.OUT, depotA, today, "110", WasteOperationCode.R3));
        // Lotul de 100 s-a dus întreg, din cel de 300 au plecat 10: cel mai vechi rămas are 200 de zile.
        assertThat(row(stock.stock(depotA.getId(), today))).satisfies(r -> {
            assertThat(r.stockKg()).isEqualByComparingTo("310");
            assertThat(r.oldestDays()).isEqualTo(200);
        });
    }

    @Test
    void theLimitsFromTheAuthorizationAreComparedWhereTheAppWeighs() {
        UUID code = cardboard.getWasteCode().getId();
        operations.finalizeOperation(weighed(WeighingOperationType.IN, depotA, today, "800", null));
        operations.finalizeOperation(weighed(WeighingOperationType.OUT, depotA, today, "150", WasteOperationCode.R3));
        settings.addLimit(new AuthorizedLimitRequest(depotA.getId(), "STORED", code, new BigDecimal("0.5"), "T", "AT_ONCE",
                null, false, null));
        settings.addLimit(new AuthorizedLimitRequest(depotA.getId(), "OUTPUT", null, new BigDecimal("1"), "T", "YEAR",
                null, true, null));
        settings.addLimit(new AuthorizedLimitRequest(depotA.getId(), "STORED", null, new BigDecimal("40"), "M3", "AT_ONCE",
                null, false, null));

        var limits = stock.stock(depotA.getId(), today).limits();
        assertThat(limits).hasSize(3);
        assertThat(limits.get(0)).satisfies(l -> {
            assertThat(l.usedKg()).isEqualByComparingTo("650");
            assertThat(l.exceeded()).isTrue();
        });
        assertThat(limits.get(1)).satisfies(l -> {
            assertThat(l.usedKg()).isEqualByComparingTo("150");
            assertThat(l.exceeded()).isFalse();
        });
        assertThat(limits.get(2).comparable()).as("volumul nu se cântărește").isFalse();
        assertThat(stock.stock(depotB.getId(), today).limits()).isEmpty();

        assertBusiness(() -> settings.addLimit(new AuthorizedLimitRequest(depotA.getId(), "ALTCEVA", null, BigDecimal.ONE,
                "T", "YEAR", null, false, null)), ErrorMessageEnum.AUTHORIZED_LIMIT_INVALID);
        assertBusiness(() -> settings.addLimit(new AuthorizedLimitRequest(depotA.getId(), "STORED", null, BigDecimal.ZERO,
                "T", "YEAR", null, false, null)), ErrorMessageEnum.AUTHORIZED_LIMIT_QUANTITY);
    }

    @Test
    void onlyWhoManagesTheFirmWritesThresholdsAndLimits() throws Exception {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        AppUser operator = user(Role.OPERATOR);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/stock/thresholds")
                        .header("Authorization", "Bearer " + jwtService.generateToken(operator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workPointId\":\"" + depotA.getId() + "\",\"articleId\":\"" + cardboard.getId()
                                + "\",\"maxKg\":10}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/stock/thresholds")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workPointId\":\"" + depotA.getId() + "\",\"articleId\":\"" + cardboard.getId()
                                + "\",\"maxKg\":10}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    private static void assertBusiness(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorMessageEnum code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }

    // ---------------------------------------------------------------------------------------------

    private UUID weighed(WeighingOperationType type, WorkPoint depot, LocalDate date, String kg, WasteOperationCode code) {
        boolean transfer = type == WeighingOperationType.TRANSFER;
        UUID id = operations.create(new WeighingOperationRequest(type, depot.getId(), date,
                transfer ? null : partner.getId(), null, null, null, null, null, null, null, null, null, null, null,
                null, transfer ? depotB.getId() : null)).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(new WeighingLinesRequest.Line(
                cardboard.getId(), null, null, new BigDecimal(kg), null, null, code, null))));
        return id;
    }

    private StockResponse.Row row(StockResponse response) {
        return response.rows().stream().filter(r -> cardboard.getId().equals(r.articleId())).findFirst()
                .orElseThrow(() -> new AssertionError("fără rând de carton: " + response.rows()));
    }

    private BigDecimal cardboardKg(StockResponse response) {
        return row(response).stockKg();
    }

    private WorkPoint depot(String name) {
        return workPointRepository.save(WorkPoint.builder()
                .company(company).name(name + " " + suffix()).active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Role role) {
        return appUserRepository.save(AppUser.builder()
                .email("stoc+" + suffix() + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private void actAs(AppUser user) {
        TenantContext.set(company.getId());
        AppUser fresh = appUserRepository.findById(user.getId()).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(fresh, null, List.of()));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
