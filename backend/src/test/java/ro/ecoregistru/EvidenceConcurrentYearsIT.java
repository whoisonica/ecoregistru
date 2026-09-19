package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.EvidenceCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA de lansare, generator — G08: citiri paralele pe <b>ani diferiți</b> ai aceleiași firme.
 *
 * <p>{@code EvidenceCalculatorIT#twoReadsAtOnceDoNotCollideOverTheRebuild} probează două citiri pe
 * <em>același</em> an. Lacătul de reconstrucție e însă pe perechea (firmă, an)
 * ({@code MonthlyEvidenceRepository.lockForRebuild}), iar o citire reconstruiește în cascadă de la
 * cel mai vechi an învechit ({@code EvidenceCalculator.refreshIfStale}). După o corectură pe 2024,
 * o citire pe 2025 și una pe 2026 reconstruiesc amândouă 2024, fiecare sub alt lacăt.
 *
 * <p>Observat 19.09.2026, la trei rulări la rând: liniile rămân corecte (una pe lună, totalul bun),
 * dar în fiecare rundă doi din trei cititori cad cu {@code ObjectOptimisticLockingFailureException}
 * — ștergerea derivată {@code deleteByCompany_IdAndYear} încarcă rândurile și le șterge unul câte
 * unul, iar celălalt fir le-a șters deja. {@code AdviceController} o transformă în 409, deci o
 * simplă citire (fișa, declarația, ecranul) primește „conflict”.
 *
 * <p>Oracolul, de mână: fiecare citire reușește, o singură linie pe (punct de lucru, cod, an, lună),
 * iar martie 2024 are suma predărilor puse în test.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class EvidenceConcurrentYearsIT {

    @Autowired EvidenceCalculator evidenceCalculator;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired MonthlyEvidenceRepository evidenceRepository;

    private UUID tenantId;
    private UUID creatorId;
    private WorkPoint workPoint;
    private WasteCode code;
    private Partner collector;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Ani Paraleli SRL").cui("ROP" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        creatorId = appUserRepository.save(AppUser.builder()
                .email("ani+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build()).getId();
        workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Ani").active(true).createdAt(Instant.now()).build());
        code = wasteCodeRepository.findByCode("20 01 01").orElseThrow();
        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector Ani SRL").cui("RO6" + suffix)
                .authorizationNumber("AM 4/2025").type(PartnerType.COLLECTOR).supplier(true)
                .active(true).createdAt(Instant.now()).build());
        for (int year = 2024; year <= 2026; year++) {
            handover(LocalDate.of(year, 3, 10), "100.000");
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    /**
     * Cinci runde. În fiecare: o predare nouă pe 2024 (toți trei anii devin învechiți), apoi trei
     * cititori porniți deodată pe 2024, 2025 și 2026, cât un al patrulea fir mai scrie o predare pe
     * 2025. La final: o linie pe lună, pe fiecare an, și totalul lui martie 2024 = 100 + 5 × 10.
     */
    @Test
    void readsOnDifferentYearsDuringWritesLeaveOneLinePerMonth() throws Exception {
        TenantContext.set(tenantId);
        for (int year = 2024; year <= 2026; year++) {
            evidenceCalculator.regenerateYear(year);
        }
        TenantContext.clear();

        for (int round = 0; round < 5; round++) {
            handover(LocalDate.of(2024, 3, 20), "10.000");
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(4);
            List<Future<?>> done = new ArrayList<>();
            for (int year = 2024; year <= 2026; year++) {
                int y = year;
                done.add(pool.submit(() -> {
                    TenantContext.set(tenantId);
                    try {
                        start.await();
                        return evidenceCalculator.list(y, null, null).size();
                    } finally {
                        TenantContext.clear();
                    }
                }));
            }
            done.add(pool.submit(() -> {
                start.await();
                return handover(LocalDate.of(2025, 6, 1), "1.000");
            }));
            start.countDown();
            try {
                for (Future<?> f : done) {
                    f.get(60, TimeUnit.SECONDS);
                }
            } finally {
                pool.shutdownNow();
            }
        }

        for (int year = 2024; year <= 2026; year++) {
            Map<Integer, Long> linesPerMonth = evidenceRepository.findByCompany_IdAndYear(tenantId, year)
                    .stream().collect(Collectors.groupingBy(MonthlyEvidence::getMonth, Collectors.counting()));
            assertThat(linesPerMonth).as("linii pe lună în " + year).hasSize(12).allSatisfy((m, n) -> assertThat(n).isEqualTo(1L));
        }
        assertThat(evidenceRepository.findByCompany_IdAndYear(tenantId, 2024).stream()
                .filter(e -> e.getMonth() == 3).map(MonthlyEvidence::getTotalRecovered).toList())
                .singleElement().satisfies(v -> assertThat(v).isEqualByComparingTo("150.000"));
    }

    private WasteMovement handover(LocalDate date, String kg) {
        return movementRepository.saveAndFlush(WasteMovement.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .workPoint(workPoint).date(date).wasteCode(code)
                .quantity(new BigDecimal(kg)).unit(Unit.KG)
                .operation(WasteOperation.RECOVERED).operationCode(WasteOperationCode.R13)
                .wasteDestination(WasteDestination.Vr).partner(collector)
                .register(WasteRegister.ANEXA_1)
                .deleted(false).createdBy(creatorId).build());
    }
}
