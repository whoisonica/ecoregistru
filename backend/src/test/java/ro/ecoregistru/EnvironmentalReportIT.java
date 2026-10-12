package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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
import ro.ecoregistru.service.DeadlineService;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The raport anual de mediu (RAM, V86): the year's answers and lists, the authorization, its deadlines, the Word. */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class EnvironmentalReportIT {

    private static final int YEAR = 2025;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired ReportingDeadlineRepository deadlineRepository;
    @Autowired DeadlineService deadlineService;

    private String admin;
    private String viewer;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Mediu " + suffix).cui("ROM" + suffix)
                .type(CompanyType.GENERATOR)
                .address("Cluj-Napoca, str. Exemplu nr. 1")
                .contactName("Ion Popescu").contactRole("Manager Mediu")
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        admin = jwtService.generateToken(user(company, "adm", suffix, Role.ADMIN));
        viewer = jwtService.generateToken(user(company, "vw", suffix, Role.CLIENT_VIEWER));
    }

    private AppUser user(Company company, String tag, String suffix, Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(tag + "+" + suffix + "@demo.ro").password("x")
                .role(role).company(company).enabled(true)
                .createdAt(Instant.now()).build());
    }

    private ResultActions putTo(String path, String token, String body) throws Exception {
        return mockMvc.perform(put("/api/v1/environmental-report" + path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions report(int year) throws Exception {
        return mockMvc.perform(get("/api/v1/environmental-report?year=" + year)
                .header("Authorization", "Bearer " + admin));
    }

    private static final String PERMIT = """
            {"number":"17","issuer":"ANMAP – Direcția Județeană pentru Mediu Cluj","issuedOn":"2015-12-20",
             "revisedOn":"2022-03-10","visaOn":"2024-11-05","ramDueMonth":1,"ramDueDay":15,
             "copyTo":"Garda Națională de Mediu – Comisariatul Județean Cluj","administratorName":"Maria Ionescu"}""";

    private List<ReportingDeadline> open(ReportType type) {
        return deadlineRepository.findAll().stream()
                .filter(d -> d.getCompany().getId().equals(tenantId) && d.getReportType() == type
                        && d.getStatus() != DeadlineStatus.DONE)
                .toList();
    }

    @Test
    void anEmptyYearHasNoAnswersAndOffersTheDefaultText() throws Exception {
        report(YEAR).andExpect(status().isOk())
                .andExpect(jsonPath("$.year", is(YEAR)))
                .andExpect(jsonPath("$.incidents", nullValue()))
                .andExpect(jsonPath("$.inspected", nullValue()))
                .andExpect(jsonPath("$.otherRequirements", startsWith("Evidența gestiunii deșeurilor")))
                .andExpect(jsonPath("$.inspections", hasSize(0)))
                .andExpect(jsonPath("$.permit.ramDueMonth", is(1)))
                .andExpect(jsonPath("$.permit.ramDueDay", is(15)))
                .andExpect(jsonPath("$.permit.visaComputedMonth", nullValue()));
        report(2019).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("env.report.year.invalid")));
    }

    @Test
    void aSaveReplacesTheAnswersAndTheListsInOrder() throws Exception {
        putTo("", admin, """
                {"year":2025,"incidents":false,"complaints":true,"complaintsText":"O sesizare a unui vecin.",
                 "inspected":true,"damage":false,"otherRequirements":"Evidența se ține lunar.",
                 "inspections":[{"workPoint":"Cluj","institution":"Garda de Mediu","reportNumber":"118",
                                 "reportDate":"2025-06-03","measures":"Salubritate permanentă."},
                                {"workPoint":"Oradea","institution":"ISU","reportNumber":"21"}],
                 "monitorings":[{"workPoint":"Cluj","factor":"Zgomot","performedOn":"2025-11-14",
                                 "testReportNumber":"Z-0457","testReportDate":"2025-11-14",
                                 "result":"Fără depășiri."}]}""").andExpect(status().isOk());
        report(YEAR).andExpect(jsonPath("$.incidents", is(false)))
                .andExpect(jsonPath("$.complaintsText", is("O sesizare a unui vecin.")))
                .andExpect(jsonPath("$.otherRequirements", is("Evidența se ține lunar.")))
                .andExpect(jsonPath("$.inspections", hasSize(2)))
                .andExpect(jsonPath("$.inspections[1].institution", is("ISU")))
                .andExpect(jsonPath("$.monitorings[0].testReportNumber", is("Z-0457")));

        putTo("", admin, "{\"year\":2025,\"inspected\":false,\"inspections\":[],\"monitorings\":[]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspections", hasSize(0)))
                .andExpect(jsonPath("$.incidents", nullValue()))
                // A text emptied on purpose stays empty: the default is offered only to a year never saved.
                .andExpect(jsonPath("$.otherRequirements", nullValue()));
    }

    @Test
    void aViewerCannotWrite() throws Exception {
        putTo("", viewer, "{\"year\":2025}").andExpect(status().isForbidden());
        putTo("/permit?year=2025", viewer, PERMIT).andExpect(status().isForbidden());
    }

    @Test
    void thePermitBringsTheRamAndTheVisaDeadlines() throws Exception {
        assertThat(open(ReportType.ENVIRONMENTAL_REPORT)).isEmpty();
        putTo("/permit?year=2025", admin, PERMIT).andExpect(status().isOk())
                .andExpect(jsonPath("$.permit.issuer", is("ANMAP – Direcția Județeană pentru Mediu Cluj")))
                // 20 December − 60 days = 21 October (Ordinul 1150/2020 art. 5 alin. (4)).
                .andExpect(jsonPath("$.permit.visaComputedMonth", is(10)))
                .andExpect(jsonPath("$.permit.visaComputedDay", is(21)));
        LocalDate today = DeadlineService.today();
        assertThat(open(ReportType.ENVIRONMENTAL_REPORT)).singleElement()
                .extracting(ReportingDeadline::getDueDate).isEqualTo(next(MonthDay.of(1, 15), today));
        assertThat(open(ReportType.PERMIT_VISA)).singleElement()
                .extracting(ReportingDeadline::getDueDate).isEqualTo(next(MonthDay.of(10, 21), today));

        // Another RAM day and a visa day written by hand: the open deadlines move, they do not pile up.
        putTo("/permit?year=2025", admin, PERMIT.replace("\"ramDueDay\":15", "\"ramDueDay\":31,\"visaDueMonth\":9,"
                + "\"visaDueDay\":1").replace("\"ramDueMonth\":1", "\"ramDueMonth\":3")).andExpect(status().isOk());
        // A day moved before today leaves this year's one overdue (aDayMovedIntoThePastLeavesTheYearOverdue), so only
        // the coming ones are counted here: which side of today the new day falls on depends on the calendar.
        assertThat(open(ReportType.ENVIRONMENTAL_REPORT)).filteredOn(d -> !d.getDueDate().isBefore(today))
                .singleElement().extracting(ReportingDeadline::getDueDate).isEqualTo(next(MonthDay.of(3, 31), today));
        assertThat(open(ReportType.PERMIT_VISA)).filteredOn(d -> !d.getDueDate().isBefore(today))
                .singleElement().extracting(ReportingDeadline::getDueDate).isEqualTo(next(MonthDay.of(9, 1), today));
        assertThat(open(ReportType.PERMIT_VISA)).filteredOn(d -> d.getDueDate().isBefore(today))
                .allSatisfy(d -> assertThat(d.getDueDate().getYear()).isEqualTo(today.getYear()));

        // No authorization number, no RAM and no visa.
        putTo("/permit?year=2025", admin, PERMIT.replace("\"number\":\"17\"", "\"number\":\"\"")).andExpect(status().isOk());
        assertThat(open(ReportType.ENVIRONMENTAL_REPORT)).isEmpty();
        assertThat(open(ReportType.PERMIT_VISA)).isEmpty();
    }

    @Test
    void aDayThatDoesNotExistIs422() throws Exception {
        putTo("/permit?year=2025", admin, PERMIT.replace("\"ramDueMonth\":1", "\"ramDueMonth\":2")
                .replace("\"ramDueDay\":15", "\"ramDueDay\":30"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("env.report.due.invalid")));
        putTo("/permit?year=2025", admin, PERMIT.replace("\"ramDueDay\":15", "\"ramDueDay\":15,\"visaDueMonth\":9"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void theWordDocumentHasTheLetterTheAnswersAndTheSignature() throws Exception {
        putTo("/permit?year=2025", admin, PERMIT).andExpect(status().isOk());
        putTo("", admin, """
                {"year":2025,"incidents":false,"inspected":true,"damage":false,
                 "inspections":[{"workPoint":"Cluj","institution":"Garda de Mediu","reportNumber":"118",
                                 "reportDate":"2025-06-03","measures":"Salubritate permanentă."}],
                 "monitorings":[{"factor":"Zgomot","performedOn":"2025-11-14","testReportNumber":"Z-0457"}]}""")
                .andExpect(status().isOk());
        byte[] body = mockMvc.perform(get("/api/v1/environmental-report/document?year=2025")
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String text;
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(body));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            text = extractor.getText();
        }
        assertThat(text)
                .contains("Nr. înregistrare ........../...............")
                .contains("Către,", "ANMAP – Direcția Județeană pentru Mediu Cluj")
                .contains("Spre știința Garda Națională de Mediu – Comisariatul Județean Cluj")
                .contains("autorizației de mediu nr. 17 din 20.12.2015, revizuită la 10.03.2022, cu viza anuală din 05.11.2024")
                .contains("RAPORT ANUAL DE MEDIU")
                .contains("În anul 2025 nu s-au înregistrat incidente, accidente sau poluări accidentale.")
                .contains("118 / 03.06.2025", "Salubritate permanentă.")
                .contains("Z-0457")
                .contains("În anul 2025 nu s-au înregistrat deșeuri generate.")
                .contains("Ultima revizuire s-a făcut la data de 10.03.2022.")
                .contains("Administrator,", "Maria Ionescu", "Prin Ion Popescu – Manager Mediu")
                .doesNotContain("RECTIFICATIV");
        // Unanswered complaints: a line to fill by hand, never "nu a fost cazul".
        assertThat(text).doesNotContain("nu s-au înregistrat sesizări");
    }

    // ── Moving the RAM day (12.10.2026): reminders, a ticked year, a day moved into the past ─────────────────────

    /** The test company with an authorization and the RAM on {@code month}/{@code day}, saved. */
    private Company permitWithRamOn(int month, int day) {
        Company c = companyRepository.findById(tenantId).orElseThrow();
        c.setEnvironmentalAuthNumber("17");
        c.setRamDueMonth(month);
        c.setRamDueDay(day);
        return companyRepository.save(c);
    }

    private List<ReportingDeadline> all(ReportType type) {
        return deadlineRepository.findAll().stream()
                .filter(d -> d.getCompany().getId().equals(tenantId) && d.getReportType() == type)
                .sorted(java.util.Comparator.comparing(ReportingDeadline::getDueDate))
                .toList();
    }

    @Test
    void aSaveThatKeepsTheDayKeepsTheRemindersAlreadySent() {
        LocalDate today = LocalDate.of(2027, 1, 11);
        Company c = permitWithRamOn(1, 15);
        deadlineService.reschedulePermitDeadlines(c, today);
        ReportingDeadline ram = all(ReportType.ENVIRONMENTAL_REPORT).get(0);
        assertThat(ram.getDueDate()).isEqualTo(LocalDate.of(2027, 1, 15));
        ram.setWarned7Days(true);
        deadlineRepository.save(ram);

        // The next day only the administrator's name is corrected: same day, same row, the 7-day mail not sent again.
        c.setAdministratorName("Maria Ionescu");
        deadlineService.reschedulePermitDeadlines(companyRepository.save(c), today.plusDays(1));
        assertThat(all(ReportType.ENVIRONMENTAL_REPORT)).singleElement().satisfies(d -> {
            assertThat(d.getId()).isEqualTo(ram.getId());
            assertThat(d.isWarned7Days()).isTrue();
        });

        // The day really moves: the reminders are due again, on the new day.
        deadlineService.reschedulePermitDeadlines(permitWithRamOn(1, 25), today.plusDays(1));
        assertThat(all(ReportType.ENVIRONMENTAL_REPORT)).singleElement().satisfies(d -> {
            assertThat(d.getDueDate()).isEqualTo(LocalDate.of(2027, 1, 25));
            assertThat(d.isWarned7Days()).isFalse();
        });
    }

    @Test
    void aYearTickedOnItsOldDayGetsNoSecondDeadline() {
        Company c = permitWithRamOn(1, 15);
        deadlineService.reschedulePermitDeadlines(c, LocalDate.of(2027, 1, 2));
        ReportingDeadline ram = all(ReportType.ENVIRONMENTAL_REPORT).get(0);
        ram.setStatus(DeadlineStatus.DONE);
        ram.setCompletedAt(Instant.now());
        deadlineRepository.save(ram);

        // Filed on 5 January; on the 8th the day becomes 20 January. 2027 is done: the next one is 2028.
        deadlineService.reschedulePermitDeadlines(permitWithRamOn(1, 20), LocalDate.of(2027, 1, 8));
        assertThat(all(ReportType.ENVIRONMENTAL_REPORT)).extracting(ReportingDeadline::getDueDate)
                .containsExactly(LocalDate.of(2027, 1, 15), LocalDate.of(2028, 1, 20));
        // The morning job agrees, and "Trecute" does not invent an overdue 20 January next to the ticked one.
        deadlineService.ensureUpcoming(tenantId, LocalDate.of(2027, 1, 9));
        assertThat(all(ReportType.ENVIRONMENTAL_REPORT)).hasSize(2);
        assertThat(deadlineService.listPast(tenantId, LocalDate.of(2027, 2, 1)))
                .filteredOn(d -> d.reportType() == ReportType.ENVIRONMENTAL_REPORT)
                .singleElement()
                .satisfies(d -> assertThat(d.status()).isEqualTo(DeadlineStatus.DONE));
    }

    @Test
    void aDayMovedIntoThePastLeavesTheYearOverdue() {
        Company c = permitWithRamOn(1, 15);
        deadlineService.reschedulePermitDeadlines(c, LocalDate.of(2027, 1, 12));

        // On 12 January the authorization turns out to say 10 January: 2027 was missed, not cancelled.
        deadlineService.reschedulePermitDeadlines(permitWithRamOn(1, 10), LocalDate.of(2027, 1, 12));
        assertThat(all(ReportType.ENVIRONMENTAL_REPORT)).extracting(ReportingDeadline::getDueDate)
                .containsExactly(LocalDate.of(2027, 1, 10), LocalDate.of(2028, 1, 10));
        assertThat(all(ReportType.ENVIRONMENTAL_REPORT).get(0).isWarnedMissed()).isFalse();
    }

    @Test
    void twoOpenDeadlinesLeftInOneYearBecomeOne() {
        // Before 12.10.2026 an overdue 15 January plus a day moved to the 25th left two open in 2027.
        Company c = permitWithRamOn(1, 25);
        for (LocalDate due : List.of(LocalDate.of(2027, 1, 15), LocalDate.of(2027, 1, 25))) {
            deadlineRepository.save(ReportingDeadline.builder().company(c).reportType(ReportType.ENVIRONMENTAL_REPORT)
                    .dueDate(due).status(DeadlineStatus.UPCOMING).createdAt(Instant.now()).build());
        }
        deadlineService.reschedulePermitDeadlines(permitWithRamOn(1, 20), LocalDate.of(2027, 1, 18));
        assertThat(all(ReportType.ENVIRONMENTAL_REPORT)).extracting(ReportingDeadline::getDueDate)
                .containsExactly(LocalDate.of(2027, 1, 20));
    }

    private static LocalDate next(MonthDay day, LocalDate today) {
        LocalDate d = day.atYear(today.getYear());
        return d.isBefore(today) ? day.atYear(today.getYear() + 1) : d;
    }
}
