package ro.ecoregistru;

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
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;

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
 * ETAPA G3 — Anexa 3 la HG 1061/2008, the transport form generated from a recorded movement.
 *
 * <p>The rules worth pinning down are the ones the filled model taught us:
 *
 * <ul>
 *   <li>a load the recipient will weigh is recorded <em>without</em> a quantity — the model's own
 *       quantity is handwritten after weighing — and neither zero nor an estimate stands in for
 *       it;</li>
 *   <li>such a movement makes its monthly evidence line provisional rather than silently counting
 *       as nothing;</li>
 *   <li>the form is for non-hazardous waste, which its title says, so a hazardous code is refused
 *       instead of printed on the wrong document;</li>
 *   <li>it describes a handover, so it needs a recipient;</li>
 *   <li>the number is allocated once and a reprint is the same document.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class Anexa3FormIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;

    private String token;
    private UUID tenantId;
    private UUID workPointId;
    private UUID wasteCodeId;
    private UUID partnerId;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        tenantId = admin.getCompany().getId();
        // Looked up directly rather than read off a seeded movement: the associations there are
        // lazy, and setUp runs outside a session.
        workPointId = workPointRepository.findAllByCompany_Id(tenantId).get(0).getId();
        wasteCodeId = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
        partnerId = partnerRepository.findAllByCompany_Id(tenantId).get(0).getId();
    }

    // ---------- A load nobody weighed yet ----------

    /**
     * The case Andreea described: a corner shop hands its cardboard to a collector and has no
     * scale. The movement is real, the quantity is not known, and the form leaves the cell blank.
     */
    @Test
    void aLoadWeighedByTheRecipientIsRecordedWithoutAQuantity() throws Exception {
        mockMvc.perform(movement("""
                          "operation": "RECOVERED", "operationCode": "R13",
                          "partnerId": "%s", "weighedAtUnloading": true, "volumeM3": 1.5
                        """.formatted(partnerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(nullValue())))
                .andExpect(jsonPath("$.weighedAtUnloading", is(true)))
                .andExpect(jsonPath("$.volumeM3", is(1.5)));
    }

    /** Without the flag, a movement with no quantity is a gap, not a decision. */
    @Test
    void aMissingQuantityIsRejectedUnlessTheRecipientWeighsIt() throws Exception {
        mockMvc.perform(movement("""
                          "operation": "RECOVERED", "operationCode": "R13", "partnerId": "%s"
                        """.formatted(partnerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.quantity.required")));
    }

    /** Somebody has to do the weighing, and it is the party taking the waste over. */
    @Test
    void weighingAtUnloadingNeedsARecipient() throws Exception {
        mockMvc.perform(movement("\"operation\": \"GENERATED\", \"weighedAtUnloading\": true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.weighing.recipient")));
    }

    /** The totals are provisional until the weight comes back, and the line says so. */
    @Test
    void anUnweighedExitMakesItsEvidenceLineProvisional() throws Exception {
        mockMvc.perform(movement("""
                          "operation": "RECOVERED", "operationCode": "R13",
                          "partnerId": "%s", "weighedAtUnloading": true
                        """.formatted(partnerId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/evidences/regenerate?year=2026")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/evidences?year=2026&month=7")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.awaitingWeighing == true)]").exists())
                .andExpect(jsonPath("$[?(@.awaitingWeighing == true && @.incomplete == true)]").exists());
    }

    // ---------- Which movements the form may be printed for ----------

    @Test
    void theFormIsRefusedForHazardousWaste() throws Exception {
        UUID hazardous = wasteCodeRepository.findByCode("13 02 08").orElseThrow().getId();
        UUID id = createMovement("""
                  "operation": "RECOVERED", "operationCode": "R13", "partnerId": "%s", "quantity": 5.0
                """.formatted(partnerId), hazardous);

        mockMvc.perform(get("/api/v1/movements/" + id + "/anexa3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("anexa3.hazardous")));
    }

    @Test
    void theFormIsRefusedWhenNothingWasHandedOver() throws Exception {
        UUID id = createMovement("\"operation\": \"GENERATED\", \"quantity\": 5.0", wasteCodeId);

        mockMvc.perform(get("/api/v1/movements/" + id + "/anexa3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("anexa3.requires.handover")));
    }

    // ---------- The document itself ----------

    @Test
    void theFormIsAPdfAndKeepsItsNumberOnAReprint() throws Exception {
        UUID id = createMovement("""
                  "operation": "RECOVERED", "operationCode": "R13", "partnerId": "%s",
                  "weighedAtUnloading": true, "volumeM3": 17,
                  "unloadDate": "2026-07-06",
                  "driverName": "Musat Liviu", "driverIdentification": "RK 157812",
                  "vehicleRegistration": "B69BMA",
                  "transportDestinations": ["COLECTARE", "VALORIFICARE"],
                  "documentReference": "aviz 1406/11.01"
                """.formatted(partnerId), wasteCodeId);

        byte[] pdf = mockMvc.perform(get("/api/v1/movements/" + id + "/anexa3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");

        Integer allocated = movementRepository.findById(id).orElseThrow().getAnexa3Number();
        assertThat(allocated).isNotNull();

        mockMvc.perform(get("/api/v1/movements/" + id + "/anexa3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(movementRepository.findById(id).orElseThrow().getAnexa3Number())
                .isEqualTo(allocated);
    }

    // ---------- helpers ----------

    private UUID createMovement(String extraJson, UUID codeId) throws Exception {
        String response = mockMvc.perform(movementFor(extraJson, codeId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(response.split("\"id\":\"")[1].split("\"")[0]);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder movement(
            String extraJson) {
        return movementFor(extraJson, wasteCodeId);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder movementFor(
            String extraJson, UUID codeId) {
        String body = """
                {
                  "workPointId": "%s",
                  "date": "2026-07-05",
                  "wasteCodeId": "%s",
                  "unit": "KG",
                  "physicalState": "SOLID",
                  %s
                }
                """.formatted(workPointId, codeId, extraJson);
        return post("/api/v1/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
