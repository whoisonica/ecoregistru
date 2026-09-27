package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
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
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.PriceVisibility;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
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
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.ecoregistru.enums.WeighingOperationType.IN;

/**
 * D1.17a — la o intrare de la o persoană fizică, finalizarea dă numărul de borderou liniilor plătite și numărul de NIR
 * 14-3-1A liniilor preluate gratuit (OMFP 2634/2015; Legea 82/1991 art. 6 alin. (1): documentul se face „în momentul
 * efectuării”). O linie fără preț nu se finalizează: n-ar ști în ce document intră.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class BorderouNirIT {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 15);

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
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
    AppUser operator;
    AppUser viewer;
    WorkPoint depot;
    Partner partner;
    NaturalPerson person;
    WasteArticle copper;
    WasteArticle cardboard;
    WasteArticle pet;

    @BeforeEach
    void setUp() {
        company = newCompany();
        admin = user(Role.ADMIN);
        operator = user(Role.OPERATOR);
        viewer = user(Role.CLIENT_VIEWER);
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
        copper = article("Cupru casnic", "17 04 01", true);
        cardboard = article("Carton", "15 01 01", false);
        pet = article("PET", "15 01 02", false);
        actAs(admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // --- numerele de la finalizare ---

    @Test
    void aPaidLineGetsABorderouNumberOnly() {
        WeighingOperationResponse r = finalized(fromPerson(), line(cardboard, "100", "0.5"));
        assertThat(r.borderouNumber()).isEqualTo(1);
        assertThat(r.receptionNoteNumber()).isNull();
    }

    @Test
    void aFreeLineGetsANirNumberOnly() {
        WeighingOperationResponse r = finalized(fromPerson(), line(pet, "40", "0"));
        assertThat(r.receptionNoteNumber()).isEqualTo(1);
        assertThat(r.borderouNumber()).isNull();
    }

    /** Ecranul trimite „0,00” la fel de des ca „0”: scala nu schimbă ce e gratuit. */
    @Test
    void aMixedIntakeGetsBoth() {
        WeighingOperationResponse r = finalized(fromPerson(), line(cardboard, "100", "5"), line(pet, "40", "0.00"));
        assertThat(r.borderouNumber()).isEqualTo(1);
        assertThat(r.receptionNoteNumber()).isEqualTo(1);
    }

    @Test
    void aLineWithoutPriceStopsFinalization() {
        UUID id = draft(fromPerson(), line(cardboard, "100", "0.5"), line(pet, "40", null));
        assertThatThrownBy(() -> service.finalizeOperation(id))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorMessageEnum.WEIGHING_PF_PRICE_REQUIRED);
        var op = operationRepository.findById(id).orElseThrow();
        assertThat(op.getStatus()).isEqualTo(WeighingOperationStatus.IN_PROGRESS);
        assertThat(op.getBorderouNumber()).isNull();
        assertThat(op.getReceptionNoteNumber()).isNull();
    }

    /**
     * Recenzia finală: la „fără consultant” consultantul finalizează, dar nu vede prețurile și nu le poate trece. Refuzul
     * rămâne (fără preț nu se știe documentul), dar îi spune cine le trece, nu să le treacă el.
     */
    @Test
    void anApproverWhoCannotSeePricesIsToldTheAdminEntersThem() {
        company.setPriceVisibility(PriceVisibility.NO_CONSULTANT);
        companyRepository.save(company);
        UUID id = draft(fromPerson(), line(pet, "40", null));
        actAs(AppUser.builder().id(UUID.randomUUID()).email("consultant@demo.ro").role(Role.CONSULTANT).enabled(true).build());
        assertThatThrownBy(() -> service.finalizeOperation(id))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorMessageEnum.WEIGHING_PF_PRICE_BY_ADMIN);
    }

    @Test
    void anIntakeFromAPartnerGetsNoNumber() {
        WeighingOperationResponse r = finalized(head(partner.getId(), null, null), line(pet, "40", "0"));
        assertThat(r.borderouNumber()).isNull();
        assertThat(r.receptionNoteNumber()).isNull();
    }

    @Test
    void numbersRunPerCompany() {
        assertThat(finalized(fromPerson(), line(cardboard, "100", "0.5")).borderouNumber()).isEqualTo(1);
        assertThat(finalized(fromPerson(), line(cardboard, "100", "0.5")).borderouNumber()).isEqualTo(2);

        setUp(); // o firmă nouă, cu depozitul și persoana ei
        assertThat(finalized(fromPerson(), line(cardboard, "100", "0.5")).borderouNumber()).isEqualTo(1);
    }

    @Test
    void cancellingKeepsTheNumbers() {
        UUID id = finalized(fromPerson(), line(cardboard, "100", "5"), line(pet, "40", "0")).id();
        WeighingOperationResponse r = service.cancel(id, "Dublură");
        assertThat(r.status()).isEqualTo(WeighingOperationStatus.CANCELLED);
        assertThat(r.borderouNumber()).isEqualTo(1);
        assertThat(r.receptionNoteNumber()).isEqualTo(1);
    }

    // --- borderoul ---

    @Test
    void theBorderouOfAMixedIntakeListsOnlyThePaidLine() throws Exception {
        UUID id = finalized(fromPerson(), line(cardboard, "100", "5"), line(pet, "40", "0")).id();
        String text = Golden.flat(Golden.pdfText(pdf(admin, id, "borderou")));
        assertThat(text).contains("Nr.1dindata").contains("Carton—").doesNotContain("PET");
    }

    @Test
    void aFreeOnlyIntakeHasNoBorderou() throws Exception {
        UUID id = finalized(fromPerson(), line(pet, "40", "0")).id();
        refused(id, "borderou", "weighing.borderou.no.paid.lines");
    }

    /** Finalizată înainte de felie, cu prețuri goale: migrarea n-a avut ce numerota. */
    @Test
    void aPreSliceFinalizedIntakeWithoutNumberExplainsItself() throws Exception {
        UUID id = finalized(fromPerson(), line(cardboard, "100", "5")).id();
        var op = operationRepository.findById(id).orElseThrow();
        op.setBorderouNumber(null);
        operationRepository.saveAndFlush(op);
        refused(id, "borderou", "weighing.borderou.no.paid.lines");
    }

    @Test
    void aCancelledBorderouIsStillPrintedWithTheBand() throws Exception {
        UUID id = finalized(fromPerson(), line(cardboard, "100", "5")).id();
        service.cancel(id, "Dublură");
        assertThat(Golden.flat(Golden.pdfText(pdf(admin, id, "borderou")))).contains("Nr.1dindata").contains("ANULAT—Dublură");
    }

    @Test
    void metalOnlyOnTheFreeLineKeepsTheBorderouPlain() throws Exception {
        UUID id = finalized(fromPerson(), line(copper, "10", "0"), line(cardboard, "100", "0.5")).id();
        assertThat(Golden.flat(Golden.pdfText(pdf(admin, id, "borderou"))))
                .contains("BORDEROUDEACHIZIŢIEDEDEŞEURI").doesNotContain("METALICE").doesNotContain("1900101123457");
    }

    @Test
    void theOperatorAtAdminOnlyGetsNoBorderou() throws Exception {
        UUID id = finalized(fromPerson(), line(cardboard, "100", "5")).id();
        company.setPriceVisibility(PriceVisibility.ADMIN_ONLY);
        companyRepository.save(company);
        http(operator, id, "borderou").andExpect(status().isForbidden());
    }

    // --- NIR-ul ---

    @Test
    void theNirCarriesTheModelRubrics() throws Exception {
        UUID id = finalized(fromPerson(), line(pet, "40", "0")).id();
        assertThat(Golden.flat(Golden.pdfText(pdf(admin, id, "nir"))))
                .contains("NOTĂDERECEPŢIEŞICONSTATAREDEDIFERENŢE")
                .contains("Nr.1").contains("15.09.2026")
                .contains(company.getCui()).contains("DepozitBaciu")
                .contains("IonPopescu").contains("fărădocument—preluaregratuitădelapersoanăfizică")
                .contains("PET").contains("150102").contains("kg").contains("40")
                .contains("Valoareajustăsestabileştedecontabil(OMFP1802/2014pct.75alin.(1)lit.d)).")
                .contains("Comisiaderecepţie").contains("Primitîngestiune")
                .contains("GeneratcuWasteHouse");
    }

    @Test
    void theNirOfAMixedIntakeListsOnlyTheFreeLine() throws Exception {
        UUID id = finalized(fromPerson(), line(cardboard, "100", "5.25"), line(pet, "40", "0")).id();
        assertThat(Golden.flat(Golden.pdfText(pdf(admin, id, "nir"))))
                .contains("PET").doesNotContain("Carton").doesNotContain("5,25").doesNotContain("525,00");
    }

    @Test
    void theNirShowsTheCnpOnlyForMetal() throws Exception {
        UUID metal = finalized(fromPerson(), line(copper, "10", "0")).id();
        assertThat(Golden.flat(Golden.pdfText(pdf(admin, metal, "nir")))).contains("1900101123457");
        UUID paper = finalized(fromPerson(), line(pet, "40", "0")).id();
        assertThat(Golden.flat(Golden.pdfText(pdf(admin, paper, "nir")))).doesNotContain("1900101123457");
    }

    @Test
    void aPaidOnlyIntakeHasNoNir() throws Exception {
        refused(finalized(fromPerson(), line(cardboard, "100", "0.5")).id(), "nir", "weighing.nir.not.available");
        refused(finalized(head(partner.getId(), null, null), line(pet, "40", "0")).id(), "nir",
                "weighing.borderou.requires.person");
        UUID draft = draft(fromPerson(), line(pet, "40", "0"));
        service.cancel(draft, "Greșit");
        refused(draft, "nir", "weighing.nir.not.available");
        refused(draft, "borderou", "weighing.borderou.requires.finalized");
    }

    @Test
    void aCancelledNirKeepsItsNumberAndCarriesTheBand() throws Exception {
        UUID id = finalized(fromPerson(), line(pet, "40", "0")).id();
        service.cancel(id, "Dublură");
        assertThat(Golden.flat(Golden.pdfText(pdf(admin, id, "nir")))).contains("Nr.1").contains("ANULAT—Dublură");
    }

    /** NIR-ul n-are prețuri: îl tipărește și operatorul la „Doar administratorul”. */
    @Test
    void theOperatorAtAdminOnlyPrintsTheNir() throws Exception {
        UUID id = finalized(fromPerson(), line(pet, "40", "0")).id();
        company.setPriceVisibility(PriceVisibility.ADMIN_ONLY);
        companyRepository.save(company);
        pdf(operator, id, "nir");
        http(viewer, id, "nir").andExpect(status().isForbidden());
    }

    // --- helpers ---

    private byte[] pdf(AppUser user, UUID id, String what) throws Exception {
        return http(user, id, what).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
    }

    private void refused(UUID id, String what, String code) throws Exception {
        http(admin, id, what).andExpect(status().isBadRequest()).andExpect(jsonPath("$['error-code']", is(code)));
    }

    /** Pe HTTP decide tokenul: contextul pus de {@link #actAs} (fără roluri) nu trebuie să ajungă în cerere. */
    private org.springframework.test.web.servlet.ResultActions http(AppUser user, UUID id, String what) throws Exception {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        try {
            return mockMvc.perform(get(url(id, what)).header("Authorization", bearer(user)));
        } finally {
            actAs(admin);
        }
    }

    private static String url(UUID id, String what) {
        return "/api/v1/weighing-operations/" + id + "/" + what;
    }

    private WeighingOperationResponse finalized(WeighingOperationRequest head, Line... lines) {
        return service.finalizeOperation(draft(head, lines));
    }

    private UUID draft(WeighingOperationRequest head, Line... lines) {
        UUID id = service.create(head).id();
        service.replaceLines(id, new WeighingLinesRequest(null, null, List.of(lines)));
        return id;
    }

    private WeighingOperationRequest fromPerson() {
        return head(null, person.getId(), true);
    }

    private WeighingOperationRequest head(UUID partnerId, UUID personId, Boolean ownHousehold) {
        return new WeighingOperationRequest(IN, depot.getId(), DAY, partnerId, personId,
                null, null, null, null, null, null, null, null, ownHousehold, null, null);
    }

    private static Line line(WasteArticle article, String kg, String price) {
        return new Line(article.getId(), null, null, new BigDecimal(kg), null,
                price == null ? null : new BigDecimal(price), null, null);
    }

    private Company newCompany() {
        return companyRepository.save(Company.builder()
                .name("Borderou NIR " + suffix() + " SRL").cui("RON" + suffix()).address("Cluj, str. Depozitului 3")
                .type(CompanyType.COLLECTOR).active(true).createdAt(Instant.now()).build());
    }

    private WasteArticle article(String name, String code, boolean metal) {
        return articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findByCode(code).orElseThrow())
                .name(name).metal(metal).active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+nir" + suffix() + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private void actAs(AppUser user) {
        TenantContext.set(company.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private String bearer(AppUser user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
