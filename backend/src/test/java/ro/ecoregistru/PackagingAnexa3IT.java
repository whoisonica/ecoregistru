package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.enums.PackagingOperatorRole;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.PackagingService;
import ro.ecoregistru.service.export.ExportFormat;
import ro.ecoregistru.service.export.PackagingAnexa3;
import ro.ecoregistru.service.export.PackagingDeclaration;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <b>Anexa 3 la Ordinul 794/2012</b> — the annual report of collectors, traders, recyclers and
 * recoverers of packaging waste, built 04.09.2026 from the act and from the blank model the
 * specialist sent ({@code documente oficiale/RAPORTARE DESEURI DE AMBALAJ COLECTATE ANUAL.ods}).
 *
 * <p>What these pin down:
 *
 * <ul>
 *   <li><b>Which table.</b> Art. 4 alin. (1) says "tabelul 1 sau, dupa caz, tabelul 2", and the
 *       company profile says which. Unanswered means no document at all, not a guessed one;</li>
 *   <li><b>the art. 48 register only.</b> Waste the company generated itself belongs on anexa 1
 *       and must not leak in here — the split HG 856/2002 art. 2 alin. (1) draws;</li>
 *   <li><b>provenance comes from the partner</b>, with the movement overriding it, and
 *       {@code POPULATIE} is reachable only through that override;</li>
 *   <li><b>recycling is R3/R4/R5</b>, read from the titles of OUG 92/2021 anexa nr. 3, so R1
 *       (burning it for energy) lands in "valorificata prin alte metode";</li>
 *   <li>a takeover nobody classified is <b>listed as unclassified</b>, not swept into a row.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PackagingAnexa3IT {

    private static final int YEAR = 2026;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PackagingService packagingService;

    private String token;
    private UUID tenantId;
    private UUID workPointId;
    private Company company;
    private Partner generatorSource;
    private Partner collectorSource;
    private Partner recipient;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        company = companyRepository.save(Company.builder()
                .name("Colectare Ambalaje " + suffix).cui("ROC" + suffix)
                // BOTH, because only a company that takes waste over from third parties keeps the
                // art. 48 register this document reads.
                .type(CompanyType.BOTH)
                .address("Cluj-Napoca, str. Exemplu nr. 1")
                .caenCode("3832")
                .environmentalAuthNumber("AM 214/12.03.2024")
                .contactName("Ion Popescu").contactRole("Manager Mediu")
                .packagingOperatorRole(PackagingOperatorRole.COLECTOR)
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();

        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("anexa3+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true)
                .createdAt(Instant.now()).build());
        token = jwtService.generateToken(admin);

        workPointId = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Statie sortare").address("Str. Depozitului 4")
                .active(true).createdAt(Instant.now()).build()).getId();

        generatorSource = partnerRepository.save(Partner.builder()
                .company(company).name("Fabrica Ambalate SRL").cui("RO111" + suffix.substring(0, 3))
                .type(PartnerType.GENERATOR).supplier(true).active(true)
                .packagingOrigin(PackagingOrigin.GENERATOR_PJ)
                .createdAt(Instant.now()).build());

        collectorSource = partnerRepository.save(Partner.builder()
                .company(company).name("Colector Mic SRL").cui("RO222" + suffix.substring(0, 3))
                .type(PartnerType.COLLECTOR).supplier(true).active(true)
                .packagingOrigin(PackagingOrigin.COLECTOR)
                .createdAt(Instant.now()).build());

        recipient = partnerRepository.save(Partner.builder()
                .company(company).name("Reciclator Hartie SA").cui("RO333" + suffix.substring(0, 3))
                .type(PartnerType.RECOVERER).client(true).active(true)
                .createdAt(Instant.now()).build());
    }

    // ------------------------------------------------------------------ the left half

    /**
     * Provenance is a sub-row under each material, which is how the received model draws it: one
     * line per source, and the same material can have several.
     */
    @Test
    void takeoversLandOnOneRowPerMaterialAndProvenance() throws Exception {
        takeover("15 01 01", "300", generatorSource.getId(), null);
        takeover("15 01 01", "200", collectorSource.getId(), null);
        takeover("15 01 07", "80", generatorSource.getId(), null);

        PackagingAnexa3 d = anexa3();

        assertThat(d.intake()).hasSize(3);
        assertThat(intake(d, PackagingMaterial.HARTIE_CARTON, PackagingOrigin.GENERATOR_PJ).total())
                .isEqualByComparingTo("300");
        assertThat(intake(d, PackagingMaterial.HARTIE_CARTON, PackagingOrigin.COLECTOR).total())
                .isEqualByComparingTo("200");
        assertThat(intake(d, PackagingMaterial.STICLA, PackagingOrigin.GENERATOR_PJ).total())
                .isEqualByComparingTo("80");
    }

    /**
     * The provenance is answered once on the partner. Nota 2 describes the source, not the
     * transport, so a collector one buys from is a collector on every load.
     */
    @Test
    void provenanceIsTakenFromThePartnerWhenTheMovementSaysNothing() throws Exception {
        takeover("15 01 01", "100", collectorSource.getId(), null);

        assertThat(anexa3().intake()).singleElement()
                .satisfies(r -> assertThat(r.origin()).isEqualTo(PackagingOrigin.COLECTOR));
    }

    /**
     * "populatie" is the reason the per-movement override exists at all: a natural person has no
     * CUI and no authorization and is never a partner, so without the override that row of the
     * form could not be filled — and a collection centre buys from natural persons daily.
     */
    @Test
    void populationIsReachableOnlyThroughTheMovement() throws Exception {
        takeover("15 01 01", "40", null, "POPULATIE");

        assertThat(anexa3().intake()).singleElement()
                .satisfies(r -> assertThat(r.origin()).isEqualTo(PackagingOrigin.POPULATIE));
    }

    /** The movement wins over the partner: one load that came from somewhere unusual. */
    @Test
    void theMovementOverridesThePartner() throws Exception {
        takeover("15 01 01", "70", collectorSource.getId(), "COMERCIANT");

        assertThat(anexa3().intake()).singleElement()
                .satisfies(r -> assertThat(r.origin()).isEqualTo(PackagingOrigin.COMERCIANT));
    }

    /**
     * Nobody said where it came from, so it stays off the table and is named instead. A form filed
     * with an authority gets no guessed rubric (regula de lucru 1).
     */
    @Test
    void aTakeoverWithoutProvenanceIsListedRatherThanPlaced() throws Exception {
        Partner nameless = partnerRepository.save(Partner.builder()
                .company(company).name("Fara provenienta SRL")
                .type(PartnerType.GENERATOR).supplier(true).active(true)
                .createdAt(Instant.now()).build());
        takeover("15 01 01", "50", nameless.getId(), null);

        PackagingAnexa3 d = anexa3();

        assertThat(d.intake()).isEmpty();
        assertThat(d.unclassified()).singleElement().satisfies(r -> {
            assertThat(r.missingOrigin()).isTrue();
            assertThat(r.missingMaterial()).isFalse();
            assertThat(r.partnerName()).isEqualTo("Fara provenienta SRL");
        });
    }

    /**
     * {@code 15 01 04} is aluminium and steel at once, so the European List cannot settle the row
     * and nobody chose one. Same treatment as anexa 1 gives it: shown, not swept into "Altele".
     */
    @Test
    void aTakeoverWithoutAMaterialIsListedToo() throws Exception {
        takeover("15 01 04", "60", generatorSource.getId(), null);

        PackagingAnexa3 d = anexa3();

        assertThat(d.intake()).isEmpty();
        assertThat(d.unclassified()).singleElement()
                .satisfies(r -> assertThat(r.missingMaterial()).isTrue());
    }

    /**
     * The split HG 856/2002 art. 2 alin. (1) draws: waste the company generated in its own activity
     * is anexa 1's business and has no place on this form. If this ever fails, the two registers
     * have started leaking into each other.
     */
    @Test
    void wasteGeneratedByTheCompanyItselfNeverReachesThisForm() throws Exception {
        createMovement("""
                {
                  "workPointId": "%s", "date": "%d-05-10", "wasteCodeId": "%s",
                  "unit": "KG", "quantity": 500, "operation": "GENERATED",
                  "register": "ANEXA_1", "packagingOnMarket": true
                }
                """.formatted(workPointId, YEAR, codeId("15 01 01")));

        PackagingAnexa3 d = anexa3();

        assertThat(d.intake()).isEmpty();
        assertThat(d.unclassified()).isEmpty();
    }

    // ------------------------------------------------------------------ tabelul 1, right half

    /** One line per operator, the same rule anexa 1's tabelul 2 states in writing. */
    @Test
    void whatWentOnIsOneLinePerOperator() throws Exception {
        takeover("15 01 01", "500", generatorSource.getId(), null);
        exit("15 01 01", "300", recipient.getId(), "R3");
        exit("15 01 01", "150", collectorSource.getId(), "R13");

        assertThat(anexa3().handovers()).hasSize(2)
                .anySatisfy(r -> {
                    assertThat(r.operatorName()).isEqualTo("Reciclator Hartie SA");
                    assertThat(r.quantity()).isEqualByComparingTo("300");
                })
                .anySatisfy(r -> assertThat(r.quantity()).isEqualByComparingTo("150"));
    }

    /** Two loads to the same operator are one line, summed — the line is per operator, not per load. */
    @Test
    void twoLoadsToTheSameOperatorAreOneLine() throws Exception {
        takeover("15 01 01", "500", generatorSource.getId(), null);
        exit("15 01 01", "300", recipient.getId(), "R3");
        exit("15 01 01", "120", recipient.getId(), "R3");

        assertThat(anexa3().handovers()).singleElement()
                .satisfies(r -> assertThat(r.quantity()).isEqualByComparingTo("420"));
    }

    // ------------------------------------------------------------------ tabelul 2, right half

    /**
     * The column split, read from the act rather than guessed. OUG 92/2021 anexa nr. 3 titles R3,
     * R4 and R5 "Reciclarea/Recuperarea"; R1 is "intrebuintarea in principal drept combustibil",
     * which the Waste Framework Directive excludes from recycling by name.
     */
    @Test
    void recyclingIsR3R4R5AndBurningItIsNot() throws Exception {
        takeover("15 01 01", "500", generatorSource.getId(), null);
        exit("15 01 01", "300", recipient.getId(), "R3");
        exit("15 01 01", "150", recipient.getId(), "R1");

        assertThat(anexa3().treatments()).singleElement().satisfies(t -> {
            assertThat(t.recycled()).isEqualByComparingTo("300");
            assertThat(t.otherRecovery()).isEqualByComparingTo("150");
            assertThat(t.methods()).containsExactlyInAnyOrder(
                    WasteOperationCode.R3, WasteOperationCode.R1);
        });
    }

    /**
     * Disposal is not recovery, and tabelul 2 has no cell for it. The quantity still shows on the
     * left, as taken over; it simply has no line on the right, which is the truthful picture.
     */
    @Test
    void disposalCountsInNeitherColumn() throws Exception {
        takeover("15 01 01", "500", generatorSource.getId(), null);
        exit("15 01 01", "200", recipient.getId(), "D1");

        PackagingAnexa3 d = anexa3();

        assertThat(d.treatments()).isEmpty();
        assertThat(intake(d, PackagingMaterial.HARTIE_CARTON, PackagingOrigin.GENERATOR_PJ).total())
                .isEqualByComparingTo("500");
    }

    // ------------------------------------------------------------------ which table, and the file

    /** Art. 4 alin. (1), from the profile: a collector fills tabelul 1. */
    @Test
    void theProfileDecidesWhichTable() {
        assertThat(anexa3().usesTable2()).isFalse();

        company.setPackagingOperatorRole(PackagingOperatorRole.RECICLATOR);
        companyRepository.save(company);

        assertThat(anexa3().usesTable2()).isTrue();
    }

    /**
     * Unanswered means no document. A screen is an offer and may appear on an empty profile
     * (decision 6); a document is an assertion, and printing "Colectori/Comercianti" in the header
     * would assert the client's legal quality on their behalf (decision 37).
     */
    @Test
    void nothingPrintsUntilTheProfileSaysWhichTable() {
        company.setPackagingOperatorRole(null);
        companyRepository.save(company);

        assertThat(anexa3().printable()).isFalse();
        assertThatThrownBy(() -> render(ExportFormat.XLS))
                .hasMessageContaining("calitatea firmei");
    }

    /**
     * Art. 6 names the format on sight: ".xls", which is BIFF8 and starts with {@code d0 cf 11 e0}
     * — not the OOXML an {@code .xlsx} would produce. And it must be protected: the same article
     * says "protejat impotriva modificarii datelor".
     */
    @Test
    void theDownloadIsARealXlsAndItIsProtected() throws Exception {
        takeover("15 01 01", "300", generatorSource.getId(), null);

        byte[] bytes = render(ExportFormat.XLS);

        assertThat(bytes).startsWith((byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0);
        try (Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("Tabelul nr. 1");
            assertThat(wb.getSheetAt(0).getProtect()).isTrue();
        }
    }

    /** The recycler gets the other sheet, and only that one — art. 4 alin. (1) says one or the other. */
    @Test
    void aRecyclerGetsTabelul2Only() throws Exception {
        company.setPackagingOperatorRole(PackagingOperatorRole.RECICLATOR);
        companyRepository.save(company);
        takeover("15 01 01", "300", generatorSource.getId(), null);

        try (Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(render(ExportFormat.XLS)))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("Tabelul nr. 2");
        }
    }

    /**
     * PET is closed by "total plastic", never by a "total pet" of its own. The blank model does it
     * that way, and the reason is not cosmetic: the same kilograms printed twice under two labels
     * read as two different quantities on a form filed with an authority.
     *
     * <p>Caught by looking at the rendered sheet, not by a test — regula de lucru 5. The first
     * version printed both lines, one directly under the other, with the identical figure.
     */
    @Test
    void aMaterialSummedIntoAGroupGetsNoTotalOfItsOwn() throws Exception {
        takeoverOfMaterial("15 01 02", "980", "PET");

        List<String> labels = firstColumn();

        assertThat(labels).contains("total plastic");
        assertThat(labels).noneSatisfy(l -> assertThat(l).startsWith("total pet"));
    }

    /** Hârtie carton stands on its own, so it does keep a total line. */
    @Test
    void aStandaloneMaterialKeepsItsOwnTotal() throws Exception {
        takeover("15 01 01", "300", generatorSource.getId(), null);

        assertThat(firstColumn()).anySatisfy(l -> assertThat(l).startsWith("total h"));
    }

    /**
     * No row pairs a provenance with an operator's kilograms. The two halves have different
     * cardinalities, and aligning them put "22.000 kg → Reciclator SA" on the same line as
     * "populaţie", which reads as a claim about where that load came from. On paper the material's
     * rubric is a box; a bordered row here looks like a statement.
     *
     * <p>Also found by looking at the rendered document rather than by a failing assertion.
     */
    @Test
    void noRowPairsAProvenanceWithAnOperatorFigure() throws Exception {
        takeover("15 01 01", "300", generatorSource.getId(), null);
        takeover("15 01 01", "200", collectorSource.getId(), null);
        exit("15 01 01", "400", recipient.getId(), "R3");

        try (Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(render(ExportFormat.XLS)))) {
            Sheet sh = wb.getSheetAt(0);
            for (int r = 0; r <= sh.getLastRowNum(); r++) {
                String provenance = text(sh, r, 4);
                String outbound = text(sh, r, 5);
                // Only real body rows: the heading band literally prints "3" under Provenienta
                // and "4" under the outbound quantity, so matching on the official labels is both
                // stricter and clearer than trying to exclude the headings.
                if (isProvenanceLabel(provenance)) {
                    assertThat(outbound)
                            .as("row %d pairs '%s' with an outbound figure", r, provenance)
                            .isBlank();
                }
            }
        }
    }

    /**
     * Both figures of tabelul 2 are numbers in the spreadsheet, and the totals count both.
     *
     * <p>The XLS is a separate code path from the PDF, and the PDF was the one rendered and looked
     * at. Here the "valorificata prin alte metode" column went through the string branch of the
     * shared row writer, so it landed as a text cell that Excel would not sum, and the material and
     * grand totals added only the recycled half.
     */
    @Test
    void bothColumnsOfTabelul2AreNumbersAndBothCountInTheTotals() throws Exception {
        company.setPackagingOperatorRole(PackagingOperatorRole.RECICLATOR);
        companyRepository.save(company);
        takeover("15 01 01", "500", generatorSource.getId(), null);
        exit("15 01 01", "300", recipient.getId(), "R3");
        exit("15 01 01", "150", recipient.getId(), "R1");

        try (Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(render(ExportFormat.XLS)))) {
            Sheet sh = wb.getSheetAt(0);
            int row = rowOfLabel(sh, "total h");

            assertThat(numeric(sh, row, 5))
                    .as("cantitatea reciclata on the material total")
                    .isEqualTo(300.0);
            assertThat(numeric(sh, row, 6))
                    .as("cantitatea valorificata prin alte metode on the material total")
                    .isEqualTo(150.0);

            int grand = rowOfLabel(sh, "TOTAL ambalaje");
            assertThat(numeric(sh, grand, 5)).isEqualTo(300.0);
            assertThat(numeric(sh, grand, 6)).isEqualTo(150.0);

            // And on the detail line the two figures are numeric cells, not text.
            int detail = rowOfLabel(sh, "H") + 1;
            assertThat(sh.getRow(detail).getCell(5).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(sh.getRow(detail).getCell(6).getCellType()).isEqualTo(CellType.NUMERIC);
        }
    }

    /**
     * A takeover still waiting to be weighed must not vanish. Quantity is nullable by design
     * (decision 5), so a load recorded before the scale figure comes back has no kilograms yet —
     * and the house rule is that what is missing is shown to be missing, not filtered away.
     */
    @Test
    void aTakeoverStillAwaitingItsWeightIsShownRatherThanDropped() throws Exception {
        createMovement("""
                {
                  "workPointId": "%s", "date": "%d-05-12", "wasteCodeId": "%s",
                  "unit": "KG", "operation": "COLLECTED", "weighedAtUnloading": true,
                  "partnerId": "%s"
                }
                """.formatted(workPointId, YEAR, codeId("15 01 01"), generatorSource.getId()));

        PackagingAnexa3 d = anexa3();

        assertThat(d.intake()).as("no kilograms yet, so no row in the table").isEmpty();
        assertThat(d.unclassified()).as("but it has to be visible somewhere").hasSize(1);
    }

    /**
     * "din care periculoase" is a sub-column of the total beside it, so the total rows sum it too.
     *
     * <p>They did not: a material line showed 120 kg hazardous while its own "total metal" line
     * left the cell empty, so the document contradicted itself. Found by reading the rendered
     * sheet, like the other two layout defects.
     */
    @Test
    void theTotalRowsSumTheHazardousColumnToo() throws Exception {
        // 15 01 10 — ambalaje care conţin reziduuri de substanţe periculoase. Codul se ţine fără
        // asterisc în nomenclator: periculozitatea e flagul, iar asteriscul îl pune
        // WasteCodeLabel la tipărire (decizia 33).
        takeoverOfMaterial("15 01 10", "200", "OTEL");

        try (Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(render(ExportFormat.XLS)))) {
            Sheet sh = wb.getSheetAt(0);
            assertThat(numeric(sh, rowOfLabel(sh, "total metal"), 3)).isEqualTo(200.0);
            assertThat(numeric(sh, rowOfLabel(sh, "TOTAL ambalaje"), 3)).isEqualTo(200.0);
        }
    }

    /**
     * The two outbound figures of tabelul 2 stay in their own columns when they are summed.
     *
     * <p>They did not, in the PDF: it folded both into "cantitatea reciclata" and left the
     * neighbouring column empty, so a form reported 450 kg recycled where 300 were recycled and 150
     * burned for energy. The spreadsheet had it right, which is what made the two documents
     * disagree — each generator summed on its own. The totals now live on
     * {@link PackagingAnexa3.Totals}, so this test covers both printers at once.
     */
    @Test
    void aSummedLineKeepsRecyclingApartFromOtherRecovery() throws Exception {
        company.setPackagingOperatorRole(PackagingOperatorRole.RECICLATOR);
        companyRepository.save(company);
        takeover("15 01 01", "500", generatorSource.getId(), null);
        exit("15 01 01", "300", recipient.getId(), "R3");
        exit("15 01 01", "150", recipient.getId(), "R1");

        PackagingAnexa3 d = anexa3();

        assertThat(d.totalsFor(PackagingMaterial.HARTIE_CARTON)).satisfies(x -> {
            assertThat(x.first()).as("reciclata").isEqualByComparingTo("300");
            assertThat(x.second()).as("valorificata prin alte metode").isEqualByComparingTo("150");
        });
        assertThat(d.grandTotals()).satisfies(x -> {
            assertThat(x.intake()).isEqualByComparingTo("500");
            assertThat(x.first()).isEqualByComparingTo("300");
            assertThat(x.second()).isEqualByComparingTo("150");
        });
    }

    /**
     * On tabelul 1 the second outbound column holds an operator's name, so it totals nothing — and
     * everything that left counts in the first, whichever R or D code it carried.
     */
    @Test
    void onTabelul1EverythingThatLeftCountsInOneColumn() throws Exception {
        takeover("15 01 01", "500", generatorSource.getId(), null);
        exit("15 01 01", "300", recipient.getId(), "R3");
        exit("15 01 01", "150", collectorSource.getId(), "R13");

        assertThat(anexa3().grandTotals()).satisfies(x -> {
            assertThat(x.first()).isEqualByComparingTo("450");
            assertThat(x.second()).isEqualByComparingTo("0");
        });
    }

    /**
     * The incident of 25.08.2026, as a regression test: 300 kg of 15 01 01 taken over from a shop
     * and passed on to a recycler used to show up in tabelul 1 of Anexa 1 Ambalaje — that is, this
     * company declared, to the authority, that it had put someone else's cardboard on the national
     * market. Anexa 3 is where those kilograms belong; the declaration of own packaging must not
     * see them at all, in any of its three lists.
     */
    @Test
    void goodsTakenOverAndPassedOnAreNotDeclaredAsOwnPackaging() throws Exception {
        takeover("15 01 01", "300", generatorSource.getId(), null);
        exit("15 01 01", "300", recipient.getId(), "R3");

        PackagingDeclaration declaration = declaration();

        assertThat(declaration.handoverRows()).isEmpty();
        assertThat(declaration.unclassified()).isEmpty();
        assertThat(declaration.marketRows())
                .filteredOn(r -> r.material() == PackagingMaterial.HARTIE_CARTON)
                .allSatisfy(r -> {
                    assertThat(r.salesPackaging()).isNull();
                    assertThat(r.primaryTotal()).isNull();
                    assertThat(r.secondaryTotal()).isNull();
                });
    }

    /** Art. 6 asks for the paper copy beside the file, so the PDF is half a legal requirement. */
    @Test
    void thePaperCopyRenders() throws Exception {
        takeover("15 01 01", "300", generatorSource.getId(), null);

        assertThat(render(ExportFormat.PDF)).startsWith("%PDF".getBytes());
    }

    // ------------------------------------------------------------------ helpers

    private PackagingAnexa3 anexa3() {
        TenantContext.set(tenantId);
        try {
            return packagingService.anexa3(YEAR, workPointId);
        } finally {
            TenantContext.clear();
        }
    }

    private PackagingDeclaration declaration() {
        TenantContext.set(tenantId);
        try {
            return packagingService.declaration(YEAR);
        } finally {
            TenantContext.clear();
        }
    }

    private byte[] render(ExportFormat format) {
        TenantContext.set(tenantId);
        try {
            return packagingService.renderAnexa3(YEAR, workPointId, format);
        } finally {
            TenantContext.clear();
        }
    }

    private PackagingAnexa3.IntakeRow intake(PackagingAnexa3 d, PackagingMaterial material,
                                             PackagingOrigin origin) {
        return d.intake().stream()
                .filter(r -> r.material() == material && r.origin() == origin)
                .findFirst().orElseThrow(() ->
                        new AssertionError("no row for " + material + " / " + origin));
    }

    /** A takeover from a third party: {@code COLLECTED}, which always lands in the art. 48 register. */
    private void takeover(String code, String quantity, UUID partnerId, String origin)
            throws Exception {
        StringBuilder extra = new StringBuilder();
        if (partnerId != null) {
            extra.append(", \"partnerId\": \"").append(partnerId).append('"');
        }
        if (origin != null) {
            extra.append(", \"packagingOrigin\": \"").append(origin).append('"');
        }
        createMovement("""
                {
                  "workPointId": "%s", "date": "%d-05-12", "wasteCodeId": "%s",
                  "unit": "KG", "quantity": %s, "operation": "COLLECTED"%s
                }
                """.formatted(workPointId, YEAR, codeId(code), quantity, extra));
    }

    /**
     * Passing goods on. The register is stated explicitly because this account can take waste over
     * (decision 23): on a COLLECTOR/BOTH account an exit could belong to either register, so the
     * form asks instead of defaulting.
     */
    private void exit(String code, String quantity, UUID partnerId, String operationCode)
            throws Exception {
        createMovement("""
                {
                  "workPointId": "%s", "date": "%d-06-12", "wasteCodeId": "%s",
                  "unit": "KG", "quantity": %s, "operation": "%s", "operationCode": "%s",
                  "partnerId": "%s", "register": "ART_48"
                }
                """.formatted(workPointId, YEAR, codeId(code), quantity,
                operationCode.startsWith("R") ? "RECOVERED" : "DISPOSED",
                operationCode, partnerId));
    }

    /** The index of the first row whose label column starts with the given text. */
    private int rowOfLabel(Sheet sh, String prefix) {
        for (int r = 0; r <= sh.getLastRowNum(); r++) {
            if (text(sh, r, 1).startsWith(prefix)) {
                return r;
            }
        }
        throw new AssertionError("no row labelled " + prefix);
    }

    private double numeric(Sheet sh, int r, int c) {
        Cell cell = sh.getRow(r).getCell(c);
        assertThat(cell.getCellType())
                .as("cell (%d,%d) should be numeric but was %s", r, c, cell.getCellType())
                .isEqualTo(CellType.NUMERIC);
        return cell.getNumericCellValue();
    }

    /** True when the cell holds one of the four words nota 2 allows. */
    private boolean isProvenanceLabel(String value) {
        for (PackagingOrigin origin : PackagingOrigin.values()) {
            if (origin.getOfficialLabel().equals(value)) {
                return true;
            }
        }
        return false;
    }

    /** The label column of the rendered sheet, trimmed, blanks dropped. */
    private List<String> firstColumn() throws Exception {
        try (Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(render(ExportFormat.XLS)))) {
            Sheet sh = wb.getSheetAt(0);
            List<String> labels = new ArrayList<>();
            for (int r = 0; r <= sh.getLastRowNum(); r++) {
                String value = text(sh, r, 1);
                if (!value.isBlank()) {
                    labels.add(value);
                }
            }
            return labels;
        }
    }

    private String text(Sheet sh, int r, int c) {
        Row row = sh.getRow(r);
        if (row == null || row.getCell(c) == null) {
            return "";
        }
        Cell cell = row.getCell(c);
        return cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : "";
    }

    /** A takeover whose material the code cannot settle, so the client chooses it. */
    private void takeoverOfMaterial(String code, String quantity, String material)
            throws Exception {
        createMovement("""
                {
                  "workPointId": "%s", "date": "%d-05-12", "wasteCodeId": "%s",
                  "unit": "KG", "quantity": %s, "operation": "COLLECTED",
                  "partnerId": "%s", "packagingMaterial": "%s"
                }
                """.formatted(workPointId, YEAR, codeId(code), quantity,
                generatorSource.getId(), material));
    }

    private UUID codeId(String code) {
        return wasteCodeRepository.findByCode(code).orElseThrow().getId();
    }

    private void createMovement(String body) throws Exception {
        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }
}
