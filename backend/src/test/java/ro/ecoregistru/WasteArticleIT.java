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
import ro.ecoregistru.controller.request.WasteArticleRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest.Line;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.WasteArticleResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.NaturalPerson;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.ErrorMessageEnum;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.NaturalPersonRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.WasteArticleService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.ecoregistru.enums.WeighingOperationType.IN;

/**
 * D1.6 — catalogul de sortimente: propunerea „metal” din cod, unicitatea numelui, izolarea între firme
 * și bifa „interzis de la PF”, care se aplică la cântar (OUG 31/2011 art. 1 alin. (1)).
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class WasteArticleIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired WasteArticleService service;
    @Autowired WeighingOperationService operations;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired NaturalPersonRepository personRepository;

    Company company;
    AppUser admin;
    WorkPoint depot;
    WasteCode copper;
    WasteCode paper;

    @BeforeEach
    void setUp() {
        company = company("Catalog");
        admin = user(company, Role.ADMIN);
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit Baciu").active(true).createdAt(Instant.now()).build());
        copper = wasteCodeRepository.findByCode("17 04 01").orElseThrow();
        paper = wasteCodeRepository.findByCode("20 01 01").orElseThrow();
        actAs(company, admin);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void metalIsProposedFromTheCodeWhenTheFormDoesNotSay() {
        assertThat(service.create(new WasteArticleRequest("Cupru", copper.getId(), null, false)).metal()).isTrue();
        assertThat(service.create(new WasteArticleRequest("Hârtie", paper.getId(), null, false)).metal()).isFalse();
    }

    @Test
    void theChoiceOfTheFormWinsOverTheProposal() {
        WasteArticleResponse saved = service.create(new WasteArticleRequest("Cupru murdar", copper.getId(), false, false));
        assertThat(saved.metal()).isFalse();
        assertThat(service.update(saved.id(), new WasteArticleRequest("Cupru murdar", paper.getId(), true, true)))
                .satisfies(a -> {
                    assertThat(a.metal()).isTrue();
                    assertThat(a.forbiddenFromIndividuals()).isTrue();
                    assertThat(a.wasteCode()).isEqualTo("20 01 01");
                });
    }

    @Test
    void theNameIsUniqueInTheFirmIgnoringCase() {
        WasteArticleResponse cupru = service.create(new WasteArticleRequest("Cupru", copper.getId(), null, false));

        assertThatThrownBy(() -> service.create(new WasteArticleRequest(" cupru ", copper.getId(), null, false)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorMessageEnum.WASTE_ARTICLE_NAME_TAKEN));
        // Propria denumire, cu altă scriere, nu se ciocnește cu ea însăși.
        assertThat(service.update(cupru.id(), new WasteArticleRequest("CUPRU", copper.getId(), null, false)).name())
                .isEqualTo("CUPRU");

        // Altă firmă poate avea același nume.
        Company other = company("Alt depozit");
        actAs(other, user(other, Role.ADMIN));
        assertThat(service.create(new WasteArticleRequest("Cupru", copper.getId(), null, false)).name()).isEqualTo("Cupru");
    }

    @Test
    void nameAndCodeAreRequired() {
        assertThatThrownBy(() -> service.create(new WasteArticleRequest("  ", copper.getId(), null, false)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorMessageEnum.WASTE_ARTICLE_NAME_REQUIRED));
        assertThatThrownBy(() -> service.create(new WasteArticleRequest("Cupru", null, null, false)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorMessageEnum.WASTE_ARTICLE_CODE_REQUIRED));
    }

    @Test
    void anotherFirmsArticleIsNotFoundAndNotListed() {
        UUID mine = service.create(new WasteArticleRequest("Cupru", copper.getId(), null, false)).id();

        Company other = company("Străin");
        actAs(other, user(other, Role.ADMIN));
        assertThat(service.list()).isEmpty();
        assertThatThrownBy(() -> service.update(mine, new WasteArticleRequest("Furat", copper.getId(), null, false)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.deactivate(mine)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deactivationKeepsTheArticleInTheListAndIsReversible() {
        UUID id = service.create(new WasteArticleRequest("Cupru", copper.getId(), null, false)).id();

        service.deactivate(id);
        assertThat(service.list()).singleElement().satisfies(a -> assertThat(a.active()).isFalse());
        service.reactivate(id);
        assertThat(service.list()).singleElement().satisfies(a -> assertThat(a.active()).isTrue());
    }

    /** Șina de cale ferată: de la un partener autorizat se primește, de la o persoană fizică nu. */
    @Test
    void anArticleForbiddenFromIndividualsIsRefusedOnlyWhenAPersonSells() {
        UUID rail = service.create(new WasteArticleRequest("Șină CF", copper.getId(), null, true)).id();
        UUID scrap = service.create(new WasteArticleRequest("Cupru casnic", copper.getId(), null, false)).id();

        // Identitate completă: la metal borderoul o cere (D1.7), iar aici se probează doar bifa „interzis”.
        NaturalPerson person = personRepository.save(NaturalPerson.builder()
                .company(company).name("Ion Popescu").cnp("1900101123457").identification("CJ 123456")
                .address("Cluj, str. X 1").active(true).createdAt(Instant.now()).build());
        UUID fromPerson = operations.create(new WeighingOperationRequest(IN, depot.getId(), LocalDate.of(2026, 9, 15),
                null, person.getId(), null, null, null, null, null, null, null, null, null)).id();

        assertThatThrownBy(() -> operations.replaceLines(fromPerson, lines(rail)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode())
                        .isEqualTo(ErrorMessageEnum.WEIGHING_LINE_ARTICLE_FORBIDDEN_FROM_INDIVIDUALS));
        // Control pozitiv: aceeași operațiune primește un sortiment permis.
        assertThat(operations.replaceLines(fromPerson, lines(scrap)).lines()).hasSize(1);

        Partner partner = partnerRepository.save(Partner.builder()
                .company(company).name("CFR Reciclare SA").cui("RO" + suffix())
                .type(PartnerType.COLLECTOR).supplier(true).packagingOrigin(PackagingOrigin.COLECTOR)
                .active(true).createdAt(Instant.now()).build());
        UUID fromPartner = operations.create(new WeighingOperationRequest(IN, depot.getId(), LocalDate.of(2026, 9, 15),
                partner.getId(), null, null, null, null, null, null, null, null, null, null)).id();
        assertThat(operations.replaceLines(fromPartner, lines(rail)).lines()).hasSize(1);
    }

    /** Oricine scrie își personalizează catalogul (decizia proprietarului, 15.09.2026); vizualizatorul nu. */
    @Test
    void everyWriterCustomizesTheCatalogButNotTheViewerOverHttp() throws Exception {
        AppUser operator = user(company, Role.OPERATOR);
        AppUser viewer = user(company, Role.CLIENT_VIEWER);
        String body = "{\"name\":\"Aluminiu\",\"wasteCodeId\":\"" + copper.getId() + "\",\"forbiddenFromIndividuals\":false}";
        // Capcana din WeighingOperationStatusIT: o autentificare lăsată pe thread oprește filtrul JWT.
        SecurityContextHolder.clearContext();
        TenantContext.clear();

        mockMvc.perform(post("/api/v1/waste-articles")
                        .header("Authorization", "Bearer " + jwtService.generateToken(viewer))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/waste-articles")
                        .header("Authorization", "Bearer " + jwtService.generateToken(operator))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metal").value(true))
                .andExpect(jsonPath("$.wasteCode").value("17 04 01"));
        // Viewerul citește lista: formularul de operațiune și Setările o afișează oricui din firmă.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/waste-articles")
                        .header("Authorization", "Bearer " + jwtService.generateToken(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aluminiu"));
    }

    // --- helpers ---

    private WeighingLinesRequest lines(UUID articleId) {
        return new WeighingLinesRequest(null, null, List.of(
                new Line(articleId, null, null, new BigDecimal("40"), null, null, null, null)));
    }

    private Company company(String name) {
        return companyRepository.save(Company.builder()
                .name(name + " " + suffix() + " SRL").cui("ROA" + suffix()).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Company owner, Role role) {
        return appUserRepository.save(AppUser.builder()
                .email("sortiment+" + suffix() + "@demo.ro").password("x")
                .role(role).company(owner).enabled(true).createdAt(Instant.now()).build());
    }

    private void actAs(Company owner, AppUser user) {
        TenantContext.set(owner.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
