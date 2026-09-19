package ro.ecoregistru;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA de lansare, generator — lotul 2: G04, G05, G06, G11, G12, G15
 * (`ecoregistru-docs/qa/LAUNCH-GAP-MAP.md`).
 *
 * <p>G04, G05, G06 și G12 sunt [DECIZIE] încă nedecise: testele {@code characterizes_…} fixează ce
 * face aplicația azi, ca o schimbare de regulă să se vadă aici, nu la un client. Nu spun că e bine.
 * G11 și G15 sunt reguli decise și se probează ca atare.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class GeneratorRulesAndDecisionsIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired ReportingDeadlineRepository deadlineRepository;

    private Company company;
    private String token;
    private UUID workPoint;
    private UUID collector;
    private WasteCode paper;
    private WasteCode glass;
    private WasteCode oil;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        company = companyRepository.save(Company.builder()
                .name("Reguli SRL").cui("ROR" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("reguli+" + suffix + "@demo.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        token = jwtService.generateToken(admin);
        workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Reguli").active(true).createdAt(Instant.now()).build()).getId();
        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector Reguli SRL").cui("RO8" + suffix)
                .authorizationNumber("AM 2/2025").type(PartnerType.COLLECTOR).supplier(true)
                .active(true).createdAt(Instant.now()).build()).getId();
        paper = wasteCodeRepository.findByCode("20 01 01").orElseThrow();
        glass = wasteCodeRepository.findByCode("20 01 02").orElseThrow();
        oil = wasteCodeRepository.findByCode("13 02 08").orElseThrow();
        assertThat(oil.isHazardous()).as("premisa: 13 02 08 e periculos în nomenclator").isTrue();
    }

    // ---------- G04 [DECIZIE]: anul declarat se poate schimba ----------

    /**
     * // DECIZIE: G04. Anul 2025 e declarat: termenul SIM din 15.03.2026 e bifat. Apoi o mișcare din
     * 2025 se corectează și alta se șterge. Azi amândouă trec (200), iar declarația retipărită pe
     * 2025 arată cifrele noi. Singurul semn e avertismentul din formular
     * ({@code frontend/src/lib/deadlines.ts:declarationOf}); serverul nu știe de anul declarat.
     */
    @Test
    void characterizes_aDeclaredYearCanStillBeEditedAndItsDeclarationChanges() throws Exception {
        String corrected = handover(paper, "2025-05-10", "1000", "KG");
        String removed = handover(paper, "2025-06-10", "200", "KG");
        assertThat(declarationText(2025)).contains("1200.000");
        deadlineRepository.save(ReportingDeadline.builder()
                .company(company).reportType(ReportType.SIM_ANNUAL)
                .dueDate(LocalDate.of(2026, 3, 15)).status(DeadlineStatus.DONE)
                .completedAt(Instant.parse("2026-03-10T09:00:00Z")).createdAt(Instant.now()).build());

        mockMvc.perform(put("/api/v1/movements/" + corrected).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handoverJson(paper, "2025-05-10", "400", "KG")))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/movements/" + removed).header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful());

        String after = declarationText(2025);
        assertThat(after).contains("400.000").doesNotContain("1200.000");
    }

    // ---------- G05 [DECIZIE]: date în viitor ----------

    /** // DECIZIE: G05. O predare datată peste un an de azi (ora României) se acceptă. */
    @Test
    void characterizes_aMovementDatedAYearAheadIsAccepted() throws Exception {
        LocalDate nextYear = LocalDate.now(ZoneId.of("Europe/Bucharest")).plusYears(1);
        post(handoverJson(paper, nextYear.toString(), "5", "KG")).andExpect(status().isOk());
    }

    // ---------- G06 [DECIZIE]: cum se tipărește 1,5 t ----------

    /**
     * // DECIZIE: G06. O singură predare de 1,5 t, patru documente, trei forme ale aceluiași număr:
     * <ul>
     *   <li>Anexa 1 și declarația anuală: {@code 1500.000} — kg, punct zecimal (Locale.ROOT);</li>
     *   <li>avizul: {@code 1,500} — t, virgulă zecimală (ro-RO), în unitatea mișcării;</li>
     *   <li>Anexa 3: {@code 1.5} — t, punct zecimal, fără zerouri.</li>
     * </ul>
     * Citit românește, „1500.000” poate însemna un milion și jumătate. Ecranul arată „1.500” (kg,
     * punct la mii) — partea de ecran e pentru Faza 4, în Chrome.
     */
    @Test
    void characterizes_oneAndAHalfTonnesPrintsInThreeDifferentForms() throws Exception {
        String id = handover(paper, "2025-05-10", "1.5", "TONS");

        assertThat(anexa1Text(2025)).contains("1500.000");
        assertThat(declarationText(2025)).contains("1500.000");
        assertThat(pdfText("/api/v1/movements/" + id + "/aviz")).contains("1,500");
        assertThat(pdfText("/api/v1/movements/" + id + "/anexa3")).contains("1.5");
    }

    // ---------- G11: marcajul de periculozitate ----------

    /**
     * La generator, Anexa 3 refuză deșeul periculos și Anexa 2 nu se folosește (decizia din
     * 17.09), deci <b>avizul e singurul document de transport</b> tipărit pentru un cod periculos.
     * Anexa 1 și declarația tipăresc {@code 13 02 08*} (HG 856/2002 art. 4 alin. (3), prin
     * {@code WasteCodeLabel.official}); avizul tipărește {@code 13 02 08}, fără asterisc
     * ({@code AvizGenerator.java:146} ia codul brut). Așteptat: același cod, aceeași formă.
     */
    @Test
    void theAvizMarksAHazardousCodeLikeTheOtherDocuments() throws Exception {
        String id = handoverOf(oil, "R9");

        assertThat(anexa1Text(2025)).contains("13 02 08*");
        assertThat(pdfText("/api/v1/movements/" + id + "/aviz")).contains("13 02 08*");
    }

    /** Controlul: un cod obișnuit nu primește asterisc pe aviz. */
    @Test
    void theAvizPrintsAPlainCodeWithoutAnAsterisk() throws Exception {
        String id = handover(paper, "2025-05-10", "5", "KG");
        assertThat(pdfText("/api/v1/movements/" + id + "/aviz")).contains("20 01 01").doesNotContain("20 01 01*");
    }

    // ---------- G12 [DECIZIE]: codurile de deșeu din profil ----------

    /**
     * // DECIZIE: G12. Profilul spune „lucrăm doar cu 20 01 01”; o predare pe 20 01 02 se acceptă.
     * Pentru codurile R/D, aceeași situație dă 400 ({@code movement.operation.code.not.in.profile}).
     */
    @Test
    void characterizes_aWasteCodeOutsideTheProfileIsAccepted() throws Exception {
        company.setAuthorizedWasteCodes(new LinkedHashSet<>(Set.of(paper)));
        companyRepository.save(company);

        post(handoverJson(glass, "2025-05-10", "5", "KG")).andExpect(status().isOk());
    }

    // ---------- G15: codurile R/D ----------

    /**
     * Profil gol = formular necompletat, nu „nimic permis”: orice R la valorificare și orice D la
     * eliminare trec. Rămâne regula familiei.
     */
    @Test
    void anEmptyProfileAllowsAnyRecoveryOrDisposalCode() throws Exception {
        assertThat(company.getAuthorizedOperationCodes()).isEmpty();
        post(handoverJson(paper, "2025-05-10", "5", "KG").replace("\"R13\"", "\"R1\""))
                .andExpect(status().isOk());
        post(disposalJson("D1")).andExpect(status().isOk());
    }

    /** Oglinda lui {@code WasteMovementValidationIT#recoveredWithDisposalCodeIsRejected}. */
    @Test
    void aDisposalWithARecoveryCodeIsRejected() throws Exception {
        post(disposalJson("R3")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.operation.code.disposal")));
    }

    /** Profil completat cu R13: un D5 la eliminare e în afara lui. */
    @Test
    void aFilledProfileRestrictsDisposalCodesToo() throws Exception {
        company.setAuthorizedOperationCodes(new LinkedHashSet<>(Set.of(WasteOperationCode.R13)));
        companyRepository.save(company);

        post(disposalJson("D5")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.operation.code.not.in.profile")));
    }

    // --- helpers ---

    private String handover(WasteCode code, String date, String qty, String unit) throws Exception {
        String json = post(handoverJson(code, date, qty, unit)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String handoverOf(WasteCode code, String operationCode) throws Exception {
        String json = post(handoverJson(code, "2025-05-10", "5", "KG").replace("\"R13\"", "\"" + operationCode + "\""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private ResultActions post(String body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/movements").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String handoverJson(WasteCode code, String date, String qty, String unit) {
        return """
                {"workPointId": "%s", "date": "%s", "wasteCodeId": "%s", "quantity": %s,
                 "unit": "%s", "physicalState": "SOLID", "operation": "RECOVERED", "register": "ANEXA_1",
                 "wasteDestination": "Vr", "operationCode": "R13", "partnerId": "%s"}
                """.formatted(workPoint, date, code.getId(), qty, unit, collector);
    }

    private String disposalJson(String operationCode) {
        return """
                {"workPointId": "%s", "date": "2025-05-10", "wasteCodeId": "%s", "quantity": 5,
                 "unit": "KG", "physicalState": "SOLID", "operation": "DISPOSED", "register": "ANEXA_1",
                 "wasteDestination": "DO", "operationCode": "%s", "partnerId": "%s"}
                """.formatted(workPoint, paper.getId(), operationCode, collector);
    }

    private String anexa1Text(int year) throws Exception {
        return pdfText("/api/v1/evidences/anexa1?year=" + year);
    }

    private String declarationText(int year) throws Exception {
        return pdfText("/api/v1/evidences/declaratie-anuala?year=" + year);
    }

    private String pdfText(String url) throws Exception {
        byte[] pdf = mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(new PdfTextExtractor(reader).getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }
}
