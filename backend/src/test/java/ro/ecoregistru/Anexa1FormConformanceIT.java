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
 * ETAPA 2c — the reference test: a whole year of one fişă, checked against the shape and the
 * arithmetic of the working Anexa 1 sheet the environmental expert uses in practice
 * ({@code documente oficiale/RAPORTARE DESEURI GENERATE.xlsx}, sheets "20 03 01", "20 01 01" and
 * "15 01 02", read on 23.08.2026).
 *
 * <p><b>Why the numbers here are ours.</b> The workbook turned out to be a blank template: every
 * quantity cell is empty and every TOTAL AN reads 0, so there are no figures of hers to reproduce.
 * What it does carry is the arithmetic, in its own formulas, and that is what this test pins:
 * <pre>
 *   C26 = SUM(C14:C25)   TOTAL AN "Generate"      = the twelve monthly rows
 *   D26 = SUM(D14:D25)   TOTAL AN "valorificată"  = the twelve monthly rows
 *   E26 = SUM(E14:E25)   TOTAL AN "eliminată final"
 *   F26 = C26 - D26      "rămasă în stoc" = generated − treated (her sheets have no disposal,
 *                        so the general identity is generated − recovered − disposed)
 *   "Stoc: 0 kg"         a header line above cap. 1: the year opens from the carried stock
 * </pre>
 * The file is gitignored, so nothing here reads it: the contract is transcribed, with its source
 * and date named above, and the fixture below is a plausible office fişă built to exercise it.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class Anexa1FormConformanceIT {

    private static final int YEAR = 2025;

    /** Closing stock per month, in KG, as the fişa would read down its twelve rows. */
    private static final String[] EXPECTED_STOCK = {
            "100", "200", "50", "150", "250", "100", "150", "250", "100", "200", "300", "150"
    };

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
    private Partner recycler;
    private UUID creatorId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Birou Anexa 1 SRL").cui("ROA" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("anexa1+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        creatorId = user.getId();
        workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Sediu").active(true).createdAt(Instant.now()).build());
        code = wasteCodeRepository.findByCode("20 03 01").orElseThrow();
        recycler = partnerRepository.save(Partner.builder()
                .company(company).name("Reciclator SRL").cui("RO" + suffix)
                .type(PartnerType.COLLECTOR).client(true).active(true).createdAt(Instant.now()).build());

        // The year as an office would live it: 100 kg generated every month, handed over for
        // recycling at the end of each quarter, plus one lot sent to a landfill in July.
        for (int month = 1; month <= 12; month++) {
            save(LocalDate.of(YEAR, month, 15), "100.000", WasteOperation.GENERATED, null, null);
        }
        for (int quarterEnd : new int[]{3, 6, 9, 12}) {
            save(LocalDate.of(YEAR, quarterEnd, 28), "250.000",
                    WasteOperation.RECOVERED, WasteOperationCode.R3, recycler);
        }
        save(LocalDate.of(YEAR, 7, 20), "50.000", WasteOperation.DISPOSED, WasteOperationCode.D5, null);

        TenantContext.set(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void theFicheHasTwelveRowsInMonthOrder() {
        evidenceCalculator.regenerateYear(YEAR);
        List<MonthlyEvidenceResponse> fiche = evidenceCalculator.list(YEAR, null, workPoint.getId());

        assertThat(fiche).hasSize(12);
        assertThat(fiche).extracting(MonthlyEvidenceResponse::month)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

    @Test
    void stockReadsDownTheTwelveRowsAsTheSheetDoes() {
        evidenceCalculator.regenerateYear(YEAR);
        List<MonthlyEvidenceResponse> fiche = evidenceCalculator.list(YEAR, null, workPoint.getId());

        for (int i = 0; i < 12; i++) {
            assertThat(fiche.get(i).closingStock())
                    .as("stoc după luna %d", i + 1)
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(new BigDecimal(EXPECTED_STOCK[i]));
        }
    }

    @Test
    void totalAnMatchesTheSumOfTheTwelveRowsAndClosesTheStock() {
        evidenceCalculator.regenerateYear(YEAR);
        List<MonthlyEvidenceResponse> fiche = evidenceCalculator.list(YEAR, null, workPoint.getId());

        BigDecimal generated = sum(fiche, MonthlyEvidenceResponse::totalGenerated);
        BigDecimal recovered = sum(fiche, MonthlyEvidenceResponse::totalRecovered);
        BigDecimal disposed = sum(fiche, MonthlyEvidenceResponse::totalDisposed);

        assertThat(generated).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("1200"));
        assertThat(recovered).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("1000"));
        assertThat(disposed).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("50"));

        // F26 = C26 − D26 (− E26): the sheet's own identity, and December's row has to agree with it.
        assertThat(fiche.get(11).closingStock()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(generated.subtract(recovered).subtract(disposed));
    }

    @Test
    void aHandoverForRecyclingIsReportedAsValorificata() {
        evidenceCalculator.regenerateYear(YEAR);
        MonthlyEvidenceResponse march = evidenceCalculator.list(YEAR, 3, workPoint.getId()).get(0);

        // Cap. 1 has no "predare" column: the 250 kg handed to the recycler are "valorificată",
        // and cap. 3 names the operation (R3) and the operator. The memo says it left as a
        // handover; it is not a second exit.
        assertThat(march.totalRecovered()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("250"));
        assertThat(march.totalHandedOver()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("250"));
        assertThat(march.totalDisposed()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
        assertThat(march.incomplete()).isFalse();
    }

    @Test
    void theYearOpensFromZeroWhenNothingWasCarriedOver() {
        evidenceCalculator.regenerateYear(YEAR);
        MonthlyEvidenceResponse january = evidenceCalculator.list(YEAR, 1, workPoint.getId()).get(0);

        // "Stoc: 0 kg" — the header line of the sheet, for a first year with no previous December.
        assertThat(january.closingStock()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(january.totalGenerated());
    }

    private static BigDecimal sum(List<MonthlyEvidenceResponse> fiche,
                                  java.util.function.Function<MonthlyEvidenceResponse, BigDecimal> column) {
        return fiche.stream().map(column).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void save(LocalDate date, String qty, WasteOperation op,
                      WasteOperationCode opCode, Partner partner) {
        movementRepository.save(WasteMovement.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .workPoint(workPoint).date(date).wasteCode(code)
                .quantity(new BigDecimal(qty)).unit(Unit.KG).operation(op).operationCode(opCode)
                .partner(partner)
                .deleted(false).createdBy(creatorId).build());
    }
}
