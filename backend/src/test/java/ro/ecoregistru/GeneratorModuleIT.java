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
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.repository.*;

import java.time.Instant;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ETAPA G1 — the foundation of the generator module, as settled in the meeting of 23.08.2026.
 *
 * <p>Four rules, and the reason each one exists:
 *
 * <ul>
 *   <li>a partner carries a commercial role — client (we hand waste over and we invoice them) or
 *       supplier (they do the work and they invoice us), or both — and cannot be saved without
 *       one, because the screen colours rows by it;</li>
 *   <li>an internal generator is the section inside a work point, printed as "Secţia" in Anexa 1
 *       cap. 2, and belongs to exactly one work point for good;</li>
 *   <li>a movement may name the section it came from, and only one of its own work point;</li>
 *   <li>which operations a company may record follows from its type, and "predare" is not among
 *       them for anybody — see {@link RegisterSeamIT}.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class GeneratorModuleIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteMovementRepository movementRepository;

    private String token;
    private UUID tenantId;
    private UUID workPointId;
    private UUID wasteCodeId;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        tenantId = admin.getCompany().getId();
        WasteMovement seeded = movementRepository.findAllByCompany_IdAndDeletedFalse(tenantId).get(0);
        workPointId = seeded.getWorkPoint().getId();
        wasteCodeId = seeded.getWasteCode().getId();
    }

    // ---------- The commercial role of a partner ----------

    @Test
    void aPartnerWithoutACommercialRoleIsRejected() throws Exception {
        mockMvc.perform(partner("Fara Rol SRL", false, false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("partner.role.required")));
    }

    /**
     * The case that rules out a single enum: the same authorised operator buys our cardboard and
     * sells us a bin-emptying service.
     */
    @Test
    void aPartnerCanBeBothClientAndSupplier() throws Exception {
        mockMvc.perform(partner("Ambele SRL", true, true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.client", is(true)))
                .andExpect(jsonPath("$.supplier", is(true)));
    }

    // ---------- The internal generator: Anexa 1 cap. 2 "Secţia" ----------

    @Test
    void twoSectionsOfTheSameWorkPointCannotShareAName() throws Exception {
        mockMvc.perform(section(workPointId, "Atelier vopsitorie")).andExpect(status().isOk());
        mockMvc.perform(section(workPointId, "atelier vopsitorie"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("internal.generator.name.taken")));
    }

    /** Moving a section would rewrite the "Secţia" column of sheets already printed elsewhere. */
    @Test
    void aSectionCannotBeMovedToAnotherWorkPoint() throws Exception {
        UUID id = createSection(workPointId, "Cantina");
        UUID otherWorkPoint = otherWorkPoint();

        mockMvc.perform(put("/api/v1/internal-generators/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sectionBody(otherWorkPoint, "Cantina")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("internal.generator.work.point.immutable")));
    }

    @Test
    void aMovementCarriesTheSectionItCameFrom() throws Exception {
        UUID id = createSection(workPointId, "Depozit ambalaje");

        mockMvc.perform(movement(workPointId,
                        "  \"operation\": \"GENERATED\", \"internalGeneratorId\": \"" + id + "\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internalGeneratorId", is(id.toString())))
                .andExpect(jsonPath("$.internalGeneratorName", is("Depozit ambalaje")));
    }

    /** A section from another work point would name a source that never produced the waste. */
    @Test
    void aMovementRefusesASectionOfAnotherWorkPoint() throws Exception {
        UUID id = createSection(otherWorkPoint(), "Hala 2");

        mockMvc.perform(movement(workPointId,
                        "  \"operation\": \"GENERATED\", \"internalGeneratorId\": \"" + id + "\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("internal.generator.wrong.work.point")));
    }

    // ---------- Which operations the account type offers ----------

    @Test
    void theOperationsFollowTheCompanyType() {
        assertThat(CompanyType.GENERATOR.allowedOperations())
                .containsExactlyInAnyOrder(WasteOperation.GENERATED,
                        WasteOperation.RECOVERED, WasteOperation.DISPOSED);

        // A collector keeps Anexa 1 too, for its own waste (art. 2 alin. (1)), so it never loses
        // GENERATED — it only gains the takeover.
        assertThat(CompanyType.COLLECTOR.allowedOperations())
                .contains(WasteOperation.GENERATED, WasteOperation.COLLECTED);
        assertThat(CompanyType.BOTH.allowedOperations())
                .contains(WasteOperation.GENERATED, WasteOperation.COLLECTED);

        // Neither type may choose the legacy state a migration writes.
        assertThat(CompanyType.BOTH.allowedOperations())
                .doesNotContain(WasteOperation.UNCLASSIFIED_OUT);
    }

    @Test
    void theLegacyUnclassifiedExitCannotBeChosen() throws Exception {
        mockMvc.perform(movement(workPointId, "  \"operation\": \"UNCLASSIFIED_OUT\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.operation.not.selectable")));
    }

    /** Every screen needs the tenant's own type; only PLATFORM_ADMIN could read it before. */
    @Test
    void aMemberCanReadItsOwnCompanyProfile() throws Exception {
        mockMvc.perform(get("/api/v1/companies/current").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(tenantId.toString())))
                .andExpect(jsonPath("$.type", is("BOTH")));
    }

    // ---------- The account profile narrows what may be recorded ----------

    /**
     * The demo tenant answered R3, R4, R5, R13 and D5 on its intake form. R1 is a real operation
     * code and a perfectly valid one — it is simply not this client's, and offering it is how a
     * 28-entry list turns into a wrong pick months later.
     */
    @Test
    void anOperationCodeOutsideTheProfileIsRejected() throws Exception {
        mockMvc.perform(movement(workPointId,
                        "  \"operation\": \"RECOVERED\", \"register\": \"ANEXA_1\", \"operationCode\": \"R1\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.operation.code.not.in.profile")));
    }

    @Test
    void anOperationCodeInsideTheProfileIsAccepted() throws Exception {
        mockMvc.perform(movement(workPointId,
                        "  \"operation\": \"RECOVERED\", \"register\": \"ANEXA_1\", \"operationCode\": \"R3\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationCode", is("R3")));
    }

    /** Cap. 2 of Anexa 1: what the waste sat in, and what was done to it. */
    @Test
    void aMovementCarriesTheStorageAndTreatmentOfCapitolul2() throws Exception {
        mockMvc.perform(movement(workPointId,
                        "  \"operation\": \"GENERATED\", \"storageType\": \"CT\","
                                + " \"treatmentMethod\": \"TM\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageType", is("CT")))
                .andExpect(jsonPath("$.treatmentMethod", is("TM")));
    }

    /** The screens read the profile from here, so it has to travel with the company. */
    @Test
    void theProfileTravelsOnTheCurrentCompany() throws Exception {
        mockMvc.perform(get("/api/v1/companies/current").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizedOperationCodes", hasItem("R13")))
                .andExpect(jsonPath("$.authorizedOperationCodes", not(hasItem("R1"))))
                .andExpect(jsonPath("$.authorizedWasteCodes", not(empty())))
                .andExpect(jsonPath("$.transportLicenseNumber", is("LTM-2024-0912")));
    }

    // ---------- helpers ----------

    private UUID otherWorkPoint() {
        return workPointRepository.findAllByCompany_Id(tenantId).stream()
                .map(WorkPoint::getId)
                .filter(id -> !id.equals(workPointId))
                .findFirst()
                .orElseGet(() -> workPointRepository.save(WorkPoint.builder()
                        .company(appUserRepository.findByEmail("admin@demo.ro").orElseThrow().getCompany())
                        .name("PL " + UUID.randomUUID().toString().substring(0, 6))
                        .active(true).createdAt(Instant.now()).build()).getId());
    }

    private UUID createSection(UUID workPoint, String name) throws Exception {
        String response = mockMvc.perform(section(workPoint, name))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(response.split("\"id\":\"")[1].split("\"")[0]);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder section(
            UUID workPoint, String name) {
        return post("/api/v1/internal-generators")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sectionBody(workPoint, name));
    }

    private String sectionBody(UUID workPoint, String name) {
        return """
                { "workPointId": "%s", "name": "%s", "description": null }
                """.formatted(workPoint, name);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder partner(
            String name, boolean client, boolean supplier) {
        String body = """
                {
                  "name": "%s",
                  "cui": null,
                  "authorizationNumber": null,
                  "authorizationExpiry": null,
                  "type": "COLLECTOR",
                  "client": %s,
                  "supplier": %s
                }
                """.formatted(name, client, supplier);
        return post("/api/v1/partners")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder movement(
            UUID workPoint, String extraJson) {
        String body = """
                {
                  "workPointId": "%s",
                  "date": "2026-07-05",
                  "wasteCodeId": "%s",
                  "quantity": 5.000,
                  "unit": "KG",
                  "physicalState": "SOLID",
                %s
                }
                """.formatted(workPoint, wasteCodeId, extraJson);
        return post("/api/v1/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
