package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.WeighingLinesRequest;
import ro.ecoregistru.controller.request.WeighingLinesRequest.Line;
import ro.ecoregistru.controller.request.WeighingOperationRequest;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WasteArticle;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteArticleRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.WeighingOperationService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.ecoregistru.enums.WeighingOperationType.IN;

/**
 * Aceeași măsurătoare ca {@code PerformanceIT}, pe lista ecranului „Cântar” (D1.15): <b>interogările
 * unei liste nu cresc cu numărul de rânduri</b>. E singura afirmație care e o proprietate, nu un prag.
 *
 * <p>Există fiindcă răspunsul scrie pe fiecare linie codul de deșeu și sortimentul, iar amândouă sunt
 * legături leneșe — exact forma lui BUG-016.
 *
 * <p><b>Ce s-a măsurat, ca să nu se creadă altceva</b> (16.09.2026): numărul nu crește nici fără fetch
 * join, fiindcă {@code default_batch_fetch_size: 100} (pus tot la BUG-016) aduce toate codurile și
 * sortimentele paginii în două interogări, oricâte rânduri ar fi. Fetch-ul din repository scoate chiar
 * și cele două: <b>3 interogări cu el, 5 fără</b>, la 3 ca și la 15 operațiuni, fiecare cu sortimentul
 * și codul ei. Deci proba apără proprietatea (nu crește cu rândurile), nu fetch-ul în sine; iar dacă
 * cineva adaugă pe rând o citire care nu se poate grupa, aici cade.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DepotListPerformanceIT {

    private static final LocalDate DAY = LocalDate.of(2031, 3, 10);

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired WeighingOperationService service;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteArticleRepository articleRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;

    Company company;
    AppUser admin;
    String token;
    WorkPoint depot;
    Partner partner;
    /** Codurile catalogului, citite o dată: fiecare linie primește altul, vezi {@link #seed(int)}. */
    List<ro.ecoregistru.entity.WasteCode> codes;
    int seeded;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        company = companyRepository.save(Company.builder()
                .name("Depozit perf SRL").cui("ROQ" + suffix).type(CompanyType.COLLECTOR)
                .active(true).createdAt(Instant.now()).build());
        admin = appUserRepository.save(AppUser.builder()
                .email("perf+" + suffix + "@demo.ro").password("x").role(Role.ADMIN)
                .company(company).enabled(true).createdAt(Instant.now()).build());
        token = jwtService.generateToken(admin);
        depot = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Depozit").active(true).createdAt(Instant.now()).build());
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Magazin Alfa SRL").cui("RO" + suffix)
                .type(PartnerType.GENERATOR).packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .active(true).createdAt(Instant.now()).build());
        codes = wasteCodeRepository.findAll();
        TenantContext.set(company.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void theOperationListCostsTheSameStatementsWhateverItsLength() throws Exception {
        seed(3);
        String url = "/api/v1/weighing-operations?year=2031&month=3";
        long three = statements(url);

        seed(12);
        long fifteen = statements(url);

        System.out.printf("[cântar] 3 operațiuni → %d interogări, 15 → %d%n", three, fifteen);
        assertThat(fifteen).as("interogări pentru 15 operațiuni față de 3").isEqualTo(three);
    }

    // --- helpers ---

    /** Câte interogări costă un GET, cu contextul deja cald (a doua rulare). */
    private long statements(String url) throws Exception {
        call(url);
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        call(url);
        return stats.getPrepareStatementCount();
    }

    private void call(String url) throws Exception {
        mockMvc.perform(get(url).header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", company.getId().toString()))
                .andExpect(status().isOk());
    }

    /**
     * Operațiuni finalizate, fiecare cu două linii, și <b>fiecare linie cu alt sortiment și alt cod</b>.
     * Asta contează: cu două sortimente refolosite, lazy-loadul le-ar găsi în sesiune după primele două
     * și proba ar trece și fără fetch — măsurat, 5 interogări și la 3, și la 15 operațiuni.
     */
    private void seed(int count) {
        // `TenantFilter` golește contextul la sfârșitul fiecărei cereri MockMvc, deci se pune la loc.
        TenantContext.set(company.getId());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
        for (int i = 0; i < count; i++) {
            WasteArticle first = article();
            WasteArticle second = article();
            UUID id = service.create(new WeighingOperationRequest(IN, depot.getId(), DAY, partner.getId(),
                    null, null, null, null, null, null, null, null, null, null)).id();
            service.replaceLines(id, new WeighingLinesRequest(null, null, List.of(
                    new Line(first.getId(), null, null, new BigDecimal("100"), null, new BigDecimal("0.5"), null, null),
                    new Line(second.getId(), null, null, new BigDecimal("50"), null, new BigDecimal("20"), null, null))));
            service.finalizeOperation(id);
        }
    }

    private WasteArticle article() {
        int index = seeded++;
        return articleRepository.save(WasteArticle.builder()
                .company(company).wasteCode(codes.get(index % codes.size()))
                .name("Sortiment " + index).active(true).createdAt(Instant.now()).build());
    }
}
