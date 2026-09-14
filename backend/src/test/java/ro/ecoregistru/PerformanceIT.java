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
import org.springframework.test.web.servlet.MvcResult;
import ro.ecoregistru.audit.AuditChangeCodec;
import ro.ecoregistru.audit.PendingAudit;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P2.16 of the pre-launch QA audit: performance, measured rather than assumed.
 *
 * <p>The plan forbids invented thresholds, so only one kind of assertion is made here — the one
 * that is a property, not a number: <b>the statements a list costs do not grow with the rows it
 * returns</b>. A page of 50 must cost what a page of 10 costs. Everything else (payload bytes,
 * document generation time) is printed with the {@code [P2.16]} prefix and recorded in
 * {@code QA-STATUS.md}, not judged.
 *
 * <p>Statements are counted by Hibernate's own statistics ({@code getPrepareStatementCount}), which
 * include every lazy load a mapper triggers — exactly what an N+1 is made of.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PerformanceIT {

    private static final int YEAR = 2031;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired PartnerWorkPointRepository partnerWorkPointRepository;
    @Autowired DriverRepository driverRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired AttachmentRepository attachmentRepository;
    @Autowired AuditLogRepository auditLogRepository;

    private Company company;
    private AppUser admin;
    private String token;
    private WorkPoint workPoint;
    private List<WasteCode> codes;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        company = companyRepository.save(Company.builder()
                .name("Performance SRL").cui("ROP" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        admin = appUserRepository.save(AppUser.builder()
                .email("perf+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        token = jwtService.generateToken(admin);
        workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Perf").active(true).createdAt(Instant.now()).build());
        codes = wasteCodeRepository.findAll().subList(0, 5);
    }

    // ---------- N+1: the statements a list costs ----------

    /** The most-opened screen. Each row carries attachments, destinations, a partner, a carrier. */
    @Test
    void aPageOfMovementsCostsTheSameStatementsWhateverItsSize() throws Exception {
        seedMovements(60);

        long ten = statements("/api/v1/movements?year=" + YEAR + "&size=10");
        long fifty = statements("/api/v1/movements?year=" + YEAR + "&size=50");
        System.out.printf("[P2.16] movements: page 10 → %d statements, page 50 → %d%n", ten, fifty);

        assertThat(fifty).as("statements for 50 rows vs 10 rows").isEqualTo(ten);
    }

    /** Not paginated: every partner of the tenant, each with its work points and drivers. */
    @Test
    void thePartnerListCostsTheSameStatementsWhateverItsLength() throws Exception {
        seedPartners(5);
        long five = statements("/api/v1/partners");
        seedPartners(20);
        long twentyFive = statements("/api/v1/partners");
        System.out.printf("[P2.16] partners: 5 → %d statements, 25 → %d%n", five, twentyFive);

        assertThat(twentyFive).as("statements for 25 partners vs 5").isEqualTo(five);
    }

    /** Names in the changes are resolved per table, not per row — the javadoc says at most six. */
    @Test
    void aPageOfTheAuditLogCostsTheSameStatementsWhateverItsSize() throws Exception {
        Partner partner = seedPartners(1).get(0);
        for (int i = 0; i < 60; i++) {
            auditLogRepository.save(AuditLog.builder()
                    .company(company).entityType("WasteMovement").entityId(UUID.randomUUID())
                    .action(AuditAction.UPDATE).label("Mişcare " + i)
                    .changes(AuditChangeCodec.write(List.of(
                            new PendingAudit.FieldChange("partner", null, partner.getId().toString()),
                            new PendingAudit.FieldChange("wasteCode", null,
                                    codes.get(i % codes.size()).getId().toString()))))
                    .actorId(admin.getId()).actorEmail(admin.getEmail()).actorRole(Role.ADMIN)
                    .occurredAt(Instant.now()).build());
        }

        long ten = statements("/api/v1/audit-log?size=10");
        long fifty = statements("/api/v1/audit-log?size=50");
        System.out.printf("[P2.16] audit log: page 10 → %d statements, page 50 → %d%n", ten, fifty);

        assertThat(fifty).as("statements for 50 rows vs 10 rows").isEqualTo(ten);
    }

    /**
     * The control dossier regenerates the year and then walks every movement's attachments. The
     * regeneration flushes a hundred-odd writes in between, and on Hibernate 6.4 the lazy
     * collections stopped batching after it: 200 movements cost 200 attachment selects (BUG-016).
     *
     * <p>The second batch of movements repeats the (month, code) pattern of the first — the
     * pattern has period 60 — so the evidence lines, and with them the regeneration's writes, are
     * identical in both measurements. Only the number of movements changes.
     */
    @Test
    void theControlDossierCostsTheSameStatementsWhateverTheMovements() throws Exception {
        seedMovements(60);
        String url = "/api/v1/audit-file?year=" + YEAR;
        request(url);
        long sixty = statements(url);
        seedMovements(60);
        request(url);
        long hundredTwenty = statements(url);
        System.out.printf("[P2.16] control dossier: 60 movements → %d statements, 120 → %d%n",
                sixty, hundredTwenty);

        assertThat(hundredTwenty).as("statements for 120 movements vs 60").isEqualTo(sixty);
    }

    // ---------- measurements, not judged ----------

    @Test
    void payloadsAndDocumentTimesAreMeasured() throws Exception {
        seedMovements(200);

        for (int size : new int[] {25, 200}) {
            MvcResult r = request("/api/v1/movements?year=" + YEAR + "&size=" + size);
            System.out.printf("[P2.16] payload movements size=%d: %d bytes%n",
                    size, r.getResponse().getContentAsByteArray().length);
        }
        String[] documents = {
                "/api/v1/evidences/anexa1?year=" + YEAR,
                "/api/v1/evidences/declaratie-anuala?year=" + YEAR,
                "/api/v1/evidences/export?year=" + YEAR + "&format=xlsx",
                "/api/v1/audit-file?year=" + YEAR,
        };
        for (String url : documents) {
            request(url); // warm-up: first call pays class loading and the evidence rebuild
            long start = System.nanoTime();
            MvcResult r = request(url);
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("[P2.16] %s: %d ms, %d bytes, %d statements (warm)%n",
                    url, ms, r.getResponse().getContentAsByteArray().length, statements(url));
            Statistics s = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
            System.out.printf("[P2.16]   entity loads %d, fetches %d, collection fetches %d, queries %d,"
                            + " inserts %d, updates %d, deletes %d%n",
                    s.getEntityLoadCount(), s.getEntityFetchCount(), s.getCollectionFetchCount(),
                    s.getQueryExecutionCount(), s.getEntityInsertCount(), s.getEntityUpdateCount(),
                    s.getEntityDeleteCount());
        }
    }

    // ---------- helpers ----------

    private MvcResult request(String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
    }

    private long statements(String url) throws Exception {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        request(url);
        return stats.getPrepareStatementCount();
    }

    private void seedMovements(int count) {
        List<Partner> partners = seedPartners(3);
        for (int i = 0; i < count; i++) {
            Partner partner = partners.get(i % partners.size());
            WasteMovement m = movementRepository.save(WasteMovement.builder()
                    .company(company).workPoint(workPoint)
                    .date(LocalDate.of(YEAR, 1 + i % 12, 1 + i % 28))
                    .wasteCode(codes.get(i % codes.size()))
                    .quantity(new BigDecimal("10.000")).unit(Unit.KG)
                    .operation(WasteOperation.RECOVERED).operationCode(WasteOperationCode.R3)
                    .partner(partner).transportPartner(partners.get((i + 1) % partners.size()))
                    .transportDestinations(new java.util.LinkedHashSet<>(Set.of(TransportDestination.TRATARE)))
                    .deleted(false).createdBy(admin.getId()).build());
            attachmentRepository.save(Attachment.builder()
                    .movement(m).url("https://res.cloudinary.com/x/v1/aviz" + i + ".pdf")
                    .publicId("ecoregistru/movements/aviz" + i)
                    .resourceType("image").deliveryType("authenticated").format("pdf")
                    .fileName("aviz" + i + ".pdf").contentType("application/pdf")
                    .createdAt(Instant.now()).build());
        }
    }

    private List<Partner> seedPartners(int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(i -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            Partner p = partnerRepository.save(Partner.builder()
                    .company(company).name("Partener " + suffix).cui("RO" + suffix)
                    .type(PartnerType.COLLECTOR).supplier(true).carrier(true).active(true)
                    .createdAt(Instant.now()).build());
            for (int j = 0; j < 2; j++) {
                partnerWorkPointRepository.save(PartnerWorkPoint.builder()
                        .partner(p).name("Punct " + j).address("Str. " + j)
                        .createdAt(Instant.now()).build());
                driverRepository.save(Driver.builder()
                        .company(company).partner(p).name("Şofer " + j).identification("X" + j)
                        .active(true).createdAt(Instant.now()).build());
            }
            return p;
        }).toList();
    }
}
