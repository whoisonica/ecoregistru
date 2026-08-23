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
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Correctness of the evidence engine (FAZA EVID / Etapa 2b): which movements reach Anexa 1, which
 * column each of them lands in, the cumulative stock, and the shape of the report — 12 months per
 * live (work point, code) pair, carried across years.
 *
 * <p>Runs on an isolated tenant so the dev seed data can't perturb the expected numbers.
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
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;

    private UUID tenantId;
    private WorkPoint workPoint;
    private WasteCode code;
    private Partner collector;
    private UUID creatorId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        // BOTH, not GENERATOR: the fixture records a COLLECTED takeover, and only a company that
        // actually takes waste over keeps an art. 48 register.
        Company company = companyRepository.save(Company.builder()
                .name("Evidence Co SRL").cui("ROE" + suffix).type(CompanyType.BOTH)
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("evid+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        creatorId = user.getId();
        workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Evidence").active(true).createdAt(Instant.now()).build());
        code = wasteCodeRepository.findAll().get(0);
        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector SRL").cui("RO" + suffix)
                .type(PartnerType.COLLECTOR).supplier(true).active(true).createdAt(Instant.now()).build());

        // Year 2025, one (work point, code) pair. Running stock in KG, Anexa 1 cap. 1:
        //   stock = previous + generated − recovered − disposed − unclassified out
        //  Jan: +2000 generated (2 TONS)                        -> 2000
        //       +500 collected from a third party (art. 48)     -> not in Anexa 1 at all
        //  Feb: -1000 recovered by the collector, R3            -> 1000, memo "din care predat"
        //  Mar: -200 disposed by the company itself, D5         ->  800
        //  Apr: -300 out with no code at all (legacy row)       ->  500, line incomplete
        //  Dec: +100 generated                                  ->  600
        save(LocalDate.of(2025, 1, 10), "2.000", Unit.TONS, WasteOperation.GENERATED, null, null);
        save(LocalDate.of(2025, 1, 20), "500.000", Unit.KG, WasteOperation.COLLECTED, null, null);
        save(LocalDate.of(2025, 2, 5), "1.000", Unit.TONS, WasteOperation.RECOVERED, WasteOperationCode.R3, collector);
        save(LocalDate.of(2025, 3, 8), "200.000", Unit.KG, WasteOperation.DISPOSED, WasteOperationCode.D5, null);
        save(LocalDate.of(2025, 4, 8), "300.000", Unit.KG, WasteOperation.UNCLASSIFIED_OUT, null, collector);
        save(LocalDate.of(2025, 12, 15), "100.000", Unit.KG, WasteOperation.GENERATED, null, null);

        TenantContext.set(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void reportsTwelveMonthsPerPairWithCumulativeStock() {
        var result = evidenceCalculator.regenerateYear(2025);
        assertThat(result.linesGenerated()).isEqualTo(12); // Anexa 1 is a 12-row table

        Map<Integer, MonthlyEvidenceResponse> byMonth = byMonth(2025);
        assertThat(byMonth.keySet()).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

        assertThat(byMonth.get(1).totalGenerated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("2000")); // TONS→KG
        assertStock(byMonth.get(1), "2000");
        assertStock(byMonth.get(2), "1000");
        assertStock(byMonth.get(3), "800");
        assertStock(byMonth.get(4), "500");
        // Months with no movements still get a row, carrying the stock forward untouched.
        assertStock(byMonth.get(5), "500");
        assertStock(byMonth.get(11), "500");
        assertThat(byMonth.get(5).totalGenerated()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
        assertStock(byMonth.get(12), "600");
    }

    @Test
    void anExitLandsInTheColumnItsOperationCodeImplies() {
        evidenceCalculator.regenerateYear(2025);
        Map<Integer, MonthlyEvidenceResponse> byMonth = byMonth(2025);

        // R3 performed by the collector: reported as "valorificată", with the quantity shown
        // again as the memo "din care predat" — the fișa has no "predat" column, and one physical
        // exit must not be counted twice.
        MonthlyEvidenceResponse feb = byMonth.get(2);
        assertThat(feb.totalRecovered()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("1000"));
        assertThat(feb.totalDisposed()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
        assertThat(feb.totalHandedOver()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("1000"));
        assertThat(feb.incomplete()).isFalse();

        // The company's own D5 disposal: "eliminată final", nothing handed over.
        MonthlyEvidenceResponse mar = byMonth.get(3);
        assertThat(mar.totalDisposed()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("200"));
        assertThat(mar.totalHandedOver()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void anExitWithoutAnOperationCodeLeavesStockButNoOfficialColumn() {
        evidenceCalculator.regenerateYear(2025);
        MonthlyEvidenceResponse apr = byMonth(2025).get(4);

        assertThat(apr.totalUnclassifiedOut()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("300"));
        assertThat(apr.totalRecovered()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
        assertThat(apr.totalDisposed()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
        assertThat(apr.incomplete()).isTrue(); // visible as a gap, never closed over a guess
        assertStock(apr, "500");
    }

    @Test
    void takeoverStaysOutOfAnexa1() {
        evidenceCalculator.regenerateYear(2025);
        Map<Integer, MonthlyEvidenceResponse> byMonth = byMonth(2025);

        // The 500 kg taken over in January is art. 48: it neither raises the stock nor appears
        // here. That separation is the rule (HG 856/2002 art. 2 alin. (1)); the "for review" flag
        // that once rode along with it is gone — the generator module does not follow takeovers.
        assertStock(byMonth.get(1), "2000");
        assertThat(byMonth.get(1).totalGenerated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("2000"));
    }

    @Test
    void januaryOpeningStockCarriesFromPreviousDecember() {
        evidenceCalculator.regenerateYear(2025); // establishes Dec 2025 closing = 600
        save(LocalDate.of(2026, 1, 9), "100.000", Unit.KG, WasteOperation.RECOVERED, WasteOperationCode.R3, collector);

        var result = evidenceCalculator.regenerateYear(2026);
        assertThat(result.linesGenerated()).isEqualTo(12);

        Map<Integer, MonthlyEvidenceResponse> byMonth = byMonth(2026);
        assertStock(byMonth.get(1), "500"); // 600 carried − 100 recovered
        assertStock(byMonth.get(12), "500");
    }

    @Test
    void pairWithCarriedStockAndNoMovementsStillGetsItsFicheAndKeepsTheStock() {
        evidenceCalculator.regenerateYear(2025); // Dec 2025 closing = 600, no 2026 movements at all

        var result = evidenceCalculator.regenerateYear(2026);
        assertThat(result.linesGenerated()).isEqualTo(12); // the pair cannot vanish from the report

        Map<Integer, MonthlyEvidenceResponse> byMonth = byMonth(2026);
        assertStock(byMonth.get(1), "600");
        assertStock(byMonth.get(12), "600");
    }

    @Test
    void regeneratingAnEarlierYearRebuildsTheYearsAfterIt() {
        evidenceCalculator.regenerateYear(2025);
        evidenceCalculator.regenerateYear(2026); // 2026 opens at 600, stays 600 all year

        // A movement is added to 2025 after the fact — a correction, as it happens in practice.
        save(LocalDate.of(2025, 6, 4), "50.000", Unit.KG, WasteOperation.GENERATED, null, null);
        var result = evidenceCalculator.regenerateYear(2025);

        assertThat(result.cascadedYears()).containsExactly(2026);
        assertThat(result.linesGenerated()).isEqualTo(24); // 2025 and 2026 together
        assertStock(byMonth(2025).get(12), "650");
        assertStock(byMonth(2026).get(1), "650"); // the correction reached the following year
    }

    @Test
    void regenerateIsIdempotent() {
        evidenceCalculator.regenerateYear(2025);
        var second = evidenceCalculator.regenerateYear(2025);
        assertThat(second.linesGenerated()).isEqualTo(12);
        assertThat(second.cascadedYears()).isEmpty();
        assertThat(evidenceCalculator.list(2025, null, null)).hasSize(12);
    }

    // --- helpers ---

    private Map<Integer, MonthlyEvidenceResponse> byMonth(int year) {
        return evidenceCalculator.list(year, null, workPoint.getId()).stream()
                .collect(Collectors.toMap(MonthlyEvidenceResponse::month, Function.identity()));
    }

    private static void assertStock(MonthlyEvidenceResponse line, String expectedKg) {
        assertThat(line.closingStock()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal(expectedKg));
    }

    private void save(LocalDate date, String qty, Unit unit, WasteOperation op,
                      WasteOperationCode opCode, Partner partner) {
        movementRepository.save(WasteMovement.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .workPoint(workPoint).date(date).wasteCode(code)
                .quantity(new BigDecimal(qty)).unit(unit).operation(op).operationCode(opCode)
                .partner(partner)
                .deleted(false).createdBy(creatorId).build());
    }
}
