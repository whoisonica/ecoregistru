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

    // --- helpers ---

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
