package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.MarketRole;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.service.EvidenceCalculator;
import ro.ecoregistru.service.export.Anexa1Sheet;
import ro.ecoregistru.security.TenantContext;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ETAPA G5 — the Anexa 1 form itself: an identification header and four chapters, one page per
 * waste code per work point, after the filled sheets received from the specialist
 * ({@code deseuri generate_Cluj_2025_Iuhos Lorena.pdf} and the ten workbooks beside it).
 *
 * <p>What these pin down:
 *
 * <ul>
 *   <li>every sheet is twelve rows, whatever happened in the year — the form is a twelve-row
 *       table and the stock has to be readable on each line;</li>
 *   <li>chapter 1 is the evidence engine's, not a second implementation of the stock identity;</li>
 *   <li>chapter 2 counts as "treated" only what this company did itself, which is why the filled
 *       model shows zero there while chapter 3 shows the whole quantity;</li>
 *   <li>the header's opening stock is what the year started with, not what it ended with.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class Anexa1FormIT {

    private static final int YEAR = 2026;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired EvidenceCalculator evidenceCalculator;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;

    private String token;
    private AppUser admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        mockMvc.perform(post("/api/v1/evidences/regenerate?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void everySheetIsTwelveRows() {
        List<Anexa1Sheet> sheets = sheets();
        assertThat(sheets).isNotEmpty();
        assertThat(sheets).allSatisfy(s -> assertThat(s.rows()).hasSize(12));
    }

    /** The header line "Stoc/kg" is where the year opened, not where it closed. */
    @Test
    void theHeaderCarriesTheOpeningStock() {
        // The demo data starts in 2026 with nothing carried over from 2025.
        assertThat(sheets()).allSatisfy(s ->
                assertThat(s.openingStock()).usingComparator(BigDecimal::compareTo)
                        .isEqualTo(BigDecimal.ZERO));
    }

    /**
     * Both "Cant." columns of chapter 2 carry the month's quantity.
     *
     * <p>Changed on 25.08.2026, on the specialist's own printed sheets: "stocarea trebuie sa apara
     * si la cantitatea 1 si la cantitatea 2". Her corpus reads the same — all 336 filled months
     * have a figure in both. It does sit against answer U of the day before, where she confirmed
     * 0 in "Tratare: Cant." for a client who only hands the waste over; her latest word, given
     * while looking at real output, is what this pins, and the tension is question V.
     */
    @Test
    void chapterTwoCarriesTheMonthQuantityInBothColumns() {
        Anexa1Sheet paper = sheets().stream()
                .filter(s -> s.wasteCode().equals("20 01 01"))
                .findFirst()
                .orElseThrow();

        // February at Cluj: 100 kg generated, 60 kg recovered by the collector.
        Anexa1Sheet.Anexa1MonthRow february = paper.rows().get(1);
        assertThat(february.recovered()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("60.000"));
        assertThat(february.storedQuantity()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(february.generated());
        assertThat(february.treatedQuantity()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(february.generated());
        assertThat(february.recoveries()).singleElement()
                .satisfies(h -> assertThat(h.operator()).isNotBlank());
    }

    /**
     * The sheet the specialist printed from her own account on 25.08.2026, reproduced: two
     * handovers, no generation recorded anywhere, and nothing else.
     *
     * <p>It used to come out reading <em>Generate 0, valorificata 100, ramasa in stoc -100</em> —
     * a sheet nobody can file and nobody can explain. Her objection was the shortest possible one:
     * "cum poti sa valorifici ceva ce nu este generat?". The form agrees with her, in its own
     * heading: cap. 1 is "Generate — din care: valorificata | eliminata final | ramasa in stoc".
     *
     * <p>So a quantity that left is now also reported as generated, unless the recorded generation
     * or the stock carried in already covers it. Nothing is invented: it is the figure recorded on
     * the way out, acknowledged in the column it must have come from.
     */
    @Test
    void aHandoverWithNoRecordedGenerationStillReportsGeneration() throws Exception {
        Company company = admin.getCompany();
        UUID workPointId = workPointRepository.findAllByCompany_Id(company.getId()).get(0).getId();
        UUID codeId = wasteCodeRepository.findByCode("15 01 03").orElseThrow().getId();
        UUID partnerId = partnerRepository.findAllByCompany_Id(company.getId()).get(0).getId();

        String body = """
                {
                  "workPointId": "%s", "date": "%d-08-24", "wasteCodeId": "%s",
                  "unit": "KG", "quantity": 100,
                  "operation": "RECOVERED", "operationCode": "R3", "partnerId": "%s"
                }
                """.formatted(workPointId, YEAR, codeId, partnerId);
        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/evidences/regenerate?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Anexa1Sheet sheet = sheets().stream()
                .filter(s -> s.wasteCode().equals("15 01 03"))
                .findFirst().orElseThrow();
        Anexa1Sheet.Anexa1MonthRow august = sheet.rows().get(7);

        assertThat(august.generated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("100.000"));
        assertThat(august.recovered()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("100.000"));
        // The point of the whole change: the sheet closes at zero instead of at minus a hundred.
        assertThat(august.closingStock()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(BigDecimal.ZERO);
        assertThat(sheet.rows().get(11).closingStock()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(BigDecimal.ZERO);
    }

    /**
     * The other half of the same rule: when the client does record the generation, nothing is
     * implied and the figures stay exactly as recorded — no quantity is counted twice.
     */
    @Test
    void recordedGenerationIsNotDoubled() {
        Anexa1Sheet paper = sheets().stream()
                .filter(s -> s.wasteCode().equals("20 01 01"))
                .findFirst().orElseThrow();

        // The demo tenant records both sides in February: 100 generated, 60 recovered.
        Anexa1Sheet.Anexa1MonthRow february = paper.rows().get(1);
        assertThat(february.generated()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("100.000"));
    }

    @Test
    void theFormIsAPdfWithOnePagePerSheet() throws Exception {
        byte[] pdf = mockMvc.perform(get("/api/v1/evidences/anexa1?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        // Read the page tree rather than grepping the bytes: OpenPDF compresses it.
        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(sheets().size());
        reader.close();
    }

    /**
     * The document names itself, above the identification block: "Evidenţa gestiunii deşeurilor
     * generate {an}". Verbatim from the filled workbooks — six of them carry it in exactly that
     * form (Cluj and Timişoara, 2022–2024) and the blank template has it with the year as "20..".
     *
     * <p>The year in the title is the point: it is a yearly document, twelve rows and a TOTAL AN,
     * never a monthly one.
     */
    @Test
    void theSheetIsTitledAfterTheDocumentAndCarriesTheYear() throws Exception {
        byte[] pdf = mockMvc.perform(get("/api/v1/evidences/anexa1?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdf);
        String firstPage = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();

        assertThat(firstPage).contains("Evidenţa gestiunii deşeurilor generate " + YEAR);
    }

    /**
     * A "comerciant" keeps the sheet like anyone else.
     *
     * <p>The intake form asks what the business is on the market — producător, importator,
     * comerciant — and the answer decides the packaging declaration (Ordinul 794/2012, anexa 1),
     * a different document that only shares the name. The fişa de gestiune of HG 856/2002 is owed
     * by whoever generates waste, art. 1 alin. (1), whatever they sell. This test exists so that
     * nobody later "helpfully" hides the sheet for a trader.
     */
    @Test
    void aTraderStillKeepsTheSheet() {
        Company company = admin.getCompany();
        company.setMarketRoles(new java.util.LinkedHashSet<>(List.of(MarketRole.TRADER)));
        companyRepository.saveAndFlush(company);

        assertThat(MarketRole.putsPackagingOnMarket(company.getMarketRoles())).isFalse();
        assertThat(sheets()).isNotEmpty();
    }

    private List<Anexa1Sheet> sheets() {
        TenantContext.set(admin.getCompany().getId());
        try {
            return evidenceCalculator.anexa1(YEAR, null);
        } finally {
            TenantContext.clear();
        }
    }
}
