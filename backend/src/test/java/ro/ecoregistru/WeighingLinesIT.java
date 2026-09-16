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
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest.Line;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.WeighingOperationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ro.ecoregistru.enums.WeighingOperationType.IN;
import static ro.ecoregistru.enums.WeighingOperationType.OUT;

/**
 * D1.4 — cântarul pe linii. Liniile se salvează tot formularul odată și ajung în registrul art. 48
 * cu cantitatea finală; neto rămâne alături ca dovadă.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class WeighingLinesIT {

    @Autowired WeighingOperationService service;
    @Autowired WeighingOperationRepository operationRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;

    Company company;
    WorkPoint depot;
    Partner partner;
    WasteArticle cardboard;
    WasteArticle copper;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.builder()
                .name("Cântar " + suffix() + " SRL").cui("ROC" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("cantar+" + suffix() + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit Baciu").active(true).createdAt(Instant.now()).build());
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Reciclator SRL").cui("RO" + suffix())
                .type(PartnerType.COLLECTOR).supplier(true).packagingOrigin(PackagingOrigin.COLECTOR)
                .active(true).createdAt(Instant.now()).build());
        cardboard = article(company, "Carton", 0);
        copper = article(company, "Cupru", 1);
        TenantContext.set(company.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    /** Descărcarea succesivă: tara liniei 1 e brutul liniei 2. */
    @Test
    void successiveLinesKeepNetAsProofAndTheFinalAsQuantity() {
        UUID id = service.create(head(IN)).id();

        WeighingOperationResponse saved = service.replaceLines(id, new WeighingLinesRequest(
                kg("12000"), kg("7500"), List.of(
                        new Line(cardboard.getId(), kg("12000"), kg("9000"), null, kg("2900"), null, null, null),
                        new Line(copper.getId(), kg("9000"), kg("7500"), null, null, null, null, null))));

        assertThat(saved.grossKg()).isEqualByComparingTo("12000");
        assertThat(saved.lines()).extracting(WeighingOperationResponse.Line::lineNo).containsExactly(1, 2);
        assertThat(saved.lines()).extracting(l -> l.netKg().intValue()).containsExactly(3000, 1500);
        assertThat(saved.lines()).extracting(l -> l.finalKg().intValue()).containsExactly(2900, 1500);

        List<WasteMovement> rows = movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id);
        assertThat(rows).extracting(m -> m.getQuantity().intValue()).containsExactly(2900, 1500);
        assertThat(rows).allSatisfy(m -> {
            assertThat(m.getOperation()).isEqualTo(WasteOperation.COLLECTED);
            assertThat(m.getRegister()).isEqualTo(WasteRegister.ART_48);
            assertThat(m.getDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        });
        assertThat(service.get(id).lines()).extracting(WeighingOperationResponse.Line::articleName)
                .containsExactly("Carton", "Cupru");
    }

    @Test
    void netAloneIsEnoughWhenThereIsNoWeighing() {
        UUID id = service.create(head(IN)).id();
        var saved = service.replaceLines(id, lines(new Line(cardboard.getId(), null, null, kg("250"), null, null, null, null)));
        assertThat(saved.lines().get(0).finalKg()).isEqualByComparingTo("250");
    }

    @Test
    void theFinalCannotExceedTheNet() {
        UUID id = service.create(head(IN)).id();
        assertRefused(id, new Line(cardboard.getId(), kg("1000"), kg("400"), null, kg("601"), null, null, null),
                "finală nu poate fi mai mare");
    }

    @Test
    void aTareAboveTheGrossIsRefused() {
        UUID id = service.create(head(IN)).id();
        assertRefused(id, new Line(cardboard.getId(), kg("400"), kg("1000"), null, null, null, null, null),
                "Neto trebuie să fie mai mare decât zero");
    }

    @Test
    void aNetThatContradictsTheWeighingIsRefused() {
        UUID id = service.create(head(IN)).id();
        assertRefused(id, new Line(cardboard.getId(), kg("1000"), kg("400"), kg("500"), null, null, null, null),
                "nu se potrivește");
    }

    @Test
    void aLineWithoutAnyWeightIsRefused() {
        UUID id = service.create(head(IN)).id();
        assertRefused(id, new Line(cardboard.getId(), kg("1000"), null, null, null, null, null, null),
                "brutul și tara sau direct");
    }

    @Test
    void savingAgainReplacesTheLines() {
        UUID id = service.create(head(IN)).id();
        service.replaceLines(id, lines(
                new Line(cardboard.getId(), null, null, kg("100"), null, null, null, null),
                new Line(copper.getId(), null, null, kg("200"), null, null, null, null)));

        service.replaceLines(id, lines(new Line(copper.getId(), null, null, kg("300"), null, null, null, null)));

        assertThat(movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id))
                .extracting(m -> m.getQuantity().intValue()).containsExactly(300);
    }

    @Test
    void theValueIsTheFinalTimesThePriceRounded() {
        UUID id = service.create(head(IN)).id();
        var saved = service.replaceLines(id, lines(
                new Line(copper.getId(), null, null, kg("2900"), null, new BigDecimal("1.23456"), null, null)));
        assertThat(saved.lines().get(0).totalValue()).isEqualByComparingTo("3580.22");
    }

    @Test
    void anExitLineCarriesItsOwnRecoveryOrDisposalCode() {
        UUID id = service.create(head(OUT)).id();
        assertRefused(id, new Line(cardboard.getId(), null, null, kg("100"), null, null, null, null),
                "alege pe fiecare linie operația");

        service.replaceLines(id, lines(
                new Line(cardboard.getId(), null, null, kg("100"), null, null, WasteOperationCode.R3, null),
                new Line(copper.getId(), null, null, kg("50"), null, null, WasteOperationCode.D1, null)));

        assertThat(movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id))
                .extracting(WasteMovement::getOperation)
                .containsExactly(WasteOperation.RECOVERED, WasteOperation.DISPOSED);
    }

    @Test
    void anEntryLineTakesNoOperationCode() {
        UUID id = service.create(head(IN)).id();
        assertThatThrownBy(() -> service.replaceLines(id, lines(
                new Line(cardboard.getId(), null, null, kg("100"), null, null, WasteOperationCode.R3, null))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void aFinalizedOperationIsNoLongerEdited() {
        UUID id = service.create(head(IN)).id();
        WeighingOperation op = operationRepository.findById(id).orElseThrow();
        op.setStatus(WeighingOperationStatus.FINALIZED);
        op.setFinalizedAt(Instant.now());
        operationRepository.saveAndFlush(op);

        assertRefused(id, new Line(cardboard.getId(), null, null, kg("100"), null, null, null, null),
                "nu se mai modifică");
    }

    @Test
    void anotherCompanysArticleIsNotFound() {
        Company other = companyRepository.save(Company.builder()
                .name("Străin " + suffix() + " SRL").cui("ROS" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
        WasteArticle foreign = article(other, "Fier", 2);
        UUID id = service.create(head(IN)).id();

        assertThatThrownBy(() -> service.replaceLines(id, lines(
                new Line(foreign.getId(), null, null, kg("100"), null, null, null, null))))
                .isInstanceOf(NotFoundException.class);
    }

    // --- helpers ---

    private void assertRefused(UUID id, Line line, String message) {
        assertThatThrownBy(() -> service.replaceLines(id, lines(line)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(message);
    }

    private WeighingOperationRequest head(WeighingOperationType type) {
        return new WeighingOperationRequest(type, depot.getId(), LocalDate.of(2026, 9, 15),
                partner.getId(), null, null, null, null, null, null, null, null, null, null, null);
    }

    private static WeighingLinesRequest lines(Line... lines) {
        return new WeighingLinesRequest(null, null, List.of(lines));
    }

    private WasteArticle article(Company owner, String name, int codeIndex) {
        return articleRepository.save(WasteArticle.builder()
                .company(owner).wasteCode(wasteCodeRepository.findAll().get(codeIndex))
                .name(name).active(true).createdAt(Instant.now()).build());
    }

    private static BigDecimal kg(String value) {
        return new BigDecimal(value);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
