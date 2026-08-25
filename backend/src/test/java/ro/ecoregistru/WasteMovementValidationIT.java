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
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.WasteMovementRepository;

import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Correctness of the FAZA M business rule: the R/D operation code is required for
 * RECOVERED (an R code) and DISPOSED (a D code), and forbidden for any other operation.
 * Exercised through the real HTTP stack so the controller -> service -> AdviceController
 * envelope is covered too.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class WasteMovementValidationIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WasteMovementRepository movementRepository;

    private String token;
    private UUID workPointId;
    private UUID wasteCodeId;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        // Reuse the seeded tenant's own work point + waste code so the payload is valid
        // in every respect except the rule under test.
        WasteMovement seeded = movementRepository
                .findAllByCompany_IdAndDeletedFalse(admin.getCompany().getId()).get(0);
        workPointId = seeded.getWorkPoint().getId();
        wasteCodeId = seeded.getWasteCode().getId();
    }

    /** RECOVERED without any operation code must be rejected. */
    @Test
    void recoveredWithoutOperationCodeIsRejected() throws Exception {
        mockMvc.perform(postMovement("RECOVERED", "SOLID", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.operation.code.recovery")));
    }

    /** RECOVERED with a disposal (D) code is the wrong family and must be rejected. */
    @Test
    void recoveredWithDisposalCodeIsRejected() throws Exception {
        mockMvc.perform(postMovement("RECOVERED", "SOLID", "D5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.operation.code.recovery")));
    }

    /** DISPOSED without a D code must be rejected. */
    @Test
    void disposedWithoutOperationCodeIsRejected() throws Exception {
        mockMvc.perform(postMovement("DISPOSED", "SOLID", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.operation.code.disposal")));
    }

    /** A plain GENERATED movement may not carry an R/D code. */
    @Test
    void operationCodeIsForbiddenForGenerated() throws Exception {
        mockMvc.perform(postMovement("GENERATED", "SOLID", "R1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("movement.operation.code.not.allowed")));
    }

    /** RECOVERED with a valid R code succeeds and the code round-trips in the response. */
    @Test
    void recoveredWithRecoveryCodeIsAccepted() throws Exception {
        mockMvc.perform(postMovement("RECOVERED", "LIQUID", "R3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationCode", is("R3")))
                .andExpect(jsonPath("$.physicalState", is("LIQUID")));
    }

    /**
     * An unknown enum constant (Jackson can't bind it) is a malformed body: it must be a
     * clean 400, not the generic 500 that would leak before the dedicated handler existed.
     */
    @Test
    void unknownEnumConstantIsBadRequestNotServerError() throws Exception {
        mockMvc.perform(postMovement("RECOVERED", "SOLID", "X9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("request.malformed")));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postMovement(
            String operation, String physicalState, String operationCode) {
        String opCodeJson = operationCode == null ? "null" : "\"" + operationCode + "\"";
        String body = """
                {
                  "workPointId": "%s",
                  "date": "2026-07-05",
                  "wasteCodeId": "%s",
                  "quantity": 5.000,
                  "unit": "KG",
                  "operation": "%s",
                  "physicalState": "%s",
                  "operationCode": %s,
                  "register": "ANEXA_1"
                }
                """.formatted(workPointId, wasteCodeId, operation, physicalState, opCodeJson);
        return post("/api/v1/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
