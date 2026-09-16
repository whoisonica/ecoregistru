package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest.Line;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.DepotRetentionReport;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.PriceVisibility;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
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
 * D1.9 și D1.10 — reținerile la sursă și raportul lor.
 *
 * <p>Ce apără, punct cu punct: cei <b>2%</b> AFM se rețin pe orice intrare cu preț, de la firmă sau de
 * la persoană fizică, pe orice deșeu (OUG 196/2005 art. 9 alin. (1) lit. a)); cei <b>10%</b> impozit,
 * doar pe liniile de metal ale unei intrări de la o persoană fizică (Codul fiscal art. 114 alin. (2)
 * lit. m^2)); amândouă pe valoarea brută, independent una de alta (§18.1); o operațiune anulată sau în
 * lucru nu se declară; luna e cea a datei operațiunii.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DepotRetentionIT {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 15);

    @Autowired WeighingOperationService service;
    @Autowired WeighingOperationRepository operationRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired NaturalPersonRepository personRepository;

    Company company;
    AppUser admin;
    AppUser viewer;
    AppUser consultant;
    WorkPoint depot;
    Partner partner;
    NaturalPerson person;
    WasteArticle copper;
    WasteArticle cardboard;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.builder()
                .name("Rețineri " + suffix() + " SRL").cui("ROR" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
        admin = user(Role.ADMIN);
        viewer = user(Role.CLIENT_VIEWER);
        // Un consultant are cabinet, nu firmă (`app_users_consultant_scope`), deci nu se salvează aici.
        consultant = AppUser.builder().id(UUID.randomUUID()).email("consultant@demo.ro")
                .role(Role.CONSULTANT).enabled(true).build();
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit Baciu").active(true).createdAt(Instant.now()).build());
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin Alfa SRL").cui("RO" + suffix())
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        person = personRepository.save(NaturalPerson.builder()
                .company(company).name("Ion Popescu").cnp("1900101123457")
                .identification("CJ 123456").address("Cluj, str. Mare 1")
                .active(true).createdAt(Instant.now()).build());
        copper = article("Cupru", 0, true);
        cardboard = article("Carton", 1, false);
        actAs(admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    /** O intrare de la o firmă: 2% pe toată valoarea, impozit zero — firma își declară singură venitul. */
    @Test
    void anEntryFromACompanyWithholdsOnlyTheTwoPercent() {
        UUID id = finalized(fromPartner(), line(cardboard, "1000", "0.50"));

        WeighingOperation stored = operationRepository.findById(id).orElseThrow();
        assertThat(stored.getAfmBase()).isEqualByComparingTo("500.00");
        assertThat(stored.getAfmContribution()).isEqualByComparingTo("10.00");
        assertThat(stored.getIncomeTaxBase()).isEqualByComparingTo("0.00");
        assertThat(stored.getIncomeTax()).isEqualByComparingTo("0.00");
    }

    /**
     * O intrare de la o persoană fizică, cu metal și carton: cei 2% pe tot, cei 10% doar pe metal.
     * Cele două nu se scad una din alta — amândouă pleacă din valoarea brută.
     */
    @Test
    void onlyTheMetalOfAnIndividualIsTaxed() {
        UUID id = finalized(fromPerson(), line(copper, "100", "20"), line(cardboard, "1000", "0.50"));

        WeighingOperation stored = operationRepository.findById(id).orElseThrow();
        assertThat(stored.getAfmBase()).isEqualByComparingTo("2500.00");   // 2000 cupru + 500 carton
        assertThat(stored.getAfmContribution()).isEqualByComparingTo("50.00");
        assertThat(stored.getIncomeTaxBase()).isEqualByComparingTo("2000.00");
        assertThat(stored.getIncomeTax()).isEqualByComparingTo("200.00");
    }

    /**
     * Metalul cumpărat de la o <b>firmă</b> nu se impozitează: impozitul e pe venitul persoanei fizice
     * din patrimoniul personal (Codul fiscal art. 114 alin. (2) lit. m^2)). Firma își declară singură
     * venitul, iar din intrare se rețin doar cei 2%.
     */
    @Test
    void metalFromACompanyIsNotTaxed() {
        UUID id = finalized(fromPartner(), line(copper, "100", "20"));

        WeighingOperation stored = operationRepository.findById(id).orElseThrow();
        assertThat(stored.getAfmContribution()).isEqualByComparingTo("40.00");
        assertThat(stored.getIncomeTaxBase()).isEqualByComparingTo("0.00");
        assertThat(stored.getIncomeTax()).isEqualByComparingTo("0.00");
        assertThat(report(9).beneficiaries()).isEmpty();
    }

    /** Ieșirea nu reține nimic: acolo depozitul e vânzătorul, iar cei 2% îi reține cumpărătorul. */
    @Test
    void anExitWithholdsNothing() {
        UUID id = service.create(head(OUT, partner.getId(), null, null)).id();
        service.replaceLines(id, new WeighingLinesRequest(null, null, List.of(
                new Line(cardboard.getId(), null, null, new BigDecimal("1000"), null,
                        new BigDecimal("0.50"), WasteOperationCode.R3, null))));
        service.finalizeOperation(id);

        WeighingOperation stored = operationRepository.findById(id).orElseThrow();
        assertThat(stored.getAfmContribution()).isEqualByComparingTo("0.00");
        assertThat(report(9).afmContribution()).isEqualByComparingTo("0.00");
        assertThat(report(9).operations()).isZero();
    }

    /** Rotunjirea se face o dată, pe total: două linii de 12,505 lei fac 25,01, nu 25,02. */
    @Test
    void theRoundingIsOnTheTotalNotOnEachLine() {
        UUID id = finalized(fromPartner(), line(cardboard, "1", "625.25"), line(cardboard, "1", "625.25"));

        WeighingOperation stored = operationRepository.findById(id).orElseThrow();
        assertThat(stored.getAfmBase()).isEqualByComparingTo("1250.50");
        assertThat(stored.getAfmContribution()).isEqualByComparingTo("25.01");
    }

    /** O linie fără preț nu intră în bază: nu s-a plătit nimic, deci n-are din ce se reține. */
    @Test
    void aLineWithoutAPriceIsNotInTheBase() {
        UUID id = finalized(fromPartner(), line(cardboard, "1000", "0.50"), line(cardboard, "500", null));

        assertThat(operationRepository.findById(id).orElseThrow().getAfmBase()).isEqualByComparingTo("500.00");
    }

    /** Cât e în lucru, nu s-a reținut nimic: reținerea se naște la finalizare, odată cu documentul. */
    @Test
    void anOperationInProgressHasNoRetentions() {
        UUID id = service.create(fromPartner()).id();
        service.replaceLines(id, new WeighingLinesRequest(null, null, List.of(line(cardboard, "1000", "0.50"))));

        WeighingOperation stored = operationRepository.findById(id).orElseThrow();
        assertThat(stored.getAfmContribution()).isNull();
        assertThat(report(9).afmBase()).isEqualByComparingTo("0.00");
        assertThat(report(9).operations()).isZero();
    }

    /** Anularea scoate operațiunea din declarație, dar cifrele rămân pe ea: documentul nu se șterge. */
    @Test
    void aCancelledOperationIsNotDeclaredButKeepsItsNumbers() {
        UUID id = finalized(fromPartner(), line(cardboard, "1000", "0.50"));
        assertThat(report(9).afmContribution()).isEqualByComparingTo("10.00");

        service.cancel(id, "Cântărire dublă");

        assertThat(operationRepository.findById(id).orElseThrow().getAfmContribution())
                .isEqualByComparingTo("10.00");
        assertThat(report(9).afmContribution()).isEqualByComparingTo("0.00");
        assertThat(report(9).operations()).isZero();
    }

    /** Luna e a datei operațiunii, nu a introducerii ei: o intrare de pe 15 septembrie e a lui septembrie. */
    @Test
    void theMonthFollowsTheOperationDate() {
        finalized(fromPartner(), line(cardboard, "1000", "0.50"));

        assertThat(report(9).afmContribution()).isEqualByComparingTo("10.00");
        assertThat(report(10).afmContribution()).isEqualByComparingTo("0.00");
        assertThat(report(9).from()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(report(9).to()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    /** Termenele: lunar pe 25 a lunii următoare; anual, D205, ultima zi a lui februarie. */
    @Test
    void theReportCarriesItsDeadline() {
        assertThat(report(9).dueDate()).isEqualTo(LocalDate.of(2026, 10, 25));
        assertThat(report(12).dueDate()).isEqualTo(LocalDate.of(2027, 1, 25));
        assertThat(service.retentions(2026, null).dueDate()).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    /** D205: pe an, fiecare persoană de la care s-a reținut impozit, cu venitul brut și impozitul. */
    @Test
    void theAnnualReportListsTheTaxedIndividuals() {
        finalized(fromPerson(), line(copper, "100", "20"));
        NaturalPerson paperOnly = personRepository.save(NaturalPerson.builder()
                .company(company).name("Maria Ionescu").active(true).createdAt(Instant.now()).build());
        finalized(head(IN, null, paperOnly.getId(), null), line(cardboard, "1000", "0.50"));

        DepotRetentionReport annual = service.retentions(2026, null);

        assertThat(annual.afmContribution()).isEqualByComparingTo("50.00");   // 2% din 2000 + 500
        assertThat(annual.beneficiaries()).singleElement().satisfies(b -> {
            assertThat(b.name()).isEqualTo("Ion Popescu");
            assertThat(b.cnp()).isEqualTo("1900101123457");
            assertThat(b.base()).isEqualByComparingTo("2000.00");
            assertThat(b.tax()).isEqualByComparingTo("200.00");
        });
    }

    /** Metalul de la o persoană fizică se cumpără doar din gospodăria ei, iar borderoul poartă declarația. */
    @Test
    void metalFromAnIndividualNeedsTheOwnHouseholdDeclaration() {
        UUID id = service.create(head(IN, null, person.getId(), null)).id();
        service.replaceLines(id, new WeighingLinesRequest(null, null, List.of(line(copper, "100", "20"))));

        assertThatThrownBy(() -> service.finalizeOperation(id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("gospodăria proprie");

        service.update(id, head(IN, null, person.getId(), true));
        assertThat(operationRepository.findById(id).orElseThrow().getOwnHousehold()).isTrue();
        assertThat(service.finalizeOperation(id).incomeTax()).isEqualByComparingTo("200.00");
    }

    /** Cartonul de la aceeași persoană nu cere declarația: interdicția e a metalelor (OUG 31/2011). */
    @Test
    void paperFromAnIndividualNeedsNoDeclaration() {
        UUID id = service.create(head(IN, null, person.getId(), null)).id();
        service.replaceLines(id, new WeighingLinesRequest(null, null, List.of(line(cardboard, "1000", "0.50"))));

        assertThat(service.finalizeOperation(id).incomeTax()).isEqualByComparingTo("0.00");
    }

    /**
     * Reținerile sunt bani calculați din prețuri: cine nu vede prețurile nu le vede nici pe ele.
     * Consultantul e aici ca să cadă pe regula prețurilor, nu pe cea a rolului: el <b>are</b> dreptul
     * de a finaliza, dar firma i-a ascuns prețurile.
     */
    @Test
    void whoeverCannotSeePricesSeesNeitherTheRetentionsNorTheReport() {
        UUID id = finalized(fromPartner(), line(cardboard, "1000", "0.50"));
        company.setPriceVisibility(PriceVisibility.NO_CONSULTANT);
        companyRepository.saveAndFlush(company);

        actAs(consultant);
        var seen = service.get(id);
        assertThat(seen.afmContribution()).isNull();
        assertThat(seen.afmBase()).isNull();
        assertThat(seen.incomeTax()).isNull();
        assertThatThrownBy(() -> service.retentions(2026, 9)).isInstanceOf(AccessDeniedException.class);

        // Iar vizualizatorul firmei, care vede prețurile, tot nu primește raportul: nu administrează.
        actAs(viewer);
        assertThatThrownBy(() -> service.retentions(2026, 9)).isInstanceOf(AccessDeniedException.class);
    }

    /** Raportul e al firmei: ce a reținut alt depozit nu se vede și nu se adună. */
    @Test
    void anotherCompanysRetentionsAreNotCounted() {
        finalized(fromPartner(), line(cardboard, "1000", "0.50"));
        Company other = companyRepository.save(Company.builder()
                .name("Alt depozit " + suffix() + " SRL").cui("ROX" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser otherAdmin = appUserRepository.save(AppUser.builder()
                .email("admin+" + suffix() + "@alt.ro").password("x").role(Role.ADMIN)
                .company(other).enabled(true).createdAt(Instant.now()).build());

        TenantContext.set(other.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(otherAdmin, null, List.of()));

        assertThat(service.retentions(2026, 9).afmContribution()).isEqualByComparingTo("0.00");
        assertThat(service.retentions(2026, null).beneficiaries()).isEmpty();
    }

    // --- helpers ---

    private DepotRetentionReport report(int month) {
        return service.retentions(2026, month);
    }

    private UUID finalized(WeighingOperationRequest head, Line... lines) {
        UUID id = service.create(head).id();
        service.replaceLines(id, new WeighingLinesRequest(null, null, List.of(lines)));
        service.finalizeOperation(id);
        return id;
    }

    private WeighingOperationRequest fromPartner() {
        return head(IN, partner.getId(), null, null);
    }

    /** Persoana fizică aduce metal, deci capul poartă declarația de gospodărie proprie. */
    private WeighingOperationRequest fromPerson() {
        return head(IN, null, person.getId(), true);
    }

    private WeighingOperationRequest head(ro.ecoregistru.enums.WeighingOperationType type,
                                          UUID partnerId, UUID personId, Boolean ownHousehold) {
        return new WeighingOperationRequest(type, depot.getId(), DAY, partnerId, personId,
                null, null, null, null, null, null, null, null, ownHousehold, null);
    }

    private static Line line(WasteArticle article, String kg, String price) {
        return new Line(article.getId(), null, null, new BigDecimal(kg), null,
                price == null ? null : new BigDecimal(price), null, null);
    }

    private WasteArticle article(String name, int codeIndex, boolean metal) {
        return articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findAll().get(codeIndex))
                .name(name).metal(metal).active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + suffix() + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private void actAs(AppUser user) {
        TenantContext.set(company.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
