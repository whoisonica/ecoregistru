package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.AfmContribution;
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.DeadlineStatus;
import ro.ecoregistru.enums.MarketRole;
import ro.ecoregistru.enums.ReportType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ReportingDeadlineRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FAZA TERMENE / T4: reporting-deadline calendar over the real HTTP stack.
 * Covers additive+idempotent generation, the AFM-only-when-obligated rule, effective
 * OVERDUE derivation, completion, role gating and tenant isolation.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DeadlineIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired ReportingDeadlineRepository deadlineRepository;

    /** Fresh tenant so deadline counts are deterministic regardless of other test methods. */
    private TenantFixture newTenant(boolean afmObligation, AfmContribution... contributions) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Termene " + suffix).cui("ROT" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(afmObligation)
                .afmContributions(new java.util.LinkedHashSet<>(java.util.List.of(contributions)))
                .createdAt(Instant.now()).build());
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("t-admin+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        return new TenantFixture(company, jwtService.generateToken(admin));
    }

    @Test
    void generatesSimPlusTwelveAfmForAnObligatedCompany() throws Exception {
        TenantFixture t = newTenant(true);
        mockMvc.perform(post("/api/v1/deadlines/regenerate").param("year", "2026")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated", is(13))); // 1 SIM + 12 AFM

        mockMvc.perform(get("/api/v1/deadlines").param("year", "2026")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(13)));
    }

    @Test
    void generatesOnlySimForANonObligatedCompany() throws Exception {
        TenantFixture t = newTenant(false);
        mockMvc.perform(post("/api/v1/deadlines/regenerate").param("year", "2026")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated", is(1))); // SIM only

        mockMvc.perform(get("/api/v1/deadlines").param("year", "2026")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].reportType", is("SIM_ANNUAL")))
                .andExpect(jsonPath("$[0].dueDate", is("2026-03-15")));
    }

    // ---------- The three cadences of OUG 196/2005 art. 11 ----------

    /**
     * The wrong output this slice was written to fix: a company whose only Environment Fund
     * contribution is the yearly packaging one used to get <b>twelve</b> monthly deadlines. It now
     * gets one, on 25 January — not 15 March, and not monthly.
     */
    @Test
    void packagingOnlyGetsOneDeadlineOn25January() throws Exception {
        TenantFixture t = newTenant(true, AfmContribution.PACKAGING);
        regenerate(t.token, 2026);

        mockMvc.perform(get("/api/v1/deadlines").param("year", "2026")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2))) // SIM + one AFM
                .andExpect(jsonPath("$[?(@.reportType == 'AFM_ANNUAL' && @.dueDate == '2026-01-25')]")
                        .exists())
                .andExpect(jsonPath("$[?(@.reportType == 'AFM_MONTHLY')]").doesNotExist());
    }

    /** The 2% a collector withholds at source is monthly, so this one really is twelve. */
    @Test
    void theWithheldTwoPercentIsMonthly() throws Exception {
        TenantFixture t = newTenant(false, AfmContribution.WITHHOLDING_2_PERCENT);
        mockMvc.perform(post("/api/v1/deadlines/regenerate").param("year", "2026")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated", is(13))); // 1 SIM + 12 monthly
    }

    /** The circular-economy contribution of a landfill: four, after each quarter. */
    @Test
    void theCircularEconomyContributionIsQuarterly() throws Exception {
        TenantFixture t = newTenant(false, AfmContribution.CIRCULAR_ECONOMY);
        regenerate(t.token, 2026);

        mockMvc.perform(get("/api/v1/deadlines").param("year", "2026")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(5))) // SIM + four quarters
                .andExpect(jsonPath("$[?(@.reportType == 'AFM_QUARTERLY' && @.dueDate == '2026-04-25')]")
                        .exists())
                .andExpect(jsonPath("$[?(@.reportType == 'AFM_QUARTERLY' && @.dueDate == '2026-10-25')]")
                        .exists());
    }

    /**
     * An account nobody has filled in keeps exactly what it had: the flag alone still means twelve
     * monthly deadlines. Switching an alert off on an assumption is worse than leaving one that is
     * too loud, so the legacy path fades out as accounts get answered — not all at once.
     */
    @Test
    void anUnansweredAccountKeepsTheOldMonthlyDeadline() throws Exception {
        TenantFixture t = newTenant(true);
        mockMvc.perform(post("/api/v1/deadlines/regenerate").param("year", "2026")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated", is(13)));
    }

    @Test
    void regenerationIsIdempotent() throws Exception {
        TenantFixture t = newTenant(true);
        regenerate(t.token, 2026); // first run creates 13
        mockMvc.perform(post("/api/v1/deadlines/regenerate").param("year", "2026")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated", is(0))); // nothing new the second time
    }

    @Test
    void pastDueUncompletedDeadlineReadsAsOverdue() throws Exception {
        TenantFixture t = newTenant(false);
        // A deadline that fell due yesterday, still not done.
        deadlineRepository.save(ReportingDeadline.builder()
                .company(t.company).reportType(ReportType.OTHER)
                .dueDate(LocalDate.now().minusDays(1)).status(DeadlineStatus.UPCOMING)
                .warned7Days(false).warned1Day(false).createdAt(Instant.now()).build());

        int year = LocalDate.now().minusDays(1).getYear();
        mockMvc.perform(get("/api/v1/deadlines").param("year", String.valueOf(year))
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reportType=='OTHER')].status", is(java.util.List.of("OVERDUE"))));
    }

    @Test
    void completeMarksDoneAndStoresNote() throws Exception {
        TenantFixture t = newTenant(false);
        regenerate(t.token, 2026);
        UUID id = deadlineRepository
                .findAllByCompany_IdAndDueDateBetweenOrderByDueDateAsc(
                        t.company.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
                .get(0).getId();

        mockMvc.perform(post("/api/v1/deadlines/" + id + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Depus SIM la ANPM\"}")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")))
                .andExpect(jsonPath("$.completionNote", is("Depus SIM la ANPM")))
                .andExpect(jsonPath("$.completedAt").exists());
    }

    @Test
    void viewerCannotGenerate() throws Exception {
        String viewerToken = jwtService.generateToken(
                appUserRepository.findByEmail("viewer@demo.ro").orElseThrow());
        mockMvc.perform(post("/api/v1/deadlines/regenerate").param("year", "2026")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$['error-code']", is("access.denied")));
    }

    @Test
    void deadlinesAreTenantScoped() throws Exception {
        TenantFixture a = newTenant(true);
        TenantFixture b = newTenant(true);
        regenerate(a.token, 2027);
        // B has generated nothing for 2027 -> B's list is empty even though A has 13.
        mockMvc.perform(get("/api/v1/deadlines").param("year", "2027")
                        .header("Authorization", "Bearer " + b.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    // ---------- 25 February: the packaging report of Ordinul 794/2012 (audit point 3) ----------

    /**
     * A producer files the packaging report at the county agency by 25 February (art. 1 + art. 6).
     * Before 04.09.2026 the application built that document and even named the term in the
     * audit-file README, but generated no deadline for it at all.
     */
    @Test
    void aProducerGetsThe25FebruaryPackagingDeadline() throws Exception {
        TenantFixture t = newTenantWithRoles(MarketRole.PRODUCER);
        regenerate(t.token, 2026);

        mockMvc.perform(get("/api/v1/deadlines").param("year", "2026")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reportType == 'PACKAGING_ANNUAL' "
                        + "&& @.dueDate == '2026-02-25')]").exists());
    }

    /**
     * A trader sells goods somebody else packaged, so it never introduced the packaging on the
     * national market and does not file — Legea 249/2015, and {@code MarketRole#putsPackagingOnMarket}.
     */
    @Test
    void aTraderGetsNoPackagingDeadline() throws Exception {
        TenantFixture t = newTenantWithRoles(MarketRole.TRADER);
        regenerate(t.token, 2026);

        mockMvc.perform(get("/api/v1/deadlines").param("year", "2026")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reportType == 'PACKAGING_ANNUAL')]").doesNotExist());
    }

    /**
     * And an account that never answered the question gets nothing either — deliberately the
     * opposite of how an empty profile treats <em>screens</em>, which stay fully offered
     * (decizia 6). An alert asserts something about the client; a screen only offers. Sending a
     * false reminder is the mistake {@code V21} spent a migration undoing, so silence wins here.
     */
    @Test
    void anUnansweredProfileGetsNoPackagingDeadline() throws Exception {
        TenantFixture t = newTenant(false); // no market roles at all
        regenerate(t.token, 2026);

        mockMvc.perform(get("/api/v1/deadlines").param("year", "2026")
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reportType == 'PACKAGING_ANNUAL')]").doesNotExist());
    }

    private TenantFixture newTenantWithRoles(MarketRole... roles) {
        TenantFixture t = newTenant(false);
        Company company = companyRepository.findById(t.company.getId()).orElseThrow();
        company.setMarketRoles(new java.util.LinkedHashSet<>(java.util.List.of(roles)));
        companyRepository.save(company);
        return t;
    }

    private void regenerate(String token, int year) throws Exception {
        mockMvc.perform(post("/api/v1/deadlines/regenerate").param("year", String.valueOf(year))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private record TenantFixture(Company company, String token) {}
}
