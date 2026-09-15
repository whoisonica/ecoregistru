package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.CompanyRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest.Line;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.controller.response.WeighingOperationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.PriceVisibility;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.AuditLogRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.CompanyService;
import ro.ecoregistru.service.WeighingOperationService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.ecoregistru.enums.PriceVisibility.ADMIN_ONLY;
import static ro.ecoregistru.enums.PriceVisibility.COMPANY;
import static ro.ecoregistru.enums.PriceVisibility.NO_CONSULTANT;
import static ro.ecoregistru.enums.WeighingOperationType.IN;

/**
 * D1.8 — cine vede prețurile depozitului. Setarea e a firmei și o schimbă doar adminul ei; platforma
 * e tratată ca un consultant (deciziile proprietarului, 15.09.2026). Cine nu vede prețul primește
 * răspunsul fără el, iar o editare făcută de el nu îl schimbă și nu îl șterge.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PriceVisibilityIT {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 15);

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired WeighingOperationService service;
    @Autowired CompanyService companyService;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired AuditLogRepository auditLogRepository;

    Company company;
    Map<Role, AppUser> users;
    WorkPoint depot;
    Partner partner;
    WasteArticle copper;
    WasteArticle cardboard;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.builder()
                .name("Prețuri " + suffix() + " SRL").cui("RO" + ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999))
                .type(CompanyType.COLLECTOR).active(true).createdAt(Instant.now()).build());
        // Consultantul are cabinet, nu firmă, iar platforma n-are niciuna: nu se salvează. Serviciile
        // citesc doar rolul din principal.
        users = Map.of(
                Role.ADMIN, user(Role.ADMIN),
                Role.OPERATOR, user(Role.OPERATOR),
                Role.CLIENT_VIEWER, user(Role.CLIENT_VIEWER),
                Role.CONSULTANT, unsaved(Role.CONSULTANT),
                Role.PLATFORM_ADMIN, unsaved(Role.PLATFORM_ADMIN));
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit Baciu").active(true).createdAt(Instant.now()).build());
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin Alfa SRL").cui("RO" + suffix())
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        copper = article("Cupru", 0);
        cardboard = article("Carton", 1);
        actAs(Role.ADMIN);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    /** Matricea întreagă: 3 setări × 5 roluri, pe citirea unei operațiuni, pe listă și pe firma curentă. */
    @Test
    void theSettingDecidesWhoSeesThePrice() {
        UUID id = pricedOperation();
        Map<PriceVisibility, Set<Role>> seeing = Map.of(
                COMPANY, EnumSet.allOf(Role.class),
                NO_CONSULTANT, EnumSet.of(Role.ADMIN, Role.OPERATOR, Role.CLIENT_VIEWER),
                ADMIN_ONLY, EnumSet.of(Role.ADMIN));

        for (PriceVisibility visibility : PriceVisibility.values()) {
            setVisibility(visibility);
            for (Role role : Role.values()) {
                actAs(role);
                boolean expected = seeing.get(visibility).contains(role);
                String who = visibility + " × " + role;

                WeighingOperationResponse.Line line = service.get(id).lines().get(0);
                assertThat(line.unitPrice() != null).as(who + ": prețul").isEqualTo(expected);
                assertThat(line.totalValue() != null).as(who + ": valoarea").isEqualTo(expected);
                assertThat(line.finalKg()).as(who + ": cantitatea rămâne").isEqualByComparingTo("100");
                assertThat(service.list().get(0).lines().get(0).unitPrice() != null).as(who + ": lista").isEqualTo(expected);
                assertThat(companyService.current().pricesVisible()).as(who + ": firma curentă").isEqualTo(expected);
            }
        }
    }

    /**
     * Operatorul nu vede prețul și trimite formularul fără el (sau cu altul): prețul salvat rămâne, pe
     * sortiment și în ordine, iar valoarea urmează cantitatea nouă.
     */
    @Test
    void anEditWithoutTheRightNeitherChangesNorClearsThePrice() {
        UUID id = service.create(head()).id();
        service.replaceLines(id, lines(
                new Line(copper.getId(), null, null, kg("100"), null, kg("40.5"), null, null),
                new Line(cardboard.getId(), null, null, kg("300"), null, kg("0.6"), null, null),
                new Line(copper.getId(), null, null, kg("50"), null, kg("38"), null, null)));
        setVisibility(ADMIN_ONLY);

        actAs(Role.OPERATOR);
        WeighingOperationResponse seen = service.replaceLines(id, lines(
                new Line(copper.getId(), null, null, kg("120"), null, kg("999"), null, null),
                new Line(cardboard.getId(), null, null, kg("300"), null, null, null, null),
                new Line(copper.getId(), null, null, kg("50"), null, null, null, null),
                new Line(copper.getId(), null, null, kg("10"), null, kg("1"), null, null)));

        assertThat(seen.lines()).allSatisfy(l -> {
            assertThat(l.unitPrice()).isNull();
            assertThat(l.totalValue()).isNull();
        });
        List<WasteMovement> rows = movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id);
        assertThat(rows).extracting(WasteMovement::getUnitPrice)
                .usingElementComparator(PriceVisibilityIT::compareNullable)
                .containsExactly(kg("40.5"), kg("0.6"), kg("38"), null);
        assertThat(rows).extracting(WasteMovement::getTotalValue)
                .usingElementComparator(PriceVisibilityIT::compareNullable)
                .containsExactly(kg("4860.00"), kg("180.00"), kg("1900.00"), null);
    }

    /** Controlul pozitiv al celui de mai sus: cine vede prețul chiar îl schimbă și îl șterge. */
    @Test
    void whoeverSeesThePriceChangesOrClearsIt() {
        UUID id = pricedOperation();

        setVisibility(ADMIN_ONLY);
        actAs(Role.ADMIN);
        service.replaceLines(id, lines(new Line(copper.getId(), null, null, kg("100"), null, null, null, null)));
        assertThat(movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id).get(0).getUnitPrice()).isNull();

        setVisibility(COMPANY);
        actAs(Role.OPERATOR);
        service.replaceLines(id, lines(new Line(copper.getId(), null, null, kg("100"), null, kg("12"), null, null)));
        assertThat(movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id).get(0).getUnitPrice())
                .isEqualByComparingTo("12");
    }

    /** Doar adminul firmei schimbă setarea; profilul editat de platformă n-o atinge. */
    @Test
    void onlyTheCompanyAdminChangesTheSetting() {
        for (Role role : List.of(Role.CONSULTANT, Role.PLATFORM_ADMIN, Role.OPERATOR, Role.CLIENT_VIEWER)) {
            actAs(role);
            assertThatThrownBy(() -> companyService.updatePriceVisibility(ADMIN_ONLY)).as(role.name())
                    .isInstanceOf(AccessDeniedException.class);
        }
        assertThat(storedVisibility()).isEqualTo(COMPANY);

        actAs(Role.ADMIN);
        assertThatThrownBy(() -> companyService.updatePriceVisibility(null)).isInstanceOf(BusinessException.class);
        var saved = companyService.updatePriceVisibility(ADMIN_ONLY);
        assertThat(saved.priceVisibility()).isEqualTo(ADMIN_ONLY);
        assertThat(saved.pricesVisible()).isTrue();
        assertThat(storedVisibility()).isEqualTo(ADMIN_ONLY);

        actAs(Role.PLATFORM_ADMIN);
        var edited = companyService.update(company.getId(), profile("Prețuri redenumită SRL"));
        assertThat(edited.name()).isEqualTo("Prețuri redenumită SRL");
        assertThat(edited.pricesVisible()).as("platforma nu vede la „doar admin”").isFalse();
        assertThat(storedVisibility()).as("editarea profilului păstrează setarea").isEqualTo(ADMIN_ONLY);
    }

    /** Aceleași reguli pe HTTP: 403 la setare pentru operator și vizualizator, iar răspunsul fără preț. */
    @Test
    void overHttp() throws Exception {
        UUID id = pricedOperation();
        // Autentificarea de pe thread ar opri filtrul JWT, iar 403-urile n-ar dovedi nimic (capcana din D1.5).
        SecurityContextHolder.clearContext();
        TenantContext.clear();

        for (Role role : List.of(Role.OPERATOR, Role.CLIENT_VIEWER)) {
            mockMvc.perform(put("/api/v1/companies/current/price-visibility")
                            .header("Authorization", "Bearer " + jwtService.generateToken(users.get(role)))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"priceVisibility\":\"COMPANY\"}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(put("/api/v1/companies/current/price-visibility")
                        .header("Authorization", "Bearer " + jwtService.generateToken(users.get(Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"priceVisibility\":\"ADMIN_ONLY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceVisibility").value("ADMIN_ONLY"));
        assertThat(storedVisibility()).isEqualTo(ADMIN_ONLY);

        String viewerSees = body("/api/v1/weighing-operations/" + id, Role.CLIENT_VIEWER);
        assertThat(viewerSees).contains("Cupru").doesNotContain("40.5").doesNotContain("4050");
        assertThat(body("/api/v1/weighing-operations", Role.OPERATOR)).contains("Cupru").doesNotContain("40.5");
        assertThat(body("/api/v1/weighing-operations/" + id, Role.ADMIN)).contains("40.5").contains("4050");
    }

    /**
     * Jurnalul de audit îl citesc și consultantul, și platforma: fapta rămâne, valoarea nu. La creare
     * jurnalul nu scrie valori (`onSave`), iar `replaceLines` șterge și inserează, deci prețul ar
     * ajunge acolo doar printr-o linie schimbată pe loc; asta se probează.
     */
    @Test
    void theAuditLogKeepsTheFactButNotThePrice() {
        UUID id = pricedOperation();
        WasteMovement row = movementRepository.findAllByWeighingOperation_IdOrderByLineNoAsc(id).get(0);
        row.setUnitPrice(kg("41.75"));
        row.setTotalValue(kg("4175.00"));
        movementRepository.saveAndFlush(row);

        List<String> changes = auditLogRepository.findAll().stream()
                .filter(l -> row.getId().equals(l.getEntityId()))
                .map(l -> String.valueOf(l.getChanges()))
                .toList();
        assertThat(String.join("", changes)).as("controlul pozitiv: schimbarea prețului chiar e scrisă")
                .contains("unitPrice").contains("•••");
        assertThat(changes).noneMatch(c -> c.contains("40.5") || c.contains("41.75")
                || c.contains("4050") || c.contains("4175"));
    }

    // --- helpers ---

    private UUID pricedOperation() {
        actAs(Role.ADMIN);
        UUID id = service.create(head()).id();
        service.replaceLines(id, lines(new Line(copper.getId(), null, null, kg("100"), null, kg("40.5"), null, null)));
        return id;
    }

    private String body(String url, Role role) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + jwtService.generateToken(users.get(role))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private void setVisibility(PriceVisibility visibility) {
        Company fresh = companyRepository.findById(company.getId()).orElseThrow();
        fresh.setPriceVisibility(visibility);
        companyRepository.saveAndFlush(fresh);
    }

    private PriceVisibility storedVisibility() {
        return companyRepository.findById(company.getId()).orElseThrow().getPriceVisibility();
    }

    private CompanyRequest profile(String name) {
        return new CompanyRequest(name, company.getCui(), CompanyType.COLLECTOR, false,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null,
                null, null, null, null,
                null, null, null, null,
                null);
    }

    private WeighingOperationRequest head() {
        return new WeighingOperationRequest(IN, depot.getId(), DAY, partner.getId(),
                null, null, null, null, null, null, null);
    }

    private static WeighingLinesRequest lines(Line... lines) {
        return new WeighingLinesRequest(null, null, List.of(lines));
    }

    private WasteArticle article(String name, int codeIndex) {
        return articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(wasteCodeRepository.findAll().get(codeIndex))
                .name(name).active(true).createdAt(Instant.now()).build());
    }

    private AppUser user(Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(role.name().toLowerCase() + "+" + suffix() + "@demo.ro").password("x")
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private static AppUser unsaved(Role role) {
        return AppUser.builder().id(UUID.randomUUID()).email(role.name().toLowerCase() + "@demo.ro")
                .role(role).enabled(true).build();
    }

    private void actAs(Role role) {
        TenantContext.set(company.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(users.get(role), null, List.of()));
    }

    private static int compareNullable(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b ? 0 : 1;
        }
        return a.compareTo(b);
    }

    private static BigDecimal kg(String value) {
        return new BigDecimal(value);
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
