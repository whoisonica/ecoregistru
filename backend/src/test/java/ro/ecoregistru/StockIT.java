package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * F3 (D3.1, D3.2) — stocul din linii: pe depozit sold = intrat − ieșit, cu marfa în tranzit spre el și cea angajată
 * (ieșiri în lucru); pe firmă, depozitele văzute plus tranzitul; la o dată din trecut, doar ce era până atunci; un
 * sold negativ nu blochează ieșirea, dar se vede pe răspunsul finalizării și în raport.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class StockIT {

    @Autowired StockService stock;
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
                .name("Stoc " + suffix() + " SRL").cui("ROS" + suffix()).type(CompanyType.COLLECTOR)
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
    void theStockOfADepotIsWhatCameInMinusWhatLeftUpToTheDate() {
        operations.finalizeOperation(weighed(WeighingOperationType.IN, depotA, today.minusDays(2), "1000", null));
        operations.finalizeOperation(weighed(WeighingOperationType.OUT, depotA, today.minusDays(1), "300", WasteOperationCode.R3));
        // În lucru nu contează în sold (e „angajat”, mai jos).
        weighed(WeighingOperationType.IN, depotA, today, "5000", null);

        assertThat(cardboardKg(stock.stock(depotA.getId(), today))).isEqualByComparingTo("700");
        assertThat(cardboardKg(stock.stock(depotA.getId(), today.minusDays(2)))).isEqualByComparingTo("1000");
        assertThat(stock.stock(depotA.getId(), today.minusDays(3)).rows()).isEmpty();
        assertThat(stock.stock(depotB.getId(), today).rows()).isEmpty();
    }

    @Test
    void inTransitAndCommittedAreShownBesideTheStockAndTheCompanyKeepsWhatIsOnTheRoad() {
        operations.finalizeOperation(weighed(WeighingOperationType.IN, depotA, today, "1000", null));
        WeighingOperationResponse dispatched = operations.finalizeOperation(
                weighed(WeighingOperationType.TRANSFER, depotA, today, "200", null));
        weighed(WeighingOperationType.OUT, depotA, today, "100", WasteOperationCode.R3);

        StockResponse.Row atA = row(stock.stock(depotA.getId(), today));
        assertThat(atA.stockKg()).isEqualByComparingTo("800");
        assertThat(atA.committedKg()).isEqualByComparingTo("100");
        assertThat(atA.availableKg()).isEqualByComparingTo("700");
        StockResponse.Row atB = row(stock.stock(depotB.getId(), today));
        assertThat(atB.stockKg()).isEqualByComparingTo("0");
        assertThat(atB.inTransitKg()).isEqualByComparingTo("200");
        assertThat(cardboardKg(stock.stock(null, today))).as("marfa de pe drum e tot a firmei").isEqualByComparingTo("1000");

        operations.receive(dispatched.id(), new TransferReceiptRequest(today, null, null, null, List.of(
                new TransferReceiptRequest.Line(dispatched.lines().get(0).id(), null, null, new BigDecimal("190"), null)),
                "NIR 3", "lipsă constatată", null));
        assertThat(row(stock.stock(depotB.getId(), today)).stockKg()).isEqualByComparingTo("190");
        assertThat(row(stock.stock(depotB.getId(), today)).inTransitKg()).isEqualByComparingTo("0");
        assertThat(cardboardKg(stock.stock(null, today))).isEqualByComparingTo("990");
        // Istoric: ieri transferul nu plecase, deci nimic în tranzit.
        assertThat(stock.stock(depotB.getId(), today.minusDays(1)).rows()).isEmpty();
    }

    @Test
    void aNegativeStockWarnsAtFinalizationButDoesNotBlock() {
        WeighingOperationResponse out = operations.finalizeOperation(
                weighed(WeighingOperationType.OUT, depotA, today, "500", WasteOperationCode.R3));
        assertThat(out.status()).isEqualTo(ro.ecoregistru.enums.WeighingOperationStatus.FINALIZED);
        assertThat(out.stockWarnings()).singleElement()
                .satisfies(w -> assertThat(w.stockKg()).isEqualByComparingTo("-500"));
        StockResponse report = stock.stock(depotA.getId(), today);
        assertThat(report.negativeRows()).isEqualTo(1);
        assertThat(row(report).negative()).isTrue();

        // O ieșire acoperită de stoc nu avertizează.
        operations.finalizeOperation(weighed(WeighingOperationType.IN, depotB, today, "900", null));
        assertThat(operations.finalizeOperation(weighed(WeighingOperationType.OUT, depotB, today, "100",
                WasteOperationCode.R3)).stockWarnings()).isEmpty();
    }

    @Test
    void aRestrictedUserSeesOnlyTheStockOfItsDepots() {
        operations.finalizeOperation(weighed(WeighingOperationType.IN, depotA, today, "1000", null));
        operations.finalizeOperation(weighed(WeighingOperationType.IN, depotB, today, "50", null));
        AppUser operator = user(Role.OPERATOR);
        users.changeWorkPoints(operator.getId(), new UserWorkPointsRequest(false, List.of(depotB.getId())));
        actAs(operator);
        assertThat(cardboardKg(stock.stock(null, today))).isEqualByComparingTo("50");
        assertThatThrownBy(() -> stock.stock(depotA.getId(), today))
                .isInstanceOfSatisfying(NotFoundException.class,
                        e -> assertThat(e.getError()).isEqualTo(ErrorMessageEnum.WORK_POINT_NOT_FOUND));
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
