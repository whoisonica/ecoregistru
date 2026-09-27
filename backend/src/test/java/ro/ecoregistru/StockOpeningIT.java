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
import ro.ecoregistru.controller.request.StockOpeningRequest;
import ro.ecoregistru.controller.request.UserWorkPointsRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.StockOpeningResponse;
import ro.ecoregistru.controller.response.StockResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.StockOpeningSource;
import ro.ecoregistru.enums.StockOpeningStatus;
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
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.CompanyUserService;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.StockOpeningService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D3.5 — nota de preluare a soldurilor: stocul care exista înainte de evidența din aplicație, luat din fișele de
 * magazie sau din contabilitate la o dată de tăiere. Confirmarea scrie liniile OPENING_BALANCE; după aceea nota nu se
 * mai schimbă. Tăierea nu poate fi după prima mișcare a depozitului (stocul s-ar număra de două ori).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class StockOpeningIT {

    @Autowired StockOpeningService openings;
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
                .name("Preluare " + suffix() + " SRL").cui("ROP" + suffix()).type(CompanyType.COLLECTOR)
                .environmentalAuthNumber("AM-" + suffix()).active(true).createdAt(Instant.now()).build());
        admin = user(Role.ADMIN);
        depotA = depot("Baciu");
        depotB = depot("Turda");
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin " + suffix() + " SRL").cui("RO" + suffix())
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        cardboard = articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findAll().stream()
                        .filter(c -> c.getCode().startsWith("15 01 01")).findFirst().orElseThrow())
                .name("Carton").active(true).createdAt(Instant.now()).build());
        actAs(admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void confirmWritesOpeningLines() {
        StockOpeningResponse draft = openings.create(request(depotA, today.minusDays(10), "500", "40"));
        assertThat(draft.status()).isEqualTo(StockOpeningStatus.DRAFT);
        assertThat(draft.number()).isNull();

        StockOpeningResponse confirmed = openings.confirm(draft.id());

        assertThat(confirmed.status()).isEqualTo(StockOpeningStatus.CONFIRMED);
        assertThat(confirmed.number()).isEqualTo(1);
        assertThat(confirmed.confirmedOn()).isEqualTo(today);
        var lines = movementRepository.findAll().stream()
                .filter(m -> m.getStockOpening() != null && m.getStockOpening().getId().equals(draft.id())).toList();
        assertThat(lines).hasSize(2).allSatisfy(m -> {
            assertThat(m.getOperation()).isEqualTo(WasteOperation.OPENING_BALANCE);
            assertThat(m.getDate()).isEqualTo(today.minusDays(10));
            assertThat(m.getWorkPoint().getId()).isEqualTo(depotA.getId());
        });
        assertThat(cardboardKg(stock.stock(depotA.getId(), today))).isEqualByComparingTo("540");
    }

    @Test
    void cutOffAfterFirstMovementRefused() {
        operations.finalizeOperation(weighed(depotA, today.minusDays(5), "100"));
        StockOpeningResponse draft = openings.create(request(depotA, today.minusDays(2), "500", null));
        assertThat(draft.firstMovementOn()).isEqualTo(today.minusDays(5));

        assertBusiness(() -> openings.confirm(draft.id()), ErrorMessageEnum.STOCK_OPENING_AFTER_FIRST_MOVEMENT);

        // Aceeași zi cu prima mișcare: soldul e al dimineții, mișcarea vine peste el.
        openings.update(draft.id(), request(depotA, today.minusDays(5), "500", null));
        assertThat(openings.confirm(draft.id()).status()).isEqualTo(StockOpeningStatus.CONFIRMED);
    }

    @Test
    void secondConfirmedRefused() {
        openings.confirm(openings.create(request(depotA, today, "500", null)).id());
        StockOpeningResponse second = openings.create(request(depotA, today, "100", null));

        assertBusiness(() -> openings.confirm(second.id()), ErrorMessageEnum.STOCK_OPENING_ALREADY_CONFIRMED);
        // Alt depozit are nota lui.
        assertThat(openings.confirm(openings.create(request(depotB, today, "100", null)).id()).status())
                .isEqualTo(StockOpeningStatus.CONFIRMED);
    }

    @Test
    void confirmedIsImmutable() {
        UUID id = openings.confirm(openings.create(request(depotA, today, "500", null)).id()).id();

        assertBusiness(() -> openings.update(id, request(depotA, today, "900", null)),
                ErrorMessageEnum.STOCK_OPENING_CONFIRMED_IMMUTABLE);
        assertBusiness(() -> openings.delete(id), ErrorMessageEnum.STOCK_OPENING_CONFIRMED_IMMUTABLE);
        assertBusiness(() -> openings.confirm(id), ErrorMessageEnum.STOCK_OPENING_CONFIRMED_IMMUTABLE);
    }

    @Test
    void draftCanBeDeleted() {
        UUID id = openings.create(request(depotA, today, "500", null)).id();
        openings.delete(id);
        assertThatThrownBy(() -> openings.get(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void keeperAndAccountantRequiredToConfirm() {
        StockOpeningRequest r = request(depotA, today, "500", null);
        UUID id = openings.create(new StockOpeningRequest(r.workPointId(), r.cutOffDate(), r.source(), " ", null,
                null, r.lines())).id();
        assertBusiness(() -> openings.confirm(id), ErrorMessageEnum.STOCK_OPENING_SIGNATURES_REQUIRED);
    }

    @Test
    void emptyLinesRefused() {
        StockOpeningRequest r = request(depotA, today, "500", null);
        assertBusiness(() -> openings.create(new StockOpeningRequest(r.workPointId(), r.cutOffDate(), r.source(),
                r.keeperName(), r.accountantName(), null, List.of())), ErrorMessageEnum.STOCK_OPENING_LINES_REQUIRED);
        assertBusiness(() -> openings.create(request(depotA, today, "0", null)), ErrorMessageEnum.STOCK_OPENING_KG_POSITIVE);
    }

    @Test
    void otherDepotIs404() {
        UUID id = openings.create(request(depotB, today, "500", null)).id();
        AppUser operator = user(Role.OPERATOR);
        users.changeWorkPoints(operator.getId(), new UserWorkPointsRequest(false, List.of(depotA.getId())));
        actAs(operator);

        assertThatThrownBy(() -> openings.get(id)).isInstanceOf(NotFoundException.class);
        assertThat(openings.list(null)).extracting(StockOpeningResponse::id).doesNotContain(id);
    }

    @Test
    void operatorForbidden() throws Exception {
        AppUser operator = user(Role.OPERATOR);
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        String body = "{\"workPointId\":\"" + depotA.getId() + "\",\"cutOffDate\":\"" + today
                + "\",\"source\":\"STOCK_CARDS\",\"keeperName\":\"G\",\"accountantName\":\"C\",\"lines\":[{\"wasteCodeId\":\""
                + cardboard.getWasteCode().getId() + "\",\"kg\":10}]}";
        mockMvc.perform(post("/api/v1/stock-openings").header("Authorization", "Bearer " + jwtService.generateToken(operator))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/stock-openings").header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    // --- helpers ---

    private StockOpeningRequest request(WorkPoint depot, LocalDate cutOff, String cardboardKg, String otherKg) {
        List<StockOpeningRequest.Line> lines = new java.util.ArrayList<>(List.of(
                new StockOpeningRequest.Line(cardboard.getId(), cardboard.getWasteCode().getId(), new BigDecimal(cardboardKg))));
        if (otherKg != null) {
            lines.add(new StockOpeningRequest.Line(null, cardboard.getWasteCode().getId(), new BigDecimal(otherKg)));
        }
        return new StockOpeningRequest(depot.getId(), cutOff, StockOpeningSource.STOCK_CARDS, "Ion Gestionar",
                "Ana Contabil", "din fișele de magazie la 31.08", lines);
    }

    private UUID weighed(WorkPoint depot, LocalDate date, String kg) {
        UUID id = operations.create(new WeighingOperationRequest(WeighingOperationType.IN, depot.getId(), date,
                partner.getId(), null, null, null, null, null, null, null, null, null, null, null, null, null)).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(new WeighingLinesRequest.Line(
                cardboard.getId(), null, null, new BigDecimal(kg), null, null, null, null))));
        return id;
    }

    private BigDecimal cardboardKg(StockResponse response) {
        return response.rows().stream().filter(r -> cardboard.getWasteCode().getId().equals(r.wasteCodeId()))
                .map(StockResponse.Row::stockKg).reduce(BigDecimal.ZERO, BigDecimal::add);
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
                .email("preluare+" + suffix() + "@demo.ro").password("x")
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
