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
import ro.ecoregistru.enums.DeadlineRecurrence;
import ro.ecoregistru.enums.DeadlineStatus;
import ro.ecoregistru.enums.ReportType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.ReportingDeadlineRepository;
import ro.ecoregistru.service.DeadlineService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Termene proprii (V81, Andreea 29.09.2026): firma își adaugă singură măsurătorile de zgomot, apă,
 * emisii. Adăugare, repetare la bifare, modificare, ștergere, și ce nu se poate face pe cele din lege.
 */
@SpringBootTest(properties = "app.deadlines.missed-shown-from=2026-01-01")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class CustomDeadlineIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired ReportingDeadlineRepository deadlineRepository;
    @Autowired DeadlineService deadlineService;

    private static final LocalDate TODAY = DeadlineService.today();

    @Test
    void aCompanyAddsItsOwnDeadlineAndSeesItNextToTheLegalOnes() throws Exception {
        Tenant t = newTenant();
        LocalDate due = TODAY.plusDays(20);

        mockMvc.perform(post("/api/v1/deadlines").header("Authorization", "Bearer " + t.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("  Măsurători de zgomot ", due, "ANNUAL", "Laboratorul Eco Test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportType", is("CUSTOM")))
                .andExpect(jsonPath("$.title", is("Măsurători de zgomot")))
                .andExpect(jsonPath("$.recurrence", is("ANNUAL")))
                .andExpect(jsonPath("$.details", is("Laboratorul Eco Test")))
                .andExpect(jsonPath("$.status", is("UPCOMING")));

        mockMvc.perform(get("/api/v1/deadlines").param("year", String.valueOf(due.getYear()))
                        .header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reportType == 'CUSTOM')].title").value("Măsurători de zgomot"));
    }

    /** Zgomotul și apa, măsurate de același laborator în aceeași zi: amândouă au loc. */
    @Test
    void twoOwnDeadlinesCanFallOnTheSameDay() throws Exception {
        Tenant t = newTenant();
        LocalDate due = TODAY.plusDays(10);
        create(t, "Zgomot", due, "ONCE");
        create(t, "Apă uzată", due, "ONCE");

        assertThat(custom(t)).extracting(ReportingDeadline::getTitle).containsExactlyInAnyOrder("Zgomot", "Apă uzată");
    }

    @Test
    void aDateInThePastIsRefused() throws Exception {
        Tenant t = newTenant();
        mockMvc.perform(post("/api/v1/deadlines").header("Authorization", "Bearer " + t.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Emisii", TODAY.minusDays(1), "ONCE", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("deadline.date.past")));
        assertThat(custom(t)).isEmpty();
    }

    @Test
    void aTitleIsRequired() throws Exception {
        Tenant t = newTenant();
        mockMvc.perform(post("/api/v1/deadlines").header("Authorization", "Bearer " + t.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("  ", TODAY.plusDays(3), "ONCE", null)))
                .andExpect(status().isUnprocessableEntity());
        assertThat(custom(t)).isEmpty();
    }

    /** Bifată, o măsurătoare anuală își creează apariția de peste un an, cu același nume și detalii. */
    @Test
    void completingARepeatingOneBringsTheNextOccurrence() throws Exception {
        Tenant t = newTenant();
        LocalDate due = TODAY.plusDays(5);
        UUID id = create(t, "Analize apă", due, "ANNUAL");

        complete(t, id);

        assertThat(custom(t)).extracting(ReportingDeadline::getDueDate, ReportingDeadline::getStatus)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(due, DeadlineStatus.DONE),
                        org.assertj.core.groups.Tuple.tuple(due.plusYears(1), DeadlineStatus.UPCOMING));
        ReportingDeadline next = custom(t).stream().filter(d -> d.getStatus() == DeadlineStatus.UPCOMING)
                .findFirst().orElseThrow();
        assertThat(next.getTitle()).isEqualTo("Analize apă");
        assertThat(next.getDetails()).isEqualTo("detalii");
        assertThat(next.getRecurrence()).isEqualTo(DeadlineRecurrence.ANNUAL);

        // Redeschis și bifat din nou: nu apare o a doua apariție pe aceeași dată.
        mockMvc.perform(post("/api/v1/deadlines/" + id + "/reopen").header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk());
        complete(t, id);
        assertThat(custom(t)).hasSize(2);
    }

    @Test
    void completingAOneOffBringsNothing() throws Exception {
        Tenant t = newTenant();
        UUID id = create(t, "Reautorizare", TODAY.plusDays(40), "ONCE");
        complete(t, id);
        assertThat(custom(t)).hasSize(1);
    }

    /** Bifată cu trei luni întârziere, o lunară sare peste lunile trecute: următoarea nu e „depășită” din prima zi. */
    @Test
    void aLateTickSkipsThePeriodsAlreadyPast() throws Exception {
        Tenant t = newTenant();
        ReportingDeadline late = deadlineRepository.save(ReportingDeadline.builder()
                .company(t.company).reportType(ReportType.CUSTOM).dueDate(TODAY.minusMonths(3))
                .status(DeadlineStatus.UPCOMING).title("Citire contor").recurrence(DeadlineRecurrence.MONTHLY)
                .seriesId(UUID.randomUUID()).createdAt(Instant.now()).build());

        complete(t, late.getId());

        LocalDate next = custom(t).stream().filter(d -> d.getStatus() == DeadlineStatus.UPCOMING)
                .findFirst().orElseThrow().getDueDate();
        assertThat(next).isAfterOrEqualTo(TODAY).isBefore(TODAY.plusMonths(1).plusDays(1));
    }

    /** Mutat pe altă dată, termenul primește din nou mementourile: cele trimise erau pentru data veche. */
    @Test
    void editingMovesTheDateAndResetsTheReminders() throws Exception {
        Tenant t = newTenant();
        UUID id = create(t, "Zgomot", TODAY.plusDays(3), "ONCE");
        ReportingDeadline row = deadlineRepository.findById(id).orElseThrow();
        row.setWarned7Days(true);
        row.setWarned1Day(true);
        deadlineRepository.save(row);

        mockMvc.perform(put("/api/v1/deadlines/" + id).header("Authorization", "Bearer " + t.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Zgomot la hală", TODAY.plusDays(30), "SEMIANNUAL", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Zgomot la hală")))
                .andExpect(jsonPath("$.recurrence", is("SEMIANNUAL")));

        ReportingDeadline saved = deadlineRepository.findById(id).orElseThrow();
        assertThat(saved.getDueDate()).isEqualTo(TODAY.plusDays(30));
        assertThat(saved.getDetails()).isNull();
        assertThat(saved.isWarned7Days()).isFalse();
        assertThat(saved.isWarned1Day()).isFalse();
    }

    /** Cele din lege vin din profil: nu se modifică și nu se șterg, se bifează. */
    @Test
    void legalDeadlinesCannotBeEditedOrDeleted() throws Exception {
        Tenant t = newTenant();
        deadlineService.ensureUpcoming(t.company.getId(), TODAY);
        ReportingDeadline sim = deadlineRepository.findAllByCompany_IdAndDueDateBetweenOrderByDueDateAsc(
                        t.company.getId(), TODAY, TODAY.plusYears(3)).stream()
                .filter(d -> d.getReportType() == ReportType.SIM_ANNUAL).findFirst().orElseThrow();

        mockMvc.perform(put("/api/v1/deadlines/" + sim.getId()).header("Authorization", "Bearer " + t.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Altceva", TODAY.plusDays(5), "ONCE", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("deadline.not.custom")));
        mockMvc.perform(delete("/api/v1/deadlines/" + sim.getId()).header("Authorization", "Bearer " + t.token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("deadline.not.custom")));
        assertThat(deadlineRepository.findById(sim.getId())).isPresent();
    }

    /** Ștergerea ia apariția deschisă; bifările de dinainte rămân, ca istorie. */
    @Test
    void deletingKeepsTheTickedHistory() throws Exception {
        Tenant t = newTenant();
        UUID first = create(t, "Emisii", TODAY.plusDays(2), "QUARTERLY");
        complete(t, first);
        ReportingDeadline open = custom(t).stream().filter(d -> d.getStatus() == DeadlineStatus.UPCOMING)
                .findFirst().orElseThrow();

        mockMvc.perform(delete("/api/v1/deadlines/" + open.getId()).header("Authorization", "Bearer " + t.token))
                .andExpect(status().isNoContent());

        assertThat(custom(t)).extracting(ReportingDeadline::getId).containsExactly(first);
    }

    @Test
    void anotherCompanyCannotTouchIt() throws Exception {
        Tenant a = newTenant();
        Tenant b = newTenant();
        UUID id = create(a, "Zgomot", TODAY.plusDays(9), "ONCE");

        mockMvc.perform(delete("/api/v1/deadlines/" + id).header("Authorization", "Bearer " + b.token))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/deadlines/" + id).header("Authorization", "Bearer " + b.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("X", TODAY.plusDays(9), "ONCE", null)))
                .andExpect(status().isNotFound());
        assertThat(custom(a)).hasSize(1);
    }

    @Test
    void aViewerCannotAdd() throws Exception {
        String viewerToken = jwtService.generateToken(appUserRepository.findByEmail("viewer@demo.ro").orElseThrow());
        mockMvc.perform(post("/api/v1/deadlines").header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Zgomot", TODAY.plusDays(9), "ONCE", null)))
                .andExpect(status().isForbidden());
    }

    /** Generarea din profil nu se atinge de termenele proprii. */
    @Test
    void regenerationLeavesOwnDeadlinesAlone() throws Exception {
        Tenant t = newTenant();
        create(t, "Zgomot", TODAY.plusDays(9), "ONCE");
        mockMvc.perform(post("/api/v1/deadlines/regenerate").header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk());
        assertThat(custom(t)).hasSize(1);
    }

    // ---------- helpers ----------

    private UUID create(Tenant t, String title, LocalDate due, String recurrence) throws Exception {
        String json = mockMvc.perform(post("/api/v1/deadlines").header("Authorization", "Bearer " + t.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(title, due, recurrence, "detalii")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id"));
    }

    private void complete(Tenant t, UUID id) throws Exception {
        mockMvc.perform(post("/api/v1/deadlines/" + id + "/complete").header("Authorization", "Bearer " + t.token))
                .andExpect(status().isOk());
    }

    private List<ReportingDeadline> custom(Tenant t) {
        return deadlineRepository.findAllByCompany_IdAndDueDateBetweenOrderByDueDateAsc(
                        t.company.getId(), LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1)).stream()
                .filter(ReportingDeadline::isCustom).toList();
    }

    private static String body(String title, LocalDate due, String recurrence, String details) {
        return "{\"title\":\"" + title + "\",\"dueDate\":\"" + due + "\",\"recurrence\":\"" + recurrence + "\""
                + (details == null ? "" : ",\"details\":\"" + details + "\"") + "}";
    }

    private Tenant newTenant() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Proprii " + suffix).cui("ROP" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("p-admin+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        return new Tenant(company, jwtService.generateToken(admin));
    }

    private record Tenant(Company company, String token) {}
}
