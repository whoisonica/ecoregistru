package ro.ecoregistru;

import com.jayway.jsonpath.JsonPath;
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
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-B (todo-clienti-abonamente.md) — ce citește tabelul Clienți pe lângă firmă: abonamentul, ultima factură (nu
 * prima, nu una la întâmplare), utilizatorii fără cei dezactivați, nimic din bani la consultant și un număr de
 * interogări care nu crește cu lista.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ClientOverviewIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired CompanyRepository companyRepository;
    @Autowired ConsultancyRepository consultancyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired SubscriptionInvoiceRepository invoiceRepository;

    private String platformToken;

    @BeforeEach
    void setUp() {
        platformToken = jwtService.generateToken(appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow());
    }

    @Test
    void theLatestPeriodAndTheLiveUsersAreShown() throws Exception {
        Company billed = company(null);
        Subscription s = subscription(billed);
        invoice(s, LocalDate.of(2026, 8, 17), InvoiceStatus.PAID, "5", null);
        invoice(s, LocalDate.of(2026, 9, 17), InvoiceStatus.DRAFT, null, "CUI-ul „123” nu e valid");
        invoice(s, LocalDate.of(2026, 7, 17), InvoiceStatus.PAID, "4", null);
        user(billed, null);
        user(billed, null);
        user(billed, Instant.now());
        Company bare = company(null);

        Map<String, Object> row = rowOf(platformToken, billed);
        assertThat(row.get("subscriptionStatus")).isEqualTo("ACTIVE");
        assertThat(row.get("plan")).isEqualTo("GENERATOR");
        assertThat(((Number) row.get("monthlyPrice")).intValue()).isEqualTo(99);
        assertThat(row.get("userCount")).isEqualTo(2);
        @SuppressWarnings("unchecked")
        Map<String, Object> last = (Map<String, Object>) row.get("lastInvoice");
        assertThat(last.get("status")).isEqualTo("DRAFT");
        assertThat(last.get("number")).isNull();
        assertThat(last.get("lastError")).isEqualTo("CUI-ul „123” nu e valid");

        Map<String, Object> empty = rowOf(platformToken, bare);
        assertThat(empty.get("subscriptionStatus")).isNull();
        assertThat(empty.get("lastInvoice")).isNull();
        assertThat(empty.get("userCount")).isEqualTo(0);
    }

    @Test
    void theIssuedNumberIsSerieAndNumar() throws Exception {
        Company c = company(null);
        invoice(subscription(c), LocalDate.of(2026, 9, 1), InvoiceStatus.ISSUED, "12", null);

        @SuppressWarnings("unchecked")
        Map<String, Object> last = (Map<String, Object>) rowOf(platformToken, c).get("lastInvoice");
        assertThat(last.get("number")).isEqualTo("WH 12");
    }

    /** Consultantul vede doar firmele cabinetului lui și niciun ban, nici dacă o firmă ar avea abonament. */
    @Test
    void aConsultantSeesTheirCompaniesWithoutMoney() throws Exception {
        Consultancy mine = consultancyRepository.save(Consultancy.builder()
                .name("Cabinet " + UUID.randomUUID()).cui(TestCui.random()).createdAt(Instant.now()).build());
        Company ours = company(mine);
        invoice(subscription(ours), LocalDate.of(2026, 9, 1), InvoiceStatus.ISSUED, "13", null);
        user(ours, null);
        Company foreign = company(null);
        AppUser consultant = appUserRepository.save(AppUser.builder()
                .email("cons+" + UUID.randomUUID() + "@demo.ro").password("x").role(Role.CONSULTANT)
                .consultancy(mine).enabled(true).createdAt(Instant.now()).build());
        String token = jwtService.generateToken(consultant);

        List<Map<String, Object>> rows = rows(token);
        assertThat(rows).extracting(r -> r.get("companyId")).containsExactly(ours.getId().toString());
        assertThat(rows.get(0).get("subscriptionStatus")).isNull();
        assertThat(rows.get(0).get("lastInvoice")).isNull();
        assertThat(rows.get(0).get("userCount")).isEqualTo(1);
        assertThat(foreign.getId()).isNotNull();
    }

    @Test
    void theListCostsTheSameStatementsWhateverItsLength() throws Exception {
        seed(2);
        long few = statements();
        seed(15);
        long many = statements();
        System.out.printf("[F-B] clients overview: → %d statements, +15 clients → %d%n", few, many);
        assertThat(many).isEqualTo(few);
    }

    @Test
    void tenantRolesAreForbidden() throws Exception {
        String admin = jwtService.generateToken(appUserRepository.findByEmail("admin@demo.ro").orElseThrow());
        mockMvc.perform(get("/api/v1/companies/overview").header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private void seed(int count) {
        for (int i = 0; i < count; i++) {
            Company c = company(null);
            invoice(subscription(c), LocalDate.of(2026, 9, 1), InvoiceStatus.ISSUED, String.valueOf(100 + i), null);
            user(c, null);
        }
    }

    private long statements() throws Exception {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        rows(platformToken);
        return stats.getPrepareStatementCount();
    }

    private List<Map<String, Object>> rows(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/companies/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$");
    }

    private Map<String, Object> rowOf(String token, Company company) throws Exception {
        return rows(token).stream()
                .filter(r -> company.getId().toString().equals(r.get("companyId")))
                .findFirst().orElseThrow();
    }

    private Company company(Consultancy consultancy) {
        return companyRepository.save(Company.builder()
                .name("Client " + UUID.randomUUID()).cui(TestCui.random()).type(CompanyType.GENERATOR)
                .consultancy(consultancy).active(true).createdAt(Instant.now()).build());
    }

    private Subscription subscription(Company company) {
        return subscriptionRepository.save(Subscription.builder()
                .company(company).plan(SubscriptionPlan.GENERATOR).status(SubscriptionStatus.ACTIVE)
                .monthlyPrice(BigDecimal.valueOf(99)).implementationFee(BigDecimal.valueOf(290)).extraWorkPointPrice(BigDecimal.valueOf(29))
                .founder(false).startedAt(LocalDate.of(2026, 7, 17)).createdAt(Instant.now()).build());
    }

    private void invoice(Subscription s, LocalDate periodStart, InvoiceStatus status, String numar, String error) {
        invoiceRepository.save(SubscriptionInvoice.builder()
                .subscription(s).periodStart(periodStart).periodEnd(periodStart.plusMonths(1).minusDays(1))
                .total(BigDecimal.valueOf(99)).linesJson("[]").status(status)
                .fgoSerie(numar == null ? null : "WH").fgoNumar(numar).lastError(error)
                .dueDate(numar == null ? null : periodStart.plusDays(10))
                .createdAt(Instant.now()).build());
    }

    private void user(Company company, Instant deactivatedAt) {
        appUserRepository.save(AppUser.builder()
                .email("u+" + UUID.randomUUID() + "@demo.ro").password("x").role(Role.OPERATOR)
                .company(company).enabled(deactivatedAt == null).deactivatedAt(deactivatedAt)
                .createdAt(Instant.now()).build());
    }
}
