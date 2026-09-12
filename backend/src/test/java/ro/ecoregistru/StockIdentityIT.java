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
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1.10 of the pre-launch QA audit: the stock identity of Anexa 1 cap. 1, term by term.
 *
 * <p>{@code EvidenceCalculatorIT} proves the <em>shape</em> of the report — twelve months, the
 * carry across years, which column an exit lands in. This class proves the <em>arithmetic</em>,
 * and in particular the fifth term the audit prompt did not know about:
 *
 * <pre>
 *   exits     = recovered + disposed + unclassified out
 *   covered   = opening stock + recorded generation
 *   implied   = max(0, exits − covered)            (V24)
 *   printed   = recorded generation + implied
 *   closing   = opening + printed − exits
 * </pre>
 *
 * <p>A test written on the four-term formula would report a false bug, so every case below names
 * what it expects {@code impliedGenerated} to be, not only the stock.
 *
 * <p>Its own tenant, with <b>no fixture movements at all</b>: each test drives the exact figures it
 * asserts. That is the point — a shared fixture is what let the register filter go unproved until
 * 12.09.2026.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class StockIdentityIT {

    @Autowired EvidenceCalculator evidenceCalculator;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;

    private UUID tenantId;
    private WorkPoint workPoint;
    private WorkPoint otherWorkPoint;
    private WasteCode code;
    private WasteCode otherCode;
    private Partner collector;
    private UUID creatorId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Stock Identity SRL").cui("ROS" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        creatorId = appUserRepository.save(AppUser.builder()
                .email("stock+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build())
                .getId();
        workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Unu").active(true).createdAt(Instant.now()).build());
        otherWorkPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Doi").active(true).createdAt(Instant.now()).build());
        List<WasteCode> codes = wasteCodeRepository.findAll();
        code = codes.get(0);
        otherCode = codes.get(1);
        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector SRL").cui("RO" + suffix)
                .type(PartnerType.COLLECTOR).supplier(true).active(true)
                .createdAt(Instant.now()).build());
        TenantContext.set(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---------- the implied term (V24) ----------

    /**
     * The ordinary client: records the handover, never the generation. Before V24 this printed
     * "Generate 0, valorificat 300, stoc −300" — a sheet that is filed nowhere.
     */
    @Test
    void anExitNobodyRecordedAsGeneratedIsReportedAsGeneratedToo() {
        recover(LocalDate.of(2026, 1, 10), "300.000");

        MonthlyEvidenceResponse january = month(2026, 1);
        assertThat(january.totalRecovered()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("300.000"));
        assertThat(january.totalGenerated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("300.000"));
        assertThat(january.impliedGenerated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("300.000"));
        assertStock(january, "0.000");
    }

    /** When both sides are recorded, the rule is inert — that is what makes it safe. */
    @Test
    void nothingIsImpliedWhenTheGenerationWasRecorded() {
        generate(LocalDate.of(2026, 1, 5), "500.000");
        recover(LocalDate.of(2026, 1, 10), "300.000");

        MonthlyEvidenceResponse january = month(2026, 1);
        assertThat(january.impliedGenerated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(BigDecimal.ZERO);
        assertThat(january.totalGenerated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("500.000"));
        assertStock(january, "200.000");
    }

    /**
     * Question <b>W</b> to the specialist, pinned as the application answers it today: records 80,
     * hands over 100. The 20 kg difference is <em>deducted</em>, not left visible as a shortfall.
     * If the answer ever comes back "show the gap", this is the test that has to change — and it
     * names the choice instead of leaving it implicit in an arithmetic expression.
     */
    @Test
    void aPartiallyRecordedGenerationImpliesOnlyTheUncoveredPart() {
        generate(LocalDate.of(2026, 1, 5), "80.000");
        recover(LocalDate.of(2026, 1, 10), "100.000");

        MonthlyEvidenceResponse january = month(2026, 1);
        assertThat(january.impliedGenerated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("20.000"));
        assertThat(january.totalGenerated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("100.000"));
        assertStock(january, "0.000");
    }

    /** Stock carried in is coverage: it is consumed before anything is implied. */
    @Test
    void stockCarriedInCoversTheExitBeforeAnythingIsImplied() {
        generate(LocalDate.of(2026, 1, 5), "1000.000");
        recover(LocalDate.of(2026, 3, 10), "400.000");

        MonthlyEvidenceResponse march = month(2026, 3);
        assertThat(march.impliedGenerated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(BigDecimal.ZERO);
        assertThat(march.totalGenerated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(BigDecimal.ZERO);
        assertStock(march, "600.000");
    }

    /** And across the year boundary, which is where the carry is a different code path. */
    @Test
    void stockCarriedFromDecemberCoversJanuaryWithoutImplying() {
        generate(LocalDate.of(2025, 12, 5), "1000.000");
        recover(LocalDate.of(2026, 1, 10), "400.000");
        evidenceCalculator.regenerateYear(2025);

        MonthlyEvidenceResponse january = month(2026, 1);
        assertThat(january.impliedGenerated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(BigDecimal.ZERO);
        assertStock(january, "600.000");
    }

    /**
     * An exit with no R/D code is still an exit, so it is covered like any other. The line stays
     * {@code incomplete} — the quantity reaches no official column — but the stock does not go
     * negative on account of it.
     */
    @Test
    void anExitWithNoOperationCodeIsCoveredToo() {
        unclassifiedExit(LocalDate.of(2026, 1, 10), "250.000");

        MonthlyEvidenceResponse january = month(2026, 1);
        assertThat(january.impliedGenerated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("250.000"));
        assertThat(january.totalUnclassifiedOut()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("250.000"));
        assertThat(january.incomplete()).isTrue();
        assertStock(january, "0.000");
    }

    /**
     * The invariant the whole rule exists for, stated once: <b>no month of any pair can close
     * negative.</b>
     *
     * <p>Worth its own test because the engine's defences against a negative stock are still
     * visible in the product — the dashboard tile colours it red and sorts it first, the annual
     * declaration marks the row — and code that guards an unreachable state is the kind that gets
     * "simplified away". If someone ever removes the implied term, twelve assertions fail here
     * before anyone prints a sheet.
     */
    @Test
    void noMonthEverClosesNegative() {
        recover(LocalDate.of(2026, 2, 10), "400.000");
        dispose(LocalDate.of(2026, 5, 10), "700.000");
        unclassifiedExit(LocalDate.of(2026, 9, 10), "120.000");

        List<MonthlyEvidenceResponse> year = evidenceCalculator.list(2026, null, workPoint.getId());
        assertThat(year).hasSize(12);
        assertThat(year).allSatisfy(line ->
                assertThat(line.closingStock().signum()).isNotNegative());
    }

    // ---------- the terms that are easy to get right and expensive to get wrong ----------

    /** Tonnes and kilograms are the same scale by the time they reach a column. */
    @Test
    void tonnesAndKilogramsLandOnTheSameScale() {
        save(LocalDate.of(2026, 1, 5), "1.000", Unit.TONS, WasteOperation.GENERATED, null, null);
        save(LocalDate.of(2026, 1, 10), "500.000", Unit.KG, WasteOperation.RECOVERED,
                WasteOperationCode.R3, collector);

        assertStock(month(2026, 1), "500.000");
    }

    /**
     * An exit still waiting for the recipient's weighbridge moves nothing: neither zero nor an
     * estimate stands in for a measurement (decision 5). The line says so instead.
     */
    @Test
    void anExitAwaitingTheWeighbridgeMovesNoStockAndSaysSo() {
        generate(LocalDate.of(2026, 1, 5), "400.000");
        movementRepository.save(base(LocalDate.of(2026, 1, 20))
                .quantity(null).unit(Unit.KG)
                .operation(WasteOperation.RECOVERED).operationCode(WasteOperationCode.R3)
                .partner(collector).weighedAtUnloading(true).build());

        MonthlyEvidenceResponse january = month(2026, 1);
        assertThat(january.awaitingWeighing()).isTrue();
        assertThat(january.totalRecovered()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(BigDecimal.ZERO);
        assertStock(january, "400.000");
    }

    /**
     * Four running stocks, not one. The engine groups by (work point, waste code), and a leak
     * between groups would be invisible on a sheet — each page prints only its own pair.
     */
    @Test
    void everyWorkPointAndCodePairCarriesItsOwnStock() {
        generate(LocalDate.of(2026, 1, 5), "100.000");
        movementRepository.save(base(LocalDate.of(2026, 1, 5))
                .wasteCode(otherCode).quantity(new BigDecimal("200.000")).unit(Unit.KG)
                .operation(WasteOperation.GENERATED).build());
        movementRepository.save(base(LocalDate.of(2026, 1, 5))
                .workPoint(otherWorkPoint).quantity(new BigDecimal("300.000")).unit(Unit.KG)
                .operation(WasteOperation.GENERATED).build());
        movementRepository.save(base(LocalDate.of(2026, 1, 5))
                .workPoint(otherWorkPoint).wasteCode(otherCode)
                .quantity(new BigDecimal("400.000")).unit(Unit.KG)
                .operation(WasteOperation.GENERATED).build());

        List<MonthlyEvidenceResponse> all = evidenceCalculator.list(2026, 1, null);
        assertThat(all).hasSize(4);
        assertThat(all).anySatisfy(l -> assertPair(l, workPoint, code, "100.000"));
        assertThat(all).anySatisfy(l -> assertPair(l, workPoint, otherCode, "200.000"));
        assertThat(all).anySatisfy(l -> assertPair(l, otherWorkPoint, code, "300.000"));
        assertThat(all).anySatisfy(l -> assertPair(l, otherWorkPoint, otherCode, "400.000"));
    }

    /** A year in which nothing happened is twelve zeroes, not an empty report. */
    @Test
    void aYearWithNoMovementsForAPairIsNotReportedAtAll() {
        generate(LocalDate.of(2026, 1, 5), "100.000");
        recover(LocalDate.of(2026, 1, 20), "100.000");
        evidenceCalculator.regenerateYear(2026);

        // Closed at zero, so nothing carries: 2027 has no live pair and no lines.
        assertThat(evidenceCalculator.list(2027, null, workPoint.getId())).isEmpty();
    }

    // --- helpers ---

    private void generate(LocalDate date, String kg) {
        save(date, kg, Unit.KG, WasteOperation.GENERATED, null, null);
    }

    private void recover(LocalDate date, String kg) {
        save(date, kg, Unit.KG, WasteOperation.RECOVERED, WasteOperationCode.R3, collector);
    }

    private void dispose(LocalDate date, String kg) {
        save(date, kg, Unit.KG, WasteOperation.DISPOSED, WasteOperationCode.D5, null);
    }

    private void unclassifiedExit(LocalDate date, String kg) {
        save(date, kg, Unit.KG, WasteOperation.UNCLASSIFIED_OUT, null, collector);
    }

    private void save(LocalDate date, String qty, Unit unit, WasteOperation op,
                      WasteOperationCode opCode, Partner partner) {
        movementRepository.save(base(date)
                .quantity(new BigDecimal(qty)).unit(unit)
                .operation(op).operationCode(opCode).partner(partner).build());
    }

    private WasteMovement.WasteMovementBuilder base(LocalDate date) {
        return WasteMovement.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .workPoint(workPoint).date(date).wasteCode(code)
                .register(WasteRegister.ANEXA_1)
                .deleted(false).createdBy(creatorId);
    }

    private MonthlyEvidenceResponse month(int year, int month) {
        return evidenceCalculator.list(year, month, workPoint.getId()).get(0);
    }

    private static void assertStock(MonthlyEvidenceResponse line, String expectedKg) {
        assertThat(line.closingStock()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal(expectedKg));
    }

    private static void assertPair(MonthlyEvidenceResponse line, WorkPoint wp, WasteCode wc,
                                   String expectedKg) {
        assertThat(line.workPointId()).isEqualTo(wp.getId());
        assertThat(line.wasteCodeId()).isEqualTo(wc.getId());
        assertStock(line, expectedKg);
    }
}
