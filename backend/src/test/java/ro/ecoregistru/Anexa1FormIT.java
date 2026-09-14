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
     * The two "Cant." columns of chapter 2 answer two different questions, and only the first is
     * about the month's quantity.
     *
     * <p>"Stocare: Cant." is what the month produced. "Tratare: Cant." is only what this company
     * did itself, so a recovery performed by a partner leaves it at 0 — answer U of 24.08.2026,
     * and confirmed again on 25.08 when a figure appeared there and she asked why.
     */
    @Test
    void chapterTwoSeparatesWhatWeStoredFromWhatWeTreated() {
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
        // The collector did the recovering, so nothing was treated on our site.
        assertThat(february.treatedQuantity()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(BigDecimal.ZERO);
        assertThat(february.recoveries()).singleElement()
                .satisfies(h -> assertThat(h.operator()).isNotBlank());
    }

    /**
     * And "Modul" keeps quiet along with the quantity — audit point 14, fixed 04.09.2026.
     *
     * <p>The quantity was already computed from own treatment alone, but "Modul" and "Scopul" were
     * read from <em>every</em> movement of the month. So a handover on which the client had also
     * ticked a treatment method printed <em>Modul: TM</em> next to <em>Cant.: 0.000</em>: a
     * treatment declared with no quantity, a rubric contradicting itself on a filed form.
     *
     * <p>The corpus decides the shape of the rubric (regula de lucru 3). Panemar — a bakery that
     * only hands waste over — writes {@code 0.000} with Modul {@code -}; Hamburger, which really
     * does bale, writes both. Both silent is their practice for a pure handover.
     */
    @Test
    void chapterTwoLeavesTheTreatmentModeBlankWhenNothingWasTreatedHere() throws Exception {
        Company company = admin.getCompany();
        UUID workPointId = workPointRepository.findAllByCompany_Id(company.getId()).get(0).getId();
        UUID codeId = wasteCodeRepository.findByCode("15 01 03").orElseThrow().getId();
        UUID partnerId = partnerRepository.findAllByCompany_Id(company.getId()).get(0).getId();

        // A plain handover to a partner, with a treatment method filled in on it anyway.
        String body = """
                {
                  "workPointId": "%s", "date": "%d-09-10", "wasteCodeId": "%s",
                  "unit": "KG", "quantity": 100,
                  "operation": "RECOVERED", "register": "ANEXA_1", "operationCode": "R3",
                  "partnerId": "%s", "treatmentMethod": "TM"
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
        Anexa1Sheet.Anexa1MonthRow september = sheet.rows().get(8);

        assertThat(september.treatedQuantity()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(BigDecimal.ZERO);
        // The point of the fix: the mode must not survive alone.
        assertThat(september.treatmentMethod()).isBlank();
        assertThat(september.purpose()).isBlank();
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
                  "operation": "RECOVERED", "register": "ANEXA_1", "operationCode": "R3", "partnerId": "%s"
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
     * The per-code sheet carries <b>no</b> document title: it opens at "Agentul economic:", exactly
     * as every model does.
     *
     * <p>Checked against the whole corpus on 02.09.2026 — 33 per-code sheets, two companies
     * (Hamburger Recycling Romania and Panemar Jr.), 2022 through 2025. Not one of them titles the
     * page. The title "Evidenţa gestiunii deşeurilor generate {an}" belongs to the summary sheet,
     * and {@code AnnualDeclarationIT} holds it there.
     *
     * <p>This assertion used to be the opposite. We printed the title on every page so that a
     * twenty-page PDF would not reach an inspector unnamed — but the control dossier already names
     * the file and describes it in its README, so the reasoning bought nothing while departing from
     * every model we have. The test is kept inverted rather than deleted: it is the same rule,
     * pointing the right way.
     */
    @Test
    void thePerCodeSheetCarriesNoDocumentTitle() throws Exception {
        byte[] pdf = mockMvc.perform(get("/api/v1/evidences/anexa1?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdf);
        String firstPage = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();

        assertThat(firstPage).doesNotContain("Evidenţa gestiunii deşeurilor generate");
        assertThat(firstPage).contains("Agentul economic:");
        assertThat(firstPage).contains(String.valueOf(YEAR));
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

    /**
     * Chapters 3 and 4 cite the annexes of the act <b>in force</b>, and they are not the same
     * number.
     *
     * <p>The filled models head these columns "conform Anexei 3 / Anexei 2 din Legea 211/2011".
     * That act was repealed by OUG 92/2021, whose <b>anexa nr. 3</b> is still the recovery list but
     * whose disposal list moved to <b>anexa nr. 7</b>. We printed "anexa nr. 2" until the
     * conformance audit of 02.09.2026 — and anexa nr. 2 of OUG 92/2021 is "EXEMPLE de instrumente
     * economice", which has nothing to do with eliminating waste.
     *
     * <p>The act says it twice, so there was never a question for the specialist here: the annex
     * titles themselves (p. 58 and p. 69 of the Monitorul Oficial text), and anexa nr. 1 pct. 17
     * ("Anexa nr. 7 stabileşte o listă a operaţiunilor de eliminare") and pct. 37 (anexa nr. 3, for
     * recovery). See docs/surse-oficiale.md §2.3.
     */
    @Test
    void chaptersThreeAndFourCiteTheAnnexesInForce() throws Exception {
        // Whitespace-normalised: the column is narrow and 6pt, so the heading wraps and the text
        // extractor puts a newline wherever the line broke. Asserting on the raw extraction would
        // make this test fail the next time a column width is nudged, which is not the rule it is
        // here to protect.
        String text = flatten(allPagesText());

        assertThat(text).contains("conform anexei nr. 3 din OUG 92/2021");
        assertThat(text).contains("conform anexei nr. 7 din OUG 92/2021");
        // The two wrong references this form has carried or could carry: anexa nr. 2 of OUG
        // 92/2021 is the list of economic instruments, and Legea 211/2011 is repealed.
        assertThat(text).doesNotContain("anexei nr. 2 din OUG 92/2021");
        assertThat(text).doesNotContain("211/2011");
    }

    /** Collapses every run of whitespace to a single space, so line breaks stop mattering. */
    private static String flatten(String text) {
        return text.replaceAll("\\s+", " ");
    }

    /**
     * A hazardous code prints with the asterisk the codification the header points at gives it.
     *
     * <p>HG 856/2002 art. 4 alin. (3): "Deşeurile periculoase prevăzute în anexa nr. 2 sunt marcate
     * cu un asterisc (*)". The rubric above reads "Tipul de deşeu … cod … (conform codificării din
     * anexa nr. 2)", so on this form the star is part of how the code is spelled. We stripped it
     * everywhere until 02.09.2026 — 408 of the 842 codes are affected — and the corpus could not
     * catch it, because not one of its 33 sheets reports a hazardous code.
     *
     * <p>The second assertion is the one that matters most: the star goes on hazardous codes and
     * <b>only</b> on them.
     */
    @Test
    void aHazardousCodePrintsItsAsteriskAndAPlainOneDoesNot() throws Exception {
        Company company = admin.getCompany();
        UUID workPointId = workPointRepository.findAllByCompany_Id(company.getId()).get(0).getId();
        UUID codeId = wasteCodeRepository.findByCode("13 02 08").orElseThrow().getId();
        UUID partnerId = partnerRepository.findAllByCompany_Id(company.getId()).get(0).getId();

        String body = """
                {
                  "workPointId": "%s", "date": "%d-09-02", "wasteCodeId": "%s",
                  "unit": "KG", "quantity": 40,
                  "operation": "RECOVERED", "register": "ANEXA_1", "operationCode": "R3", "partnerId": "%s"
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

        // The sheet carries the flag; the generator is what spells the code out.
        Anexa1Sheet hazardous = sheets().stream()
                .filter(s -> s.wasteCode().equals("13 02 08"))
                .findFirst()
                .orElseThrow();
        assertThat(hazardous.hazardous()).isTrue();
        assertThat(hazardous.wasteCode()).isEqualTo("13 02 08");

        String pages = flatten(allPagesText());
        assertThat(pages).contains("13 02 08*");
        // 20 01 01 (paper and cardboard) is not hazardous and must stay bare.
        assertThat(pages).contains("20 01 01");
        assertThat(pages).doesNotContain("20 01 01*");
    }

    // --- QA-TRACE G1, G2, G5: the closed lists the sheet prints and the codes cap. 3–4 cite ---

    /**
     * The five notes of cap. 2, as the sheet prints them — HG 856/2002 anexa nr. 1, notele 1–5,
     * transcribed in {@code docs/surse-oficiale.md} §1.1 and checked by hand on 02.09.2026. Until
     * now nothing held them: a value dropped from the generator's legend or from an enum would have
     * gone out on every sheet filed.
     *
     * <p>⚠️ Nota 5 prints "haldă"; the transcription in surse-oficiale.md reads "halda", and the text
     * on legislatie.just.ro drops diacritics throughout the note ("tratare chimica"), so it cannot
     * settle it. Pinned as printed today; the question is open, not answered by this test.
     */
    private static final String[] CHAPTER_TWO_NOTES = {
            "1) Tipul de stocare: RM - recipient metalic; RP - recipient de plastic; BZ - bazin decantor; "
                    + "CT - container transportabil; CF - container fix; S - saci; PD - platformă de deshidratare; "
                    + "VN - în vrac, neacoperit; VA - în vrac, incintă acoperită; RL - recipient din lemn; A - altele.",
            "2) Modul de tratare: TM - tratare mecanică; TC - tratare chimică; TMC - tratare mecano-chimică; "
                    + "TB - tratare biochimică; D - deshidratare; TT - tratare termică; A - altele.",
            "3) Scopul tratării: V - pentru valorificare; E - în vederea eliminării.",
            "4) Mijlocul de transport: AS - autospeciale; AN - auto nespecial; H - transport hidraulic; "
                    + "CF - cale ferată; A - altele.",
            "5) Destinaţia: DO - depozitul de gunoi al oraşului/comunei; HP - haldă proprie; "
                    + "HC - haldă industrială comună; I - incinerarea în scopul eliminării; "
                    + "Vr - valorificare prin agenţi economici autorizaţi; "
                    + "P - utilizare materială sau energetică în propria întreprindere; "
                    + "Ve - valorificare energetică prin agenţi economici autorizaţi; A - altele.",
    };

    /** G1 (B6): every note, whole and in order, on the printed sheet. */
    @Test
    void theFiveNotesOfChapterTwoArePrintedWholeAndInOrder() throws Exception {
        String pages = Golden.flat(allPagesText());
        for (String note : CHAPTER_TWO_NOTES) {
            assertThat(pages).contains(Golden.flat(note));
        }
    }

    /**
     * G1 (B6), the other half: the values the screen offers are the letters the notes print, each
     * with the note's own wording. Nota 3 is the one deliberate gap — {@code TreatmentPurpose} keeps
     * only V, see its javadoc — so there the enum must be inside the note, not equal to it.
     */
    @Test
    void everyChapterTwoNomenclatorIsTheListItsNotePrints() {
        assertNomenclator(CHAPTER_TWO_NOTES[0], ro.ecoregistru.enums.StorageType.values(),
                ro.ecoregistru.enums.StorageType::getOfficialLabel, true);
        assertNomenclator(CHAPTER_TWO_NOTES[1], ro.ecoregistru.enums.TreatmentMethod.values(),
                ro.ecoregistru.enums.TreatmentMethod::getOfficialLabel, true);
        assertNomenclator(CHAPTER_TWO_NOTES[2], ro.ecoregistru.enums.TreatmentPurpose.values(),
                ro.ecoregistru.enums.TreatmentPurpose::getOfficialLabel, false);
        assertNomenclator(CHAPTER_TWO_NOTES[3], ro.ecoregistru.enums.TransportMeans.values(),
                ro.ecoregistru.enums.TransportMeans::getOfficialLabel, true);
        assertNomenclator(CHAPTER_TWO_NOTES[4], ro.ecoregistru.enums.WasteDestination.values(),
                ro.ecoregistru.enums.WasteDestination::getOfficialLabel, true);
    }

    private static <E extends Enum<E>> void assertNomenclator(String note, E[] values,
            java.util.function.Function<E, String> label, boolean exact) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:: |; )([A-Za-z]+) - ").matcher(note);
        List<String> printed = new java.util.ArrayList<>();
        while (m.find()) {
            printed.add(m.group(1));
        }
        List<String> offered = java.util.Arrays.stream(values).map(Enum::name).toList();
        if (exact) {
            assertThat(offered).as(note).containsExactlyInAnyOrderElementsOf(printed);
        } else {
            assertThat(printed).as(note).containsAll(offered);
        }
        for (E value : values) {
            assertThat(note).contains(value.name() + " - " + label.apply(value).toLowerCase(java.util.Locale.ROOT));
        }
    }

    /**
     * G5 (point 12): nota 2 in the act's order, D before TT — the one deviation the conformity audit
     * found in the notes, repaired on 02.09.2026. Written both ways, so a legend slipping back to the
     * old order fails here by name and not only as "the note is not on the page".
     */
    @Test
    void noteTwoKeepsDehydrationBeforeThermalTreatmentAsTheActDoes() throws Exception {
        String pages = Golden.flat(allPagesText());
        assertThat(pages).contains(Golden.flat("TB - tratare biochimică; D - deshidratare; TT - tratare termică; A - altele."));
        assertThat(pages).doesNotContain(Golden.flat("TT - tratare termică; D - deshidratare"));
    }

    /**
     * G2 (B7): cap. 3 and cap. 4 cite annexes 3 and 7 of OUG 92/2021 — R1–R13 and D1–D15, 28 codes.
     * Many tests use a few of them; none used all, so a code gone from the list could no longer be
     * chosen and nothing would fail. The family decides the chapter and the "Scopul" letter.
     */
    @Test
    void theOperationCodesAreTheThirteenRecoveriesAndFifteenDisposalsOfTheAnnexes() {
        List<String> expected = new java.util.ArrayList<>();
        for (int i = 1; i <= 13; i++) {
            expected.add("R" + i);
        }
        for (int i = 1; i <= 15; i++) {
            expected.add("D" + i);
        }
        var codes = ro.ecoregistru.enums.WasteOperationCode.values();
        assertThat(java.util.Arrays.stream(codes).map(Enum::name).toList()).containsExactlyElementsOf(expected);
        assertThat(codes).allSatisfy(c -> {
            boolean recovery = c.name().startsWith("R");
            assertThat(c.isRecovery()).as(c.name()).isEqualTo(recovery);
            assertThat(c.isDisposal()).as(c.name()).isEqualTo(!recovery);
            assertThat(c.treatmentPurpose()).as(c.name())
                    .isEqualTo(recovery ? ro.ecoregistru.enums.TreatmentPurpose.V : null);
        });
    }

    /** Every page joined, for assertions that do not care which sheet a code landed on. */
    private String allPagesText() throws Exception {
        byte[] pdf = renderPdf();
        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdf);
        try {
            StringBuilder all = new StringBuilder();
            var extractor = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                all.append(extractor.getTextFromPage(page)).append('\n');
            }
            return all.toString();
        } finally {
            reader.close();
        }
    }

    private byte[] renderPdf() throws Exception {
        return mockMvc.perform(get("/api/v1/evidences/anexa1?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
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
