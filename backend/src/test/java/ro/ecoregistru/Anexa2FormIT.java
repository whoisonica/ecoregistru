package ro.ecoregistru;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P3.2 — Anexa 2 la HG 1061/2008, the consignment form for <em>hazardous</em> waste transport.
 *
 * <p>What is pinned down here is what the <b>act</b> says, because unlike every other document in
 * this application the shape of this one was read from the act itself and not from a filled model
 * (the one copy we hold is not conformant). Each test names its article:
 *
 * <ul>
 *   <li><b>art. 8</b> — the expeditor writes it, so our client prints it;</li>
 *   <li><b>art. 9 alin. (1), art. 10 alin. (1)</b> — the carrier's and the recipient's quantities
 *       and the receipt date are declared when they take the waste over, so they print
 *       <em>empty</em>;</li>
 *   <li><b>art. 15 alin. (2)</b> vs <b>art. 12 + art. 4 alin. (10)</b> — three copies below the
 *       threshold, six above it;</li>
 *   <li><b>nota *1)</b> of the model — the number belongs to the county agency, so nothing here
 *       allocates one;</li>
 *   <li><b>art. 24</b> — hazardous waste from medical activity is a different flow, drawn up by
 *       the carrier, so this form is refused for it rather than printed wrongly.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class Anexa2FormIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;

    private String token;
    private UUID workPointId;
    private UUID hazardousCodeId;
    private UUID partnerId;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        UUID tenantId = admin.getCompany().getId();
        workPointId = workPointRepository.findAllByCompany_Id(tenantId).get(0).getId();
        // Ulei de motor uzat: the service-garage case that made this the biggest hole left in the
        // generator module.
        hazardousCodeId = wasteCodeRepository.findByCode("13 02 08").orElseThrow().getId();
        partnerId = partnerRepository.findAllByCompany_Id(tenantId).get(0).getId();
    }

    // ---------- What prints, and what deliberately does not ----------

    /**
     * Art. 8: "Expeditorul completează, semnează şi ştampilează formularul". The first reading of
     * the filled copy concluded the opposite — that the collector issues it — and would have
     * turned this slice into a note on a screen instead of a document.
     */
    @Test
    void aHazardousHandoverPrintsTheConsignmentForm() throws Exception {
        UUID id = createMovement("""
                  "operation": "DISPOSED", "register": "ANEXA_1", "operationCode": "D5",
                  "partnerId": "%s", "quantity": 200, "transportDestinations": ["ELIMINARE"],
                  "driverName": "Musat Liviu", "vehicleRegistration": "B69BMA",
                  "transportMeans": "AN", "anexa2Packaging": "2 butoaie metalice 200 l"
                """.formatted(partnerId), hazardousCodeId);

        byte[] pdf = mockMvc.perform(get("/api/v1/movements/" + id + "/anexa2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        String text = firstPage(pdf);
        // ASCII fragments only: PdfTextExtractor decodes the Cp1250 page back through Latin-1, so
        // the diacritics come out mangled here even though the printed page is right.
        assertThat(text).contains("transport de");
        // The asterisk is part of how a hazardous code is spelled — HG 856/2002 art. 4 alin. (3).
        assertThat(text).contains("13 02 08*");
        // In tonnes, with the Romanian decimal separator: 200 kg is 0,2 t.
        assertThat(text).contains("0,2");
        assertThat(text).contains("B69BMA");
        assertThat(text).contains("butoaie");
        // The mmeans of transport comes from the answer Anexa 1 cap. 2 already asks for.
        assertThat(text).contains("Auto nespecial");
    }

    /**
     * Art. 9 alin. (1) and art. 10 alin. (1): the carrier and the recipient sign <em>at the moment
     * they take the waste over</em>, so the three quantities and the receipt date that belong to
     * them are not ours to fill in. The filled copy we hold has all of them completed — it was
     * written up after the transport — and copying it would have meant printing somebody else's
     * declaration for them.
     */
    @Test
    void theQuantitiesTheOtherPartiesDeclarePrintEmpty() throws Exception {
        UUID id = createMovement("""
                  "operation": "DISPOSED", "register": "ANEXA_1", "operationCode": "D5",
                  "partnerId": "%s", "quantity": 200
                """.formatted(partnerId), hazardousCodeId);

        String text = firstPage(pdfOf(id));

        // The rubrics are on the form...
        assertThat(text).contains("Cantitatea primit");
        assertThat(text).contains("Cantitatea respins");
        assertThat(text).contains("Data primirii");
        // ...and the figure we do know appears exactly once: under "Cantitatea predată".
        assertThat(text.split("0,2", -1).length - 1).isEqualTo(1);
    }

    /**
     * Nota *1) of the model: "Număr înscris de către agenţia judeţeană pentru protecţia mediului."
     * The opposite of {@code anexa3Number}, which we allocate max+1 per company — printing an
     * invented number onto an official form is exactly what this test exists to prevent.
     */
    @Test
    void theFormNumberIsNeverAllocatedAndIsPrintedAsTyped() throws Exception {
        UUID id = createMovement("""
                  "operation": "DISPOSED", "register": "ANEXA_1", "operationCode": "D5",
                  "partnerId": "%s", "quantity": 200
                """.formatted(partnerId), hazardousCodeId);

        pdfOf(id);
        assertThat(movementRepository.findById(id).orElseThrow().getAnexa2Number()).isNull();

        mockMvc.perform(get("/api/v1/movements/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anexa2Number", is(nullValue())));

        UUID typed = createMovement("""
                  "operation": "DISPOSED", "register": "ANEXA_1", "operationCode": "D5",
                  "partnerId": "%s", "quantity": 200, "anexa2Number": "1047"
                """.formatted(partnerId), hazardousCodeId);
        assertThat(firstPage(pdfOf(typed))).contains("1047");
    }

    // ---------- The threshold of 1 t/an ----------

    /**
     * Art. 15 alin. (2) — three copies below the threshold: expeditor, destinatar, transportator.
     */
    @Test
    void belowOneTonneTheFormPrintsThreeCopies() throws Exception {
        // A code the demo seed does not touch, so the yearly total is this movement alone.
        UUID code = wasteCodeRepository.findByCode("20 01 21").orElseThrow().getId();
        UUID id = createMovement("""
                  "operation": "DISPOSED", "register": "ANEXA_1", "operationCode": "D5",
                  "partnerId": "%s", "quantity": 40
                """.formatted(partnerId), code);

        assertThat(pageCount(pdfOf(id))).isEqualTo(3);
    }

    /**
     * Art. 12 with art. 4 alin. (10) — six above it: the three parties, the agency that approved
     * the transport, the ISU of the expeditor's county, and that county's agency.
     *
     * <p>The total is read from the evidence engine, so an exit with no recorded generation still
     * counts: 1,5 t left the site, which means 1,5 t was generated (V24).
     */
    @Test
    void aboveOneTonneTheFormPrintsSixCopies() throws Exception {
        UUID code = wasteCodeRepository.findByCode("08 01 17").orElseThrow().getId();
        UUID id = createMovement("""
                  "operation": "DISPOSED", "register": "ANEXA_1", "operationCode": "D5",
                  "partnerId": "%s", "quantity": 1500
                """.formatted(partnerId), code);

        assertThat(pageCount(pdfOf(id))).isEqualTo(6);
    }

    /**
     * The proposal is shown with the figures it was made from, and it never becomes a decision:
     * "categorie" is undefined in the act (art. 2 sends it to a repealed one), so the per-code
     * total is only the narrowest reading of it.
     */
    @Test
    void theThresholdIsProposedWithTheFiguresBehindIt() throws Exception {
        // Its own code, like every threshold test here: the tests share one database, so a code
        // used by two of them would carry the other one's tonnes into this year's total.
        UUID code = wasteCodeRepository.findByCode("06 01 01").orElseThrow().getId();
        UUID id = createMovement("""
                  "operation": "DISPOSED", "register": "ANEXA_1", "operationCode": "D5",
                  "partnerId": "%s", "quantity": 840
                """.formatted(partnerId), code);

        mockMvc.perform(get("/api/v1/movements/" + id + "/anexa2/prag")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year", is(2026)))
                .andExpect(jsonPath("$.wasteCode", is("06 01 01*")))
                .andExpect(jsonPath("$.generatedTons", is(0.84)))
                .andExpect(jsonPath("$.groupCode", is("06 01")))
                .andExpect(jsonPath("$.belowOneTon", is(true)))
                // Nobody has answered yet, so the tick is the proposal and says so.
                .andExpect(jsonPath("$.chosen", is(nullValue())))
                .andExpect(jsonPath("$.effectiveBelowOneTon", is(true)));
    }

    /**
     * And the client's own answer wins over the proposal — including in the number of copies,
     * because the two are the same legal question. Someone reading "categorie" more widely than a
     * six-digit code has to be able to say so.
     */
    @Test
    void theClientsAnswerOverridesTheProposal() throws Exception {
        UUID code = wasteCodeRepository.findByCode("07 01 03").orElseThrow().getId();
        UUID id = createMovement("""
                  "operation": "DISPOSED", "register": "ANEXA_1", "operationCode": "D5",
                  "partnerId": "%s", "quantity": 40, "anexa2BelowOneTon": false,
                  "anexa2ApprovalNumber": "APM BH 118/2026"
                """.formatted(partnerId), code);

        mockMvc.perform(get("/api/v1/movements/" + id + "/anexa2/prag")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.belowOneTon", is(true)))
                .andExpect(jsonPath("$.chosen", is(false)))
                .andExpect(jsonPath("$.effectiveBelowOneTon", is(false)));

        byte[] pdf = pdfOf(id);
        assertThat(pageCount(pdf)).isEqualTo(6);
        assertThat(firstPage(pdf)).contains("APM BH 118/2026");
    }

    /**
     * One copy, one page — with <em>every</em> free-text rubric filled and a long code name, which
     * is the case that broke it. The first rendering of a fully completed form spilled its last
     * footnote line onto a second page, so three copies came out as six sheets: not wrong in law,
     * but a form that runs over is a form somebody staples in the wrong order.
     *
     * <p>Caught by looking at the rendered PDF, not by the tests that existed — those used short
     * movements, which fitted. Regula de lucru 5, again.
     */
    @Test
    void aFullyFilledFormIsStillOnePagePerCopy() throws Exception {
        UUID longNamed = wasteCodeRepository.findByCode("15 01 10").orElseThrow().getId();
        UUID id = createMovement("""
                  "operation": "DISPOSED", "register": "ANEXA_1", "operationCode": "D5",
                  "partnerId": "%s", "quantity": 120, "transportMeans": "AS",
                  "transportDestinations": ["COLECTARE", "STOCARE_TEMPORARA", "TRATARE",
                                            "VALORIFICARE", "ELIMINARE"],
                  "driverName": "Musat Liviu Constantin", "driverIdentification": "CJ 157812",
                  "vehicleRegistration": "CJ 09 ECO", "anexa2Number": "1047",
                  "anexa2ApprovalNumber": "APM CJ 118/2026 valabil 2 ani",
                  "anexa2Packaging": "4 butoaie metalice de 200 l si 2 IBC-uri de 1000 l, toate etichetate conform ADR",
                  "documentReference": "aviz de insotire a marfii nr. 1406 din 11.01.2026"
                """.formatted(partnerId), longNamed);

        assertThat(pageCount(pdfOf(id))).isEqualTo(3);
    }

    // ---------- The three refusals, each naming the right document ----------

    /** The title says "periculoase". A clean code belongs on Anexa 3, and the message says so. */
    @Test
    void aNonHazardousCodeIsSentToAnexa3() throws Exception {
        UUID clean = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
        UUID id = createMovement("""
                  "operation": "RECOVERED", "register": "ANEXA_1", "operationCode": "R3",
                  "partnerId": "%s", "quantity": 100
                """.formatted(partnerId), clean);

        mockMvc.perform(get("/api/v1/movements/" + id + "/anexa2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("anexa2.not.hazardous")));
    }

    /** And the mirror refusal now names a form that exists. */
    @Test
    void anexa3StillRefusesAHazardousCodeAndNamesThisForm() throws Exception {
        UUID id = createMovement("""
                  "operation": "DISPOSED", "register": "ANEXA_1", "operationCode": "D5",
                  "partnerId": "%s", "quantity": 200
                """.formatted(partnerId), hazardousCodeId);

        mockMvc.perform(get("/api/v1/movements/" + id + "/anexa3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("anexa3.hazardous")))
                .andExpect(jsonPath("$['error-message']",
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("nu e încă implementat"))))
                .andExpect(jsonPath("$['error-message']",
                        org.hamcrest.Matchers.containsString("anexa 2")));
    }

    /** The form records a consignment from an expeditor to a destinatar. */
    @Test
    void aMovementWithoutARecipientIsRefused() throws Exception {
        UUID id = createMovement("\"operation\": \"GENERATED\", \"quantity\": 200",
                hazardousCodeId);

        mockMvc.perform(get("/api/v1/movements/" + id + "/anexa2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("anexa2.requires.handover")));
    }

    /**
     * Art. 24: for hazardous waste from medical activity the <em>carrier</em> draws the forms up —
     * "chiar dacă acesta este şi destinatar" — on the cumulated quantity of one round through an
     * area, with a schedule of the individual expeditors attached. A clinic printing this form as
     * expeditor would be holding the wrong document, and clinics are a target client, so the
     * refusal says it on screen rather than leaving it to be discovered at an inspection.
     */
    @Test
    void medicalWasteIsRefusedBecauseArticle24GivesTheFormToTheCarrier() throws Exception {
        UUID medical = wasteCodeRepository.findByCode("18 01 03").orElseThrow().getId();
        UUID id = createMovement("""
                  "operation": "DISPOSED", "register": "ANEXA_1", "operationCode": "D5",
                  "partnerId": "%s", "quantity": 30
                """.formatted(partnerId), medical);

        mockMvc.perform(get("/api/v1/movements/" + id + "/anexa2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("anexa2.medical")));
    }

    // ---------- helpers ----------

    private byte[] pdfOf(UUID id) throws Exception {
        return mockMvc.perform(get("/api/v1/movements/" + id + "/anexa2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private String firstPage(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();
        return text;
    }

    private int pageCount(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        int pages = reader.getNumberOfPages();
        reader.close();
        return pages;
    }

    private UUID createMovement(String extraJson, UUID codeId) throws Exception {
        String response = mockMvc.perform(movementFor(extraJson, codeId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(response.split("\"id\":\"")[1].split("\"")[0]);
    }

    private MockHttpServletRequestBuilder movementFor(String extraJson, UUID codeId) {
        String body = """
                {
                  "workPointId": "%s",
                  "date": "2026-07-05",
                  "wasteCodeId": "%s",
                  "unit": "KG",
                  "physicalState": "LIQUID",
                  %s
                }
                """.formatted(workPointId, codeId, extraJson);
        return post("/api/v1/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
