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
import ro.ecoregistru.service.BillingRunService;

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
 * F-B2 — Facturare pe pagini: filtrul, luna și căutarea se aplică pe server, cifrele de pe filtre sunt ale acelorași
 * reguli, o pagină costă aceleași interogări oricâte facturi sunt, iar ultima rulare spune ce căderi sunt încă deschise.
 *
 * <p>Fiecare test își face clientul cu un nume unic și caută după el: baza e comună cu celelalte teste.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class InvoicePageIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired SubscriptionInvoiceRepository invoiceRepository;
    @Autowired BillingRunRepository billingRunRepository;

    private static final LocalDate TODAY = LocalDate.now(BillingRunService.ZONE);
    private String token;
    private String tag;

    @BeforeEach
    void setUp() {
        token = jwtService.generateToken(appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow());
        tag = "Pagini" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void eachFilterHoldsItsInvoicesAndActionIsFailedPlusOverdue() throws Exception {
        Subscription s = subscription("Client " + tag);
        SubscriptionInvoice failed = invoice(s, TODAY.minusMonths(4), InvoiceStatus.DRAFT, null, null, "CUI invalid");
        SubscriptionInvoice overdue = invoice(s, TODAY.minusMonths(3), InvoiceStatus.ISSUED, "1", TODAY.minusDays(1), null);
        SubscriptionInvoice dueToday = invoice(s, TODAY.minusMonths(2), InvoiceStatus.ISSUED, "2", TODAY, null);
        SubscriptionInvoice paid = invoice(s, TODAY.minusMonths(1), InvoiceStatus.PAID, "3", TODAY.minusDays(20), null);

        assertThat(ids("ACTION", null)).containsExactlyInAnyOrder(failed.getId(), overdue.getId());
        assertThat(ids("FAILED", null)).containsExactly(failed.getId());
        assertThat(ids("OVERDUE", null)).containsExactly(overdue.getId());
        assertThat(ids("UNPAID", null)).containsExactlyInAnyOrder(overdue.getId(), dueToday.getId());
        assertThat(ids("PAID", null)).containsExactly(paid.getId());
        assertThat(ids("ALL", null)).hasSize(4);

        Map<String, Object> counts = JsonPath.read(page("ALL", null, tag, 0, 50), "$.counts");
        assertThat(counts).containsEntry("ACTION", 2).containsEntry("FAILED", 1).containsEntry("OVERDUE", 1)
                .containsEntry("UNPAID", 2).containsEntry("PAID", 1).containsEntry("ALL", 4);
    }

    @Test
    void theMonthIsTheMonthThePeriodStartsIn() throws Exception {
        Subscription s = subscription("Client " + tag);
        LocalDate july = LocalDate.of(2026, 7, 17);
        SubscriptionInvoice inJuly = invoice(s, july, InvoiceStatus.PAID, "10", july.plusDays(10), null);
        invoice(s, july.plusMonths(1), InvoiceStatus.PAID, "11", july.plusDays(40), null);

        assertThat(ids("ALL", "2026-07")).containsExactly(inJuly.getId());
        assertThat(JsonPath.<Integer>read(page("ALL", "2026-07", tag, 0, 50), "$.counts.ALL")).isEqualTo(1);
    }

    @Test
    void theSearchFindsTheClientOrTheInvoiceNumber() throws Exception {
        Subscription s = subscription("Brutăria " + tag);
        String numar = String.valueOf(900_000 + (int) (Math.random() * 99_999));
        SubscriptionInvoice issued = invoice(s, TODAY.minusMonths(1), InvoiceStatus.ISSUED, numar, TODAY.plusDays(5), null);

        assertThat(idsFor("ALL", null, "brutăria " + tag.toLowerCase())).containsExactly(issued.getId());
        assertThat(idsFor("ALL", null, "WH " + numar)).containsExactly(issued.getId());
        assertThat(idsFor("ALL", null, "wh" + numar)).containsExactly(issued.getId());
    }

    @Test
    void pagesAreCutOnTheServerAndCostTheSameStatements() throws Exception {
        // Un client pe factură: o citire leneșă a plătitorului ar costa câte o interogare pe rând.
        for (int i = 0; i < 12; i++) {
            invoice(subscription("Client " + i + " " + tag), TODAY.minusMonths(1), InvoiceStatus.PAID, String.valueOf(500 + i), TODAY, null);
        }
        String first = page("ALL", null, tag, 0, 5);
        assertThat(JsonPath.<List<?>>read(first, "$.content")).hasSize(5);
        assertThat(JsonPath.<Integer>read(first, "$.total")).isEqualTo(12);
        assertThat(JsonPath.<List<?>>read(page("ALL", null, tag, 2, 5), "$.content")).hasSize(2);

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        // Două pagini pline: una care nu e ultima cere și numărătoarea totalului, deci se compară la fel cu la fel.
        page("ALL", null, tag, 0, 2);
        long two = stats.getPrepareStatementCount();
        stats.clear();
        page("ALL", null, tag, 0, 10);
        long ten = stats.getPrepareStatementCount();
        System.out.printf("[F-B2] invoices page: 2 rows → %d statements, 10 rows → %d%n", two, ten);
        assertThat(ten).isEqualTo(two);
    }

    @Test
    void theLastRunNamesOnlyTheRefusalsStillOpen() throws Exception {
        Subscription s = subscription("Client " + tag);
        SubscriptionInvoice open = invoice(s, TODAY.minusMonths(2), InvoiceStatus.DRAFT, null, null, "CUI invalid");
        SubscriptionInvoice fixed = invoice(s, TODAY.minusMonths(1), InvoiceStatus.ISSUED, "77", TODAY.plusDays(5), null);
        String result = """
                {"configured":true,"reserved":0,"issued":0,"failed":2,"paid":0,
                 "failures":[{"invoiceId":"%s","client":"A","reason":"x"},{"invoiceId":"%s","client":"A","reason":"x"}],
                 "notStarted":[],"issuedInvoices":[],"paidInvoices":[]}""".formatted(open.getId(), fixed.getId());
        billingRunRepository.save(BillingRun.builder().startedAt(Instant.now().plusSeconds(3600))
                .finishedAt(Instant.now().plusSeconds(3600)).kind(BillingRun.Kind.MANUAL).resultJson(result).build());

        String body = mockMvc.perform(get("/api/v1/subscriptions/billing/runs/last").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(body, "$.stillFailed")).containsExactly(open.getId().toString());
    }

    @Test
    void theMoneyIsPaidThisMonthAndEverythingUnpaid() throws Exception {
        String before = money();
        Subscription s = subscription("Client " + tag);
        SubscriptionInvoice now = invoice(s, TODAY.minusMonths(1), InvoiceStatus.PAID, "31", TODAY, null);
        now.setPaidAt(Instant.now());
        invoiceRepository.save(now);
        invoice(s, TODAY.minusMonths(3), InvoiceStatus.PAID, "32", TODAY, null); // plătită acum 70 de zile
        invoice(s, TODAY.minusMonths(2), InvoiceStatus.ISSUED, "33", TODAY.minusDays(3), null);

        String after = money();
        assertThat(num(after, "$.paidThisMonth") - num(before, "$.paidThisMonth")).isEqualTo(99);
        assertThat(num(after, "$.paidThisMonthCount") - num(before, "$.paidThisMonthCount")).isEqualTo(1);
        assertThat(num(after, "$.unpaid") - num(before, "$.unpaid")).isEqualTo(99);
    }

    @Test
    void aTenantAdminIsForbidden() throws Exception {
        String admin = jwtService.generateToken(appUserRepository.findByEmail("admin@demo.ro").orElseThrow());
        mockMvc.perform(get("/api/v1/subscriptions/invoices").header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/subscriptions/invoices/money").header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    private List<UUID> ids(String filter, String month) throws Exception {
        return idsFor(filter, month, tag);
    }

    private List<UUID> idsFor(String filter, String month, String q) throws Exception {
        List<String> raw = JsonPath.read(page(filter, month, q, 0, 200), "$.content[*].id");
        return raw.stream().map(UUID::fromString).toList();
    }

    private String page(String filter, String month, String q, int page, int size) throws Exception {
        var req = get("/api/v1/subscriptions/invoices").header("Authorization", "Bearer " + token)
                .param("filter", filter).param("page", String.valueOf(page)).param("size", String.valueOf(size));
        if (month != null) req.param("month", month);
        if (q != null) req.param("q", q);
        return mockMvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String money() throws Exception {
        return mockMvc.perform(get("/api/v1/subscriptions/invoices/money").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static double num(String json, String path) {
        return ((Number) JsonPath.read(json, path)).doubleValue();
    }

    private Subscription subscription(String name) {
        Company company = companyRepository.save(Company.builder()
                .name(name).cui(TestCui.random()).type(CompanyType.GENERATOR).active(true).createdAt(Instant.now()).build());
        return subscriptionRepository.save(Subscription.builder()
                .company(company).plan(SubscriptionPlan.GENERATOR).status(SubscriptionStatus.ACTIVE)
                .monthlyPrice(BigDecimal.valueOf(99)).implementationFee(BigDecimal.valueOf(290))
                .extraWorkPointPrice(BigDecimal.valueOf(29)).founder(false)
                .startedAt(LocalDate.of(2025, 1, 1)).createdAt(Instant.now()).build());
    }

    private SubscriptionInvoice invoice(Subscription s, LocalDate periodStart, InvoiceStatus status, String numar,
                                        LocalDate dueDate, String error) {
        return invoiceRepository.save(SubscriptionInvoice.builder()
                .subscription(s).periodStart(periodStart).periodEnd(periodStart.plusMonths(1).minusDays(1))
                .total(BigDecimal.valueOf(99)).linesJson("[]").status(status)
                .fgoSerie(numar == null ? null : "WH").fgoNumar(numar).dueDate(dueDate).lastError(error)
                .paidAt(status == InvoiceStatus.PAID ? Instant.now().minusSeconds(86_400L * 70) : null)
                .createdAt(Instant.now()).build());
    }
}
