package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.controller.response.MonthlyEvidenceResponse;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.EvidenceCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Correctness of the evidence engine (FAZA EVID / E4): per-operation totals, cumulative
 * closing stock, TONS→KG normalisation, and cross-year opening carry. Runs on an isolated
 * tenant so the dev seed data can't perturb the expected numbers.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class EvidenceCalculatorIT {

    @Autowired EvidenceCalculator evidenceCalculator;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;

    private UUID tenantId;
    private WorkPoint workPoint;
    private WasteCode code;
    private UUID creatorId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Evidence Co SRL").cui("ROE" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("evid+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        creatorId = user.getId();
        workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Evidence").active(true).createdAt(Instant.now()).build());
        code = wasteCodeRepository.findAll().get(0);

        // Year 2025 activity (mixed units); running stock in KG:
        //  Jan: +2000 (2 TONS) +500 (KG)            -> 2500
        //  Feb: -1000 (1 TON handed over)           -> 1500
        //  Mar: -200 (disposed, D5)                 -> 1300
        //  Apr: -300 (recovered, R3)                -> 1000
        //  Dec: +100 (generated)                    -> 1100
        save(LocalDate.of(2025, 1, 10), new BigDecimal("2.000"), Unit.TONS, WasteOperation.GENERATED, null);
        save(LocalDate.of(2025, 1, 20), new BigDecimal("500.000"), Unit.KG, WasteOperation.COLLECTED, null);
        save(LocalDate.of(2025, 2, 5), new BigDecimal("1.000"), Unit.TONS, WasteOperation.HANDED_OVER, null);
        save(LocalDate.of(2025, 3, 8), new BigDecimal("200.000"), Unit.KG, WasteOperation.DISPOSED, WasteOperationCode.D5);
        save(LocalDate.of(2025, 4, 8), new BigDecimal("300.000"), Unit.KG, WasteOperation.RECOVERED, WasteOperationCode.R3);
        save(LocalDate.of(2025, 12, 15), new BigDecimal("100.000"), Unit.KG, WasteOperation.GENERATED, null);
        // Year 2026: one handover, to prove opening stock carries over from Dec 2025.
        save(LocalDate.of(2026, 1, 9), new BigDecimal("100.000"), Unit.KG, WasteOperation.HANDED_OVER, null);

        TenantContext.set(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void aggregatesTotalsAndCumulativeStockPerMonth() {
        var result = evidenceCalculator.regenerateYear(2025);
        assertThat(result.linesGenerated()).isEqualTo(5); // Jan, Feb, Mar, Apr, Dec

        Map<Integer, MonthlyEvidenceResponse> byMonth = evidenceCalculator.list(2025, null, workPoint.getId())
                .stream().collect(Collectors.toMap(MonthlyEvidenceResponse::month, Function.identity()));
        assertThat(byMonth.keySet()).containsExactlyInAnyOrder(1, 2, 3, 4, 12);

        MonthlyEvidenceResponse jan = byMonth.get(1);
        assertThat(jan.totalGenerated()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("2000")); // TONS→KG
        assertThat(jan.totalCollected()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("500"));
        assertThat(jan.closingStock()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("2500"));

        assertThat(byMonth.get(2).totalHandedOver()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("1000"));
        assertThat(byMonth.get(2).closingStock()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("1500"));
        assertThat(byMonth.get(3).closingStock()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("1300"));
        assertThat(byMonth.get(4).closingStock()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("1000"));
        assertThat(byMonth.get(12).closingStock()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("1100"));
    }

    @Test
    void januaryOpeningStockCarriesFromPreviousDecember() {
        evidenceCalculator.regenerateYear(2025); // establishes Dec 2025 closing = 1100
        var result = evidenceCalculator.regenerateYear(2026);
        assertThat(result.linesGenerated()).isEqualTo(1);

        List<MonthlyEvidenceResponse> lines = evidenceCalculator.list(2026, 1, workPoint.getId());
        assertThat(lines).hasSize(1);
        // opening 1100 (from Dec 2025) − 100 handed over = 1000
        assertThat(lines.get(0).closingStock()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("1000"));
    }

    @Test
    void regenerateIsIdempotent() {
        evidenceCalculator.regenerateYear(2025);
        var second = evidenceCalculator.regenerateYear(2025);
        assertThat(second.linesGenerated()).isEqualTo(5);
        assertThat(evidenceCalculator.list(2025, null, null)).hasSize(5);
    }

    private void save(LocalDate date, BigDecimal qty, Unit unit, WasteOperation op, WasteOperationCode opCode) {
        movementRepository.save(WasteMovement.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .workPoint(workPoint).date(date).wasteCode(code)
                .quantity(qty).unit(unit).operation(op).operationCode(opCode)
                .deleted(false).createdBy(creatorId).build());
    }
}
