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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V28 — the carrier tick on a partner, and drivers that are kept instead of retyped.
 *
 * <p>The rules worth pinning down are the ones that were decisions rather than plumbing:
 *
 * <ul>
 *   <li>hauling is a <em>tick</em>, not a fourth {@code PartnerType}: the firm that both collects
 *       and hauls — the ordinary case the user described — must be one row, not two;</li>
 *   <li>a pure haulage firm has no type at all, since it does nothing with the waste, and that is
 *       the only case where the field may be null;</li>
 *   <li>a carrier's drivers are written through the partner form; the drivers endpoint is for ours
 *       and refuses his, because two write paths into the same rows would let a driver added
 *       through the endpoint disappear on the next partner save;</li>
 *   <li>unticking "carrier" does not delete the drivers already typed in.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class CarrierAndDriversIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired ObjectMapper objectMapper;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;

    private String token;
    private UUID workPointId;
    private UUID wasteCodeId;
    private UUID recipientId;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        UUID tenantId = admin.getCompany().getId();
        workPointId = workPointRepository.findAllByCompany_Id(tenantId).get(0).getId();
        wasteCodeId = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
        recipientId = partnerRepository.findAllByCompany_Id(tenantId).get(0).getId();
    }

    // ---------- The tick ----------

    /**
     * The case in the request: "uneori firma care colectează şi transportă". One row carrying both
     * facts — which is exactly what a fourth enum value could not have expressed.
     */
    @Test
    void aCollectorCanAlsoBeACarrier() throws Exception {
        mockMvc.perform(createPartner("""
                        "name": "Eco Colect SRL", "type": "COLLECTOR", "supplier": true, "carrier": true
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("COLLECTOR")))
                .andExpect(jsonPath("$.carrier", is(true)));
    }

    /**
     * "alteori nu, poate să transporte o firmă de transport mai mare": it does nothing with the
     * waste, so filing it as a collector would be a guessed value on a rubric the audit file prints
     * and the Anexa 3 "Destinat:" ticks are read from.
     */
    @Test
    void aPureHaulageFirmHasNoWasteType() throws Exception {
        mockMvc.perform(createPartner("""
                        "name": "Trans Greu SA", "supplier": true, "carrier": true
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is(nullValue())))
                .andExpect(jsonPath("$.carrier", is(true)));
    }

    /**
     * V28 relaxed the column, not the product: the rule moved into the service. Either they do
     * something with the waste, or they haul it.
     */
    @Test
    void aPartnerThatIsNeitherIsRefused() throws Exception {
        mockMvc.perform(createPartner("""
                        "name": "Nimic SRL", "supplier": true
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("partner.type.required")));
    }

    // ---------- A carrier's drivers ----------

    @Test
    void aCarriersDriversAreSavedWithHimAndListedForTheMovementForm() throws Exception {
        String partnerId = createPartnerReturningId("""
                "name": "Trans Greu SA", "carrier": true, "supplier": true,
                "drivers": [{"name": "Ion Popescu", "identification": "CJ 123456",
                             "vehicleRegistration": "CJ 01 ABC"}]
                """);

        JsonNode driver = onlyDriverOf(partnerId);
        assertThat(driver.get("name").asText()).isEqualTo("Ion Popescu");
        assertThat(driver.get("vehicleRegistration").asText()).isEqualTo("CJ 01 ABC");
        assertThat(driver.get("partnerName").asText()).isEqualTo("Trans Greu SA");
    }

    /**
     * The tick comes off for a season — the collector who usually hauls but this winter does not —
     * and a list of people typed in by hand must not evaporate with it. The update sends no
     * {@code drivers} at all, which is the "leave them alone" case.
     */
    @Test
    void untickingCarrierKeepsTheDrivers() throws Exception {
        String partnerId = createPartnerReturningId("""
                "name": "Uneori Transport SRL", "type": "COLLECTOR", "carrier": true, "supplier": true,
                "drivers": [{"name": "Vasile Ionescu"}]
                """);

        mockMvc.perform(put("/api/v1/partners/" + partnerId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Uneori Transport SRL", "type": "COLLECTOR",
                                 "carrier": false, "supplier": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carrier", is(false)))
                .andExpect(jsonPath("$.drivers[0].name", is("Vasile Ionescu")));
    }

    // ---------- Our own drivers ----------

    /** The "— transportăm noi —" case: a driver with no partner, managed in Settings. */
    @Test
    void ourOwnDriversHaveNoPartner() throws Exception {
        mockMvc.perform(post("/api/v1/drivers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Gheorghe Marin", "identification": "CJ 999888",
                                 "vehicleRegistration": "CJ 22 XYZ"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partnerId", is(nullValue())))
                .andExpect(jsonPath("$.name", is("Gheorghe Marin")));
    }

    /**
     * Two write paths into the same rows would mean a driver added through the endpoint disappears
     * the next time somebody opens and saves the partner. So the endpoint refuses him instead of
     * writing a row the partner form will silently drop.
     */
    @Test
    void theDriversEndpointRefusesACarriersDriver() throws Exception {
        String partnerId = createPartnerReturningId("""
                "name": "Alt Transport SRL", "carrier": true, "supplier": true,
                "drivers": [{"name": "Marius Radu"}]
                """);
        String driverId = onlyDriverOf(partnerId).get("id").asText();

        mockMvc.perform(delete("/api/v1/drivers/" + driverId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("driver.belongs.to.partner")));

        assertThat(onlyDriverOf(partnerId).get("active").asBoolean()).isTrue();
    }

    // ---------- What the paper shows ----------

    /**
     * The point of the whole slice: the haulier named on the movement is the one Anexa 3 prints in
     * the "Date de identificare transportator" column, with <em>his</em> goods-transport licence —
     * not the sender's, which is what the form falls back to when we haul it ourselves.
     */
    @Test
    void anexa3PrintsTheChosenCarrierAndHisLicence() throws Exception {
        String carrierId = createPartnerReturningId("""
                "name": "Trans Greu SA", "carrier": true, "supplier": true,
                "cui": "RO12345678", "address": "Str. Depozitelor nr. 4, Cluj-Napoca",
                "tradeRegisterNumber": "J12/999/2019",
                "transportLicenseNumber": "LIC 4417/2025", "transportLicenseExpiry": "2027-03-31",
                "drivers": [{"name": "Ion Popescu", "identification": "CJ 123456",
                             "vehicleRegistration": "CJ 01 ABC"}]
                """);
        JsonNode driver = onlyDriverOf(carrierId);

        UUID movementId = createMovement("""
                  "operation": "RECOVERED", "register": "ANEXA_1", "operationCode": "R3",
                  "partnerId": "%s", "quantity": 120,
                  "transportPartnerId": "%s",
                  "driverName": "%s", "driverIdentification": "%s", "vehicleRegistration": "%s",
                  "transportDestinations": ["COLECTARE", "VALORIFICARE"]
                """.formatted(recipientId, carrierId, driver.get("name").asText(),
                        driver.get("identification").asText(),
                        driver.get("vehicleRegistration").asText()));

        byte[] pdf = mockMvc.perform(get("/api/v1/movements/" + movementId + "/anexa3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdf);
        String page = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();

        assertThat(page).contains("Trans Greu SA");
        assertThat(page).contains("LIC 4417/2025");
        assertThat(page).contains("Ion Popescu");
        assertThat(page).contains("CJ 01 ABC");

        // The house rule: when a change touches an official form, render it and *look* at it.
        // The PDF stays in build/ so it can be opened after a run.
        java.nio.file.Files.write(java.nio.file.Path.of("build", "anexa3-carrier.pdf"), pdf);
    }

    // ---------- helpers ----------

    private UUID createMovement(String extraJson) throws Exception {
        String body = """
                {
                  "workPointId": "%s",
                  "date": "2026-07-05",
                  "wasteCodeId": "%s",
                  "unit": "KG",
                  "physicalState": "SOLID",
                  %s
                }
                """.formatted(workPointId, wasteCodeId, extraJson);
        String response = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private MockHttpServletRequestBuilder createPartner(String fields) {
        return post("/api/v1/partners")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + fields + "}");
    }

    private String createPartnerReturningId(String fields) throws Exception {
        String body = mockMvc.perform(createPartner(fields))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    /** Reads the drivers endpoint — the one the movement form calls — and picks this carrier's. */
    private JsonNode onlyDriverOf(String partnerId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/drivers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode match = null;
        for (JsonNode node : objectMapper.readTree(body)) {
            if (partnerId.equals(node.path("partnerId").asText(null))) {
                assertThat(match).as("more than one driver for the carrier").isNull();
                match = node;
            }
        }
        assertThat(match).as("no driver for the carrier").isNotNull();
        return match;
    }
}
