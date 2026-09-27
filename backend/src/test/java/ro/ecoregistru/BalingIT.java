package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
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
import ro.ecoregistru.controller.request.WasteArticleRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.StockResponse;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.AuthorizedLimit;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.StockOpening;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.StockOpeningSource;
import ro.ecoregistru.enums.StockOpeningStatus;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.AuthorizedLimitRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.StockOpeningRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.Art48RegisterService;
import ro.ecoregistru.service.BalingService;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.service.MovementQueryService;
import ro.ecoregistru.service.StockService;
import ro.ecoregistru.service.WasteArticleService;
import ro.ecoregistru.service.WeighingDocumentService;
import ro.ecoregistru.service.WeighingOperationService;
import ro.ecoregistru.service.export.Art48Register;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * F5 — balotarea: operatorul scrie doar câți baloți a făcut; greutatea standard a sortimentului balotat dă kilogramele,
 * care ies din sortimentul vrac și intră în cel balotat, sub R12, fără să fie preluare sau predare. Pierderea reală
 * se vede la cântărirea de la vânzare și la inventar (proprietarul, 27.09.2026).
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class BalingIT {

    @Autowired BalingService balings;
    @Autowired WeighingOperationService operations;
    @Autowired WeighingDocumentService documents;
    @Autowired WasteArticleService articles;
    @Autowired StockService stock;
    @Autowired MovementQueryService movementQuery;
    @Autowired Art48RegisterService art48;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired AuthorizedLimitRepository limitRepository;
    @Autowired StockOpeningRepository openingRepository;

    LocalDate today;
    Company company;
    AppUser admin;
    AppUser operator;
    WorkPoint depot;
    WasteCode cardboardCode;
    WasteArticle loose;
    WasteArticle baled;

    @BeforeEach
    void setUp() {
        today = DeadlineService.today();
        company = companyRepository.save(Company.builder()
                .name("Balotare " + suffix() + " SRL").cui("ROB" + suffix()).type(CompanyType.COLLECTOR)
                .environmentalAuthNumber("AM-" + suffix()).active(true).createdAt(Instant.now()).build());
        admin = user(Role.ADMIN);
        operator = user(Role.OPERATOR);
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Florești " + suffix()).active(true).createdAt(Instant.now()).build());
        cardboardCode = wasteCodeRepository.findAll().stream().filter(c -> "15 01 01".equals(c.getCode())).findFirst().orElseThrow();
        actAs(admin);
        loose = articleRepository.findById(articles.create(new WasteArticleRequest("Carton vrac", cardboardCode.getId(),
                false, false, null, null)).id()).orElseThrow();
        baled = articleRepository.findById(articles.create(new WasteArticleRequest("Carton balotat", cardboardCode.getId(),
                false, false, loose.getId(), new BigDecimal("380"))).id()).orElseThrow();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void theOperatorWritesTheBaleCountAndTheStockMovesAtTheStandardWeight() {
        collect("5000");
        actAs(operator);

        WeighingOperationResponse r = balings.create(new BalingRequest(depot.getId(), today, baled.getId(), 12, "tura 1"));

        assertThat(r.type()).isEqualTo(WeighingOperationType.PROCESSING);
        assertThat(r.status()).as("fără preț și fără document: se salvează finalizată").isEqualTo(WeighingOperationStatus.FINALIZED);
        assertThat(r.number()).isEqualTo(1);
        assertThat(r.baling().baleCount()).isEqualTo(12);
        assertThat(r.baling().baleWeightKg()).isEqualByComparingTo("380");
        assertThat(r.baling().kg()).isEqualByComparingTo("4560");
        assertThat(r.baling().sourceArticleName()).isEqualTo("Carton vrac");
        assertThat(r.lines()).extracting(WeighingOperationResponse.Line::articleName)
                .containsExactly("Carton vrac", "Carton balotat");
        assertThat(r.lines()).allSatisfy(l -> {
            assertThat(l.finalKg()).isEqualByComparingTo("4560");
            assertThat(l.operationCode()).isEqualTo(WasteOperationCode.R12);
        });
        assertThat(movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(r.id()))
                .extracting(m -> m.getOperation(), m -> m.getTreatmentMethod())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(WasteOperation.PROCESSING_INPUT, ro.ecoregistru.enums.TreatmentMethod.TM),
                        org.assertj.core.groups.Tuple.tuple(WasteOperation.PROCESSING_OUTPUT, ro.ecoregistru.enums.TreatmentMethod.TM));

        StockResponse s = stock.stock(depot.getId(), today);
        assertThat(kg(s, loose)).isEqualByComparingTo("440");
        assertThat(kg(s, baled)).isEqualByComparingTo("4560");
        assertThat(r.stockWarnings()).isEmpty();
    }

    @Test
    void aLaterStandardWeightDoesNotRewriteAnEarlierBaling() {
        collect("5000");
        UUID id = balings.create(new BalingRequest(depot.getId(), today, baled.getId(), 10, null)).id();

        articles.update(baled.getId(), new WasteArticleRequest("Carton balotat", cardboardCode.getId(), false, false,
                loose.getId(), new BigDecimal("400")));

        WeighingOperationResponse again = operations.get(id);
        assertThat(again.baling().baleWeightKg()).isEqualByComparingTo("380");
        assertThat(again.baling().kg()).isEqualByComparingTo("3800");
        assertThat(kg(stock.stock(depot.getId(), today), baled)).isEqualByComparingTo("3800");
    }

    @Test
    void aBaledArticleIsMadeFromALooseOneOfTheSameCodeWithAWeight() {
        WasteCode other = wasteCodeRepository.findAll().stream()
                .filter(c -> !c.isHazardous() && !c.getId().equals(cardboardCode.getId())).findFirst().orElseThrow();
        assertBusiness(() -> articles.create(new WasteArticleRequest("Folie balotată", other.getId(), false, false,
                loose.getId(), new BigDecimal("300"))), ErrorMessageEnum.WASTE_ARTICLE_BALE_SOURCE_CODE);
        assertBusiness(() -> articles.create(new WasteArticleRequest("Carton presat", cardboardCode.getId(), false, false,
                loose.getId(), null)), ErrorMessageEnum.WASTE_ARTICLE_BALE_INCOMPLETE);
        assertBusiness(() -> articles.create(new WasteArticleRequest("Carton presat", cardboardCode.getId(), false, false,
                null, new BigDecimal("300"))), ErrorMessageEnum.WASTE_ARTICLE_BALE_INCOMPLETE);
        assertBusiness(() -> articles.create(new WasteArticleRequest("Carton presat", cardboardCode.getId(), false, false,
                loose.getId(), BigDecimal.ZERO)), ErrorMessageEnum.WASTE_ARTICLE_BALE_WEIGHT_POSITIVE);
        assertBusiness(() -> articles.create(new WasteArticleRequest("Carton dublu", cardboardCode.getId(), false, false,
                baled.getId(), new BigDecimal("300"))), ErrorMessageEnum.WASTE_ARTICLE_BALE_SOURCE_BALED);
        assertBusiness(() -> articles.update(loose.getId(), new WasteArticleRequest("Carton vrac", cardboardCode.getId(),
                false, false, loose.getId(), new BigDecimal("300"))), ErrorMessageEnum.WASTE_ARTICLE_BALE_SOURCE_BALED);
        UUID alone = articles.create(new WasteArticleRequest("Carton singur", cardboardCode.getId(), false, false,
                null, null)).id();
        assertBusiness(() -> articles.update(alone, new WasteArticleRequest("Carton singur", cardboardCode.getId(),
                false, false, alone, new BigDecimal("300"))), ErrorMessageEnum.WASTE_ARTICLE_BALE_SOURCE_BALED);

        assertThat(articles.list()).filteredOn(a -> a.id().equals(baled.getId())).singleElement().satisfies(a -> {
            assertThat(a.sourceArticleId()).isEqualTo(loose.getId());
            assertThat(a.sourceArticleName()).isEqualTo("Carton vrac");
            assertThat(a.baleWeightKg()).isEqualByComparingTo("380");
        });
    }

    @Test
    void aLooseArticleThatFeedsBalesKeepsItsCodeAndIsNotBaledItself() {
        WasteCode other = wasteCodeRepository.findAll().stream()
                .filter(c -> !c.isHazardous() && !c.getId().equals(cardboardCode.getId())).findFirst().orElseThrow();
        assertBusiness(() -> articles.update(loose.getId(), new WasteArticleRequest("Carton vrac", other.getId(),
                false, false, null, null)), ErrorMessageEnum.WASTE_ARTICLE_BALE_SOURCE_CODE);
        UUID loose2 = articles.create(new WasteArticleRequest("Carton de la magazine", cardboardCode.getId(),
                false, false, null, null)).id();
        assertBusiness(() -> articles.update(loose.getId(), new WasteArticleRequest("Carton vrac", cardboardCode.getId(),
                false, false, loose2, new BigDecimal("300"))), ErrorMessageEnum.WASTE_ARTICLE_BALE_SOURCE_BALED);
    }

    @Test
    void aBalingNeedsABaledArticleAPositiveCountAndADate() {
        assertBusiness(() -> balings.create(new BalingRequest(depot.getId(), today, loose.getId(), 3, null)),
                ErrorMessageEnum.BALING_ARTICLE_NOT_BALED);
        assertBusiness(() -> balings.create(new BalingRequest(depot.getId(), today, baled.getId(), 0, null)),
                ErrorMessageEnum.BALING_COUNT_POSITIVE);
        assertBusiness(() -> balings.create(new BalingRequest(depot.getId(), today, baled.getId(), null, null)),
                ErrorMessageEnum.BALING_COUNT_POSITIVE);
        assertBusiness(() -> balings.create(new BalingRequest(depot.getId(), null, baled.getId(), 2, null)),
                ErrorMessageEnum.WEIGHING_OPERATION_DATE_REQUIRED);
        baled.setActive(false);
        articleRepository.save(baled);
        assertBusiness(() -> balings.create(new BalingRequest(depot.getId(), today, baled.getId(), 2, null)),
                ErrorMessageEnum.BALING_ARTICLE_NOT_BALED);
    }

    @Test
    void theGenericFormStillRefusesAProcessingOperation() {
        assertBusiness(() -> operations.create(new WeighingOperationRequest(WeighingOperationType.PROCESSING,
                depot.getId(), today, null, null, null, null, null, null, null, null, null, null, null, null, null)),
                ErrorMessageEnum.WEIGHING_OPERATION_TYPE_UNAVAILABLE);
    }

    @Test
    void negativeStockAndAMissingR12WarnButDoNotStopTheBaling() {
        collect("1000");
        company = companyRepository.findById(company.getId()).orElseThrow();
        company.setAuthorizedOperationCodes(EnumSet.of(WasteOperationCode.R13));
        companyRepository.save(company);

        WeighingOperationResponse r = balings.create(new BalingRequest(depot.getId(), today, baled.getId(), 3, null));

        assertThat(r.status()).isEqualTo(WeighingOperationStatus.FINALIZED);
        assertThat(r.stockWarnings()).singleElement().satisfies(w -> {
            assertThat(w.articleId()).isEqualTo(loose.getId());
            assertThat(w.stockKg()).isEqualByComparingTo("-140");
        });
        assertThat(r.baling().r12NotAuthorized()).isTrue();

        company.setAuthorizedOperationCodes(EnumSet.of(WasteOperationCode.R12, WasteOperationCode.R13));
        companyRepository.save(company);
        assertThat(operations.get(r.id()).baling().r12NotAuthorized()).isFalse();
    }

    @Test
    void onlyAnApproverCancelsAndTheStockComesBack() {
        collect("5000");
        actAs(operator);
        UUID id = balings.create(new BalingRequest(depot.getId(), today, baled.getId(), 5, null)).id();

        assertThatThrownBy(() -> operations.cancel(id, "greșit")).isInstanceOf(AccessDeniedException.class);

        actAs(admin);
        WeighingOperationResponse cancelled = operations.cancel(id, "au fost 4 baloți");
        assertThat(cancelled.status()).isEqualTo(WeighingOperationStatus.CANCELLED);
        StockResponse s = stock.stock(depot.getId(), today);
        assertThat(kg(s, loose)).isEqualByComparingTo("5000");
        assertThat(kg(s, baled)).isEqualByComparingTo("0");
    }

    @Test
    void aBalingIsNotEditableAndHasNoTransportDocuments() {
        collect("5000");
        UUID id = balings.create(new BalingRequest(depot.getId(), today, baled.getId(), 5, null)).id();

        assertBusiness(() -> operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of())),
                ErrorMessageEnum.WEIGHING_OPERATION_NOT_EDITABLE);
        assertThatThrownBy(() -> documents.renderAnexa3(id)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> documents.renderAviz(id)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> documents.renderBorderou(id)).isInstanceOf(BusinessException.class);
    }

    @Test
    void aBalingDatedBeforeAClosedPeriodIsRefused() {
        openingRepository.save(StockOpening.builder().companyId(company.getId()).workPoint(depot).number(1)
                .cutOffDate(today).source(StockOpeningSource.STOCK_CARDS).status(StockOpeningStatus.CONFIRMED)
                .confirmedOn(today).createdAt(Instant.now()).updatedAt(Instant.now()).build());

        assertBusiness(() -> balings.create(new BalingRequest(depot.getId(), today.minusDays(1), baled.getId(), 2, null)),
                ErrorMessageEnum.STOCK_PERIOD_CLOSED);
    }

    @Test
    void theBalingIsTreatmentNotATakeoverOrAHandover() {
        collect("5000");
        balings.create(new BalingRequest(depot.getId(), today, baled.getId(), 10, null));

        // Lista de mișcări și totalurile de pe Acasă nu văd balotarea: nu e nici intrare, nici ieșire.
        assertThat(movementQuery.list(today.getYear(), null, null, null, false, false, false, null, null, null, null,
                0, 100, null, false).content()).extracting(m -> m.operation()).containsOnly(WasteOperation.COLLECTED);
        assertThat(movementQuery.summary(today.getYear(), today.getMonthValue()).movements()).isEqualTo(1);

        // Registrul art. 48: ambele rânduri, cu R12 și modul de tratare; pe cod, balotarea e tratare, stocul final rămâne.
        Art48Register register = art48.build(today.getYear(), depot.getId());
        assertThat(register.entries()).extracting(Art48Register.Entry::operation)
                .containsExactly("Preluare", "Balotare: intrat la presă", "Balotare: rezultat");
        assertThat(register.entries().subList(1, 3)).allSatisfy(e -> {
            assertThat(e.operationCode()).isEqualTo("R12");
            assertThat(e.treatment()).isEqualTo("Tratare mecanică");
        });
        assertThat(register.collection()).singleElement().satisfies(c -> {
            assertThat(c.collectedKg()).isEqualByComparingTo("5000");
            assertThat(c.treatedKg()).isEqualByComparingTo("3800");
            assertThat(c.treatedResultKg()).isEqualByComparingTo("3800");
            assertThat(c.recoveredKg()).isEqualByComparingTo("0");
            assertThat(c.closingKg()).isEqualByComparingTo("5000");
        });
        assertThat(register.recovery()).isEmpty();
        assertThat(art48.build(today.getYear(), null).collection()).singleElement()
                .satisfies(c -> assertThat(c.closingKg()).isEqualByComparingTo("5000"));
    }

    @Test
    void aBalingOfAnEarlierYearKeepsTheOpeningStockOfTheCode() {
        LocalDate lastYear = today.minusYears(1);
        collect("5000", lastYear);
        balings.create(new BalingRequest(depot.getId(), lastYear, baled.getId(), 10, null));

        assertThat(art48.build(today.getYear(), depot.getId()).collection()).singleElement()
                .satisfies(c -> assertThat(c.openingKg()).isEqualByComparingTo("5000"));
    }

    @Test
    void theTreatedLimitIsComparedAndTheOutputLimitIgnoresTheBaling() {
        collect("5000");
        limit("TREATED", "3");
        limit("OUTPUT", "1");
        balings.create(new BalingRequest(depot.getId(), today, baled.getId(), 10, null));

        List<StockResponse.Limit> limits = stock.stock(depot.getId(), today).limits();
        assertThat(limits).filteredOn(l -> l.kind().equals("TREATED")).singleElement().satisfies(l -> {
            assertThat(l.comparable()).isTrue();
            assertThat(l.usedKg()).isEqualByComparingTo("3800");
            assertThat(l.exceeded()).isTrue();
        });
        assertThat(limits).filteredOn(l -> l.kind().equals("OUTPUT")).singleElement()
                .satisfies(l -> assertThat(l.usedKg()).isEqualByComparingTo("0"));
    }

    private void limit(String kind, String tonnes) {
        limitRepository.save(AuthorizedLimit.builder().companyId(company.getId()).workPoint(depot).kind(kind)
                .quantity(new BigDecimal(tonnes)).unit("T").period("YEAR").approximate(false)
                .createdAt(Instant.now()).build());
    }

    /** O intrare finalizată de la un partener, pe sortimentul vrac. */
    private void collect(String kg) {
        collect(kg, today);
    }

    private void collect(String kg, LocalDate date) {
        Partner partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin " + suffix() + " SRL").cui("RO" + suffix())
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        UUID id = operations.create(new WeighingOperationRequest(WeighingOperationType.IN, depot.getId(), date,
                partner.getId(), null, null, null, null, null, null, null, null, null, null, null, null)).id();
        operations.replaceLines(id, new WeighingLinesRequest(null, null, List.of(
                new WeighingLinesRequest.Line(loose.getId(), null, null, new BigDecimal(kg), null, null, null, null))));
        operations.finalizeOperation(id);
    }

    private static BigDecimal kg(StockResponse s, WasteArticle article) {
        return s.rows().stream().filter(r -> article.getId().equals(r.articleId()))
                .map(StockResponse.Row::stockKg).findFirst().orElse(BigDecimal.ZERO);
    }

    private static void assertBusiness(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorMessageEnum code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }

    private AppUser user(Role role) {
        return appUserRepository.save(AppUser.builder()
                .email("balotare+" + suffix() + "@demo.ro").password("x")
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
