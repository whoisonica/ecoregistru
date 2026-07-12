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
import ro.ecoregistru.entity.ReportingDeadline;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.DeadlineStatus;
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
    private TenantFixture newTenant(boolean afmObligation) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Termene " + suffix).cui("ROT" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(afmObligation).createdAt(Instant.now()).build());
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

    private void regenerate(String token, int year) throws Exception {
        mockMvc.perform(post("/api/v1/deadlines/regenerate").param("year", String.valueOf(year))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private record TenantFixture(Company company, String token) {}
}
