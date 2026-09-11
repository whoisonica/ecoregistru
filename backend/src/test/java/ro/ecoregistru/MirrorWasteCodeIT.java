package ro.ecoregistru;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
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
import ro.ecoregistru.entity.Attachment;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.AttachmentRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.time.Instant;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * G-4 — codurile-oglindă declarate nepericuloase fără document justificativ.
 *
 * <p>OUG 92/2021 art. 8 alin. (2) admite încadrarea ca <b>nepericulos</b> a unui deșeu care are
 * pereche periculoasă <i>„numai în baza unei analize a originii, testelor, buletinelor de analiză
 * şi a altor documente relevante"</i>. E singurul loc din modul unde aplicația se uită la o
 * <b>încadrare</b>, nu la o rubrică goală — și de aceea probele de aici sunt de două feluri:
 * jumătate pinuiesc <em>perechea</em> (derivată în {@code V37} din numele oficial al codului),
 * jumătate pinuiesc <em>avertismentul</em>.
 *
 * <p>⚠️ Proba care contează cel mai mult e {@link #phrasingVariantsAreMirrorsToo()}. Nota care a
 * cerut felia presupunea o singură formulare — „altele decât cele specificate la" — iar regula
 * scrisă pe ea ar fi trecut toate celelalte probe de aici și ar fi ratat 31 de coduri, printre care
 * obiectele ascuțite din cap. 18. O regulă îngustă nu se vede decât dacă e probată pe marginea ei.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class MirrorWasteCodeIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired AttachmentRepository attachmentRepository;

    private String token;
    private UUID workPointId;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        workPointId = workPointRepository.findAllByCompany_Id(admin.getCompany().getId()).get(0).getId();
    }

    // ---------- perechea, așa cum o scrie Decizia 2014/955/UE ----------

    /** Exemplul din felie: pământ și pietre, oglinda lui 17 05 03* (cu substanțe periculoase). */
    @Test
    void theNonHazardousHalfNamesItsHazardousPair() {
        assertThat(mirrorOf("17 05 04")).isEqualTo("17 05 03");
    }

    /** Un cod poate avea două perechi; ies în ordine, separate prin virgulă. */
    @Test
    void aCodeCanMirrorMoreThanOneHazardousCode() {
        assertThat(mirrorOf("01 03 06")).isEqualTo("01 03 04, 01 03 05");
    }

    /**
     * Actul scrie același lucru în patru feluri, iar oglinda nu e a frazei, e a codului citat.
     * Fiecare rând de aici ar fi lipsit dintr-o regulă scrisă pe „altele decât cele specificate la",
     * și primele două sunt exact locul unde o încadrare greșită costă cel mai mult: obiectele
     * ascuțite din deșeurile medicale.
     */
    @Test
    void phrasingVariantsAreMirrorsToo() {
        assertThat(mirrorOf("18 01 01")).isEqualTo("18 01 03");  // „(cu excepţia 18 01 03)"
        assertThat(mirrorOf("18 02 01")).isEqualTo("18 02 02");  // idem, medicina veterinară
        assertThat(mirrorOf("01 04 08")).isEqualTo("01 04 07");  // „altele decât cele menţionate la"
        assertThat(mirrorOf("10 09 12")).isEqualTo("10 09 11");  // „alte particule decât cele..."
        assertThat(mirrorOf("10 01 01")).isEqualTo("10 01 04");  // „exclusiv praful de cazan..."
    }

    /**
     * Filtrul „codul citat e periculos" își câștigă locul pe un singur rând din 842, și ăsta e:
     * 03 03 11 citează 03 03 10, care e <b>nepericulos</b>. Fără filtru, clientul ar fi primit un
     * avertisment pentru o încadrare pe care actul n-o condiționează de nimic.
     */
    @Test
    void aCodeThatCitesOnlyANonHazardousCodeIsNotAMirror() {
        assertThat(wasteCodeRepository.findByCode("03 03 10").orElseThrow().isHazardous()).isFalse();
        assertThat(mirrorOf("03 03 11")).isNull();
    }

    /**
     * Jumătatea periculoasă nu e oglindă, oricât de mult ar semăna numele. Art. 8 alin. (2)
     * condiționează încadrarea <em>ca nepericulos</em>; cine a declarat periculos n-are ce dovedi.
     */
    @Test
    void theHazardousHalfIsNeverAMirror() {
        assertThat(mirrorOf("17 05 03")).isNull();
        assertThat(mirrorOf("13 02 08")).isNull();
        // 16 01 21* poartă chiar fraza, și tot nu e oglindă — e periculos.
        assertThat(mirrorOf("16 01 21")).isNull();
    }

    /** Un cod obișnuit, care nu citează pe nimeni. */
    @Test
    void anOrdinaryCodeHasNoPair() {
        assertThat(mirrorOf("20 01 01")).isNull();
        assertThat(mirrorOf("20 03 01")).isNull();
    }

    /**
     * Amprenta întregii derivări. Cifra e aici ca o reîncărcare a nomenclatorului sau o regulă
     * rescrisă mai îngust să cadă în build, nu la un client: 161 de oglinzi din 842 de coduri,
     * numărate pe 11.09.2026.
     */
    @Test
    void theNomenclatorHasExactlyTheMirrorsItHadWhenTheRuleWasWritten() {
        assertThat(wasteCodeRepository.findAll().stream().filter(c -> c.getMirrorOf() != null).count())
                .isEqualTo(161);
    }

    // ---------- avertismentul ----------

    @Test
    void aMirrorCodeWithoutAnyDocumentIsFlagged() throws Exception {
        JsonNode movement = createMovement("17 05 04");
        assertThat(movement.get("mirrorClassificationUnproven").asBoolean()).isTrue();
        assertThat(movement.get("mirrorOf").asText()).isEqualTo("17 05 03");
    }

    /**
     * Un document atașat stinge avertismentul. Aplicația nu poate citi PDF-ul ca să spună dacă e
     * chiar un buletin de analiză, și nu se preface că poate: întrebarea pe care o pune e „unde e
     * hârtia?", nu „e hârtia potrivită?".
     */
    @Test
    void anAttachedDocumentAnswersTheQuestion() throws Exception {
        UUID id = UUID.fromString(createMovement("17 05 04").get("id").asText());
        attachmentRepository.save(Attachment.builder()
                .movement(movementRepository.findById(id).orElseThrow())
                .url("https://res.cloudinary.com/x/image/authenticated/s--sig--/v1/buletin.pdf")
                .publicId("ecoregistru/movements/buletin")
                .resourceType("image").deliveryType("authenticated").format("pdf")
                .fileName("buletin-analiza.pdf").contentType("application/pdf")
                .createdAt(Instant.now()).build());

        assertThat(read(id).get("mirrorClassificationUnproven").asBoolean()).isFalse();
    }

    /** Un cod fără pereche nu poartă nicio afirmație de dovedit. */
    @Test
    void anOrdinaryCodeIsNotFlagged() throws Exception {
        JsonNode movement = createMovement("20 01 01");
        assertThat(movement.get("mirrorClassificationUnproven").asBoolean()).isFalse();
        assertThat(movement.get("mirrorOf").isNull()).isTrue();
    }

    /** Nici deșeul declarat periculos — acolo nu s-a ieftinit nimic. */
    @Test
    void aHazardousCodeIsNotFlagged() throws Exception {
        JsonNode movement = createMovement("13 02 08");
        assertThat(movement.get("hazardous").asBoolean()).isTrue();
        assertThat(movement.get("mirrorClassificationUnproven").asBoolean()).isFalse();
    }

    // ---------- helpers ----------

    private String mirrorOf(String code) {
        WasteCode wasteCode = wasteCodeRepository.findByCode(code).orElseThrow();
        return wasteCode.getMirrorOf();
    }

    private JsonNode createMovement(String code) throws Exception {
        UUID wasteCodeId = wasteCodeRepository.findByCode(code).orElseThrow().getId();
        String body = """
                {
                  "workPointId": "%s",
                  "date": "2026-03-10",
                  "wasteCodeId": "%s",
                  "quantity": 5.000,
                  "unit": "KG",
                  "physicalState": "SOLID",
                  "operation": "GENERATED"
                }
                """.formatted(workPointId, wasteCodeId);
        String created = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(created);
    }

    private JsonNode read(UUID movementId) throws Exception {
        String rows = mockMvc.perform(get("/api/v1/movements")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode row : objectMapper.readTree(rows)) {
            if (row.get("id").asText().equals(movementId.toString())) {
                return row;
            }
        }
        throw new AssertionError("movement not in the list: " + movementId);
    }
}
