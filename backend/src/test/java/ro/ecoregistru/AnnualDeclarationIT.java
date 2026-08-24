package ro.ecoregistru;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
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
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.EvidenceCalculator;
import ro.ecoregistru.service.export.Anexa1Sheet;
import ro.ecoregistru.service.export.AnnualDeclaration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FELIA G6 — the annual declaration ("centralizator"): the summary sheet that goes in front of the
 * Anexa 1 pages, one line per waste code and one page per work point.
 *
 * <p>Modelled on the {@code raportare deseuri generate} sheet, which nine of the filled workbooks
 * in {@code documente oficiale/} carry, plus the blank template the specialist sent. Those files
 * are gitignored, so nothing here reads them: what the corpus establishes is written as a fixture
 * and pinned by these tests.
 *
 * <p>What they pin down:
 *
 * <ul>
 *   <li>one sheet per work point — every model in the corpus is a single site;</li>
 *   <li>the row folds the twelve months and still balances: stoc final = stoc iniţial + generat
 *       − valorificat − eliminat;</li>
 *   <li>the opening stock is the same figure the fişa prints in its header — the two documents are
 *       read side by side and must not disagree;</li>
 *   <li>"valorificat prin" carries the code and the operator, both of them when a year had two;</li>
 *   <li>a takeover from a third party stays out (HG 856/2002 art. 2 alin. (1));</li>
 *   <li>a quantity that left with no R/D code is flagged, not quietly absorbed.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class AnnualDeclarationIT {

    private static final int YEAR = 2025;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired EvidenceCalculator evidenceCalculator;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;

    private UUID tenantId;
    private UUID creatorId;
    private String token;
    private WorkPoint cluj;
    private WorkPoint turda;
    private WasteCode paper;
    private WasteCode household;
    private Partner collector;
    private Partner recycler;
    private Partner sanitation;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        // BOTH, because the fixture records one takeover: only a company that actually takes waste
        // over keeps an art. 48 register, and the takeover is the point of one of the tests.
        Company company = companyRepository.save(Company.builder()
                .name("Declaratie Co SRL").cui("ROD" + suffix).type(CompanyType.BOTH)
                .address("Cluj-Napoca, str. Test nr. 1")
                .caenCode("4677")
                .contactName("Andreea Oprea").contactRole("Manager Mediu")
                .contactPhone("0700000000").contactEmail("mediu@declaratie.ro")
                .environmentalAuthNumber("61/21.12.2012")
                .environmentalAuthExpiry(LocalDate.of(2030, 1, 1))
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();

        AppUser user = appUserRepository.save(AppUser.builder()
                .email("decl+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        creatorId = user.getId();
        token = jwtService.generateToken(user);

        cluj = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Cluj").active(true).createdAt(Instant.now()).build());
        turda = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Turda").active(true).createdAt(Instant.now()).build());

        List<WasteCode> codes = wasteCodeRepository.findAll();
        paper = codes.get(0);
        household = codes.get(1);

        collector = partner(company, "Colector SRL", "RO1" + suffix);
        recycler = partner(company, "Reciclator SRL", "RO2" + suffix);
        sanitation = partner(company, "Salubritate SRL", "RO3" + suffix);

        // A year before the declared one, so the opening stock is a real carried figure and not
        // the trivial zero — that is what makes the "agrees with the fişa" test worth having.
        save(cluj, paper, LocalDate.of(2024, 12, 10), "100.000", WasteOperation.GENERATED, null, null);

        // PL Cluj / paper: 100 carried + 400 generated − 300 (R3) − 50 (R13) = 150
        save(cluj, paper, LocalDate.of(YEAR, 1, 10), "400.000", WasteOperation.GENERATED, null, null);
        save(cluj, paper, LocalDate.of(YEAR, 2, 12), "300.000", WasteOperation.RECOVERED, WasteOperationCode.R3, collector);
        save(cluj, paper, LocalDate.of(YEAR, 9, 3), "50.000", WasteOperation.RECOVERED, WasteOperationCode.R13, recycler);
        // ...and a takeover, which belongs to the art. 48 register and to no line of this sheet.
        save(cluj, paper, LocalDate.of(YEAR, 5, 6), "200.000", WasteOperation.COLLECTED, null, collector);

        // PL Cluj / household: 200 generated − 150 (D5) − 20 with no code at all = 30
        save(cluj, household, LocalDate.of(YEAR, 3, 4), "200.000", WasteOperation.GENERATED, null, null);
        save(cluj, household, LocalDate.of(YEAR, 4, 4), "150.000", WasteOperation.DISPOSED, WasteOperationCode.D5, sanitation);
        save(cluj, household, LocalDate.of(YEAR, 11, 4), "20.000", WasteOperation.UNCLASSIFIED_OUT, null, sanitation);

        // PL Turda / paper: generated and still on site.
        save(turda, paper, LocalDate.of(YEAR, 6, 1), "60.000", WasteOperation.GENERATED, null, null);

        TenantContext.set(tenantId);
        evidenceCalculator.regenerateYear(2024);
        evidenceCalculator.regenerateYear(YEAR);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    /** Every model in the corpus is one site: "punct de lucru: CLUJ", "Punct de lucru Bragadiru". */
    @Test
    void oneSheetPerWorkPointOneRowPerWasteCode() {
        List<AnnualDeclaration> declarations = evidenceCalculator.annualDeclaration(YEAR, null);

        assertThat(declarations).hasSize(2);
        assertThat(declarations).extracting(AnnualDeclaration::workPointName)
                .containsExactly("PL Cluj", "PL Turda");
        assertThat(declarations.get(0).rows()).hasSize(2);
        assertThat(declarations.get(1).rows()).hasSize(1);
    }

    /**
     * The identity the sheet asserts across every row. It is the engine's own formula, folded to
     * the year — the declaration never recomputes it, which is why it cannot drift from the fişa.
     */
    @Test
    void everyRowBalancesToItsClosingStock() {
        for (AnnualDeclaration d : evidenceCalculator.annualDeclaration(YEAR, null)) {
            for (AnnualDeclaration.Row row : d.rows()) {
                BigDecimal expected = row.openingStock()
                        .add(row.generated())
                        .subtract(row.recovered())
                        .subtract(row.disposed())
                        .subtract(row.unclassifiedOut());
                assertThat(row.closingStock()).usingComparator(BigDecimal::compareTo)
                        .isEqualTo(expected);
            }
        }

        AnnualDeclaration.Row paperRow = row("PL Cluj", paper.getCode());
        assertThat(paperRow.openingStock()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("100"));
        assertThat(paperRow.generated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("400"));
        assertThat(paperRow.recovered()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("350"));
        assertThat(paperRow.closingStock()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("150"));
    }

    /**
     * The two documents of one year are read side by side — the summary and the twelve-row sheet
     * behind it. A client who finds two different opening stocks on them stops trusting both.
     */
    @Test
    void theOpeningStockAgreesWithTheFisa() {
        Anexa1Sheet sheet = evidenceCalculator.anexa1(YEAR, cluj.getId()).stream()
                .filter(s -> s.wasteCode().equals(paper.getCode()))
                .findFirst()
                .orElseThrow();

        assertThat(row("PL Cluj", paper.getCode()).openingStock())
                .usingComparator(BigDecimal::compareTo)
                .isEqualTo(sheet.openingStock());
    }

    /**
     * "Valorificat prin:" is the code and the operator, as the models write it ("R3 - Hamburger
     * Recycling Group GMBH"). A year with two recyclers prints both: the sheet has one row per
     * code, and dropping one would hide a handover that happened.
     */
    @Test
    void throughWhomCarriesTheCodeAndTheOperator() {
        AnnualDeclaration.Row paperRow = row("PL Cluj", paper.getCode());
        assertThat(paperRow.recoveredThrough())
                .isEqualTo("R3 - Colector SRL; R13 - Reciclator SRL");
        assertThat(paperRow.disposedThrough()).isEmpty();

        AnnualDeclaration.Row householdRow = row("PL Cluj", household.getCode());
        assertThat(householdRow.disposedThrough()).isEqualTo("D5 - Salubritate SRL");
        assertThat(householdRow.recoveredThrough()).isEmpty();
    }

    /**
     * The 200 kg taken over from a third party in May is art. 48 business and reaches no line of
     * this sheet — HG 856/2002 art. 2 alin. (1). Were it counted, "Generat" would read 600.
     */
    @Test
    void takeoverStaysOutOfTheDeclaration() {
        assertThat(row("PL Cluj", paper.getCode()).generated())
                .usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("400"));
    }

    /**
     * The 20 kg that left in November with no R/D code are in neither official column, so the row
     * does not add up on its face. It is marked and explained rather than absorbed into one of the
     * two columns — putting it in either would be inventing an operation nobody recorded.
     */
    @Test
    void anExitWithoutAnOperationCodeIsFlaggedNotAbsorbed() {
        AnnualDeclaration.Row householdRow = row("PL Cluj", household.getCode());

        assertThat(householdRow.hasUnclassifiedOut()).isTrue();
        assertThat(householdRow.unclassifiedOut()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("20"));
        assertThat(householdRow.recovered()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(BigDecimal.ZERO);
        assertThat(householdRow.disposed()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("150"));

        assertThat(row("PL Turda", paper.getCode()).hasUnclassifiedOut()).isFalse();
    }

    @Test
    void theDeclarationIsAPdfWithOnePagePerWorkPoint() throws Exception {
        PdfReader reader = new PdfReader(pdf());
        try {
            assertThat(reader.getNumberOfPages()).isEqualTo(2);
        } finally {
            reader.close();
        }
    }

    /**
     * The header rubrics of the model, on the page itself: the title with the year, the unit every
     * filled sheet declares, and the CAEN code — the one field the whole slice needed a migration
     * for, because it cannot be derived from anything else we hold.
     */
    @Test
    void thePageCarriesTheHeaderOfTheModel() throws Exception {
        String text = firstPage();

        assertThat(text).contains("Evidenţa gestiunii deşeurilor generate " + YEAR);
        assertThat(text).contains("4677");            // Cod CAEN
        assertThat(text).contains("PL Cluj");
        assertThat(text).contains("Manager Mediu");   // the signature block's "Funcţia:"
        assertThat(text).contains("kg");
    }

    /** The footnote is on the page that needs it, and only there. */
    @Test
    void thePageExplainsAStarredRow() throws Exception {
        assertThat(firstPage()).contains("R/D");           // the note is the only place it appears
        assertThat(secondPage()).doesNotContain("R/D");    // PL Turda has nothing unclassified
    }

    // --- helpers ---

    private AnnualDeclaration.Row row(String workPointName, String wasteCode) {
        return evidenceCalculator.annualDeclaration(YEAR, null).stream()
                .filter(d -> d.workPointName().equals(workPointName))
                .flatMap(d -> d.rows().stream())
                .filter(r -> r.wasteCode().equals(wasteCode))
                .findFirst()
                .orElseThrow();
    }

    private byte[] pdf() throws Exception {
        return mockMvc.perform(get("/api/v1/evidences/declaratie-anuala?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private String firstPage() throws Exception {
        return page(1);
    }

    private String secondPage() throws Exception {
        return page(2);
    }

    private String page(int number) throws Exception {
        PdfReader reader = new PdfReader(pdf());
        try {
            return new PdfTextExtractor(reader).getTextFromPage(number);
        } finally {
            reader.close();
        }
    }

    private Partner partner(Company company, String name, String cui) {
        return partnerRepository.save(Partner.builder()
                .company(company).name(name).cui(cui.substring(0, Math.min(cui.length(), 12)))
                .type(PartnerType.COLLECTOR).supplier(true)
                .active(true).createdAt(Instant.now()).build());
    }

    private void save(WorkPoint workPoint, WasteCode code, LocalDate date, String qty,
                      WasteOperation operation, WasteOperationCode operationCode, Partner partner) {
        movementRepository.save(WasteMovement.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .workPoint(workPoint).date(date).wasteCode(code)
                .quantity(new BigDecimal(qty)).unit(Unit.KG)
                .operation(operation).operationCode(operationCode).partner(partner)
                .deleted(false).createdBy(creatorId).build());
    }
}
