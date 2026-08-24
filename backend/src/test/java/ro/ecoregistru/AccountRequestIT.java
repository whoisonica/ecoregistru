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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AccountRequest;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.enums.AccountRequestStatus;
import ro.ecoregistru.repository.AccountRequestRepository;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ETAPA G2 — the intake form, the only way into a closed register.
 *
 * <p>What these pin down:
 *
 * <ul>
 *   <li>submitting is public, and creates a request rather than an account — no user, no session,
 *       and nothing returned that could be used to probe which companies exist;</li>
 *   <li>reading the requests is not: only a PLATFORM_ADMIN sees what clients wrote;</li>
 *   <li>approving copies the answers onto a real company, profile included, so the account opens
 *       with its screens already narrowed — and creates the work point the form named, because
 *       asking for that address twice is how it ends up different in the two places;</li>
 *   <li>a request is handled once.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class AccountRequestIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired AccountRequestRepository accountRequestRepository;
    @Autowired WorkPointRepository workPointRepository;

    private String platformToken;
    private String tenantToken;

    @BeforeEach
    void setUp() {
        platformToken = jwtService.generateToken(
                appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow());
        tenantToken = jwtService.generateToken(
                appUserRepository.findByEmail("admin@demo.ro").orElseThrow());
    }

    @Test
    void anyoneMaySubmitTheForm() throws Exception {
        mockMvc.perform(submission("Tamplaria Noua SRL", "RO" + digits(), "GENERATOR"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    /** A request is not an account: nothing about it comes back to whoever sent it. */
    @Test
    void submittingReturnsNothingAndReadingIsPlatformOnly() throws Exception {
        mockMvc.perform(submission("Discreta SRL", "RO" + digits(), "GENERATOR"))
                .andExpect(status().isAccepted());

        // Anonymous is 401 (no session at all); a logged-in client user is 403 (a session, but
        // not this door). The two are different answers to different questions.
        mockMvc.perform(get("/api/v1/account-requests"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/account-requests")
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/account-requests")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk());
    }

    @Test
    void approvingCreatesTheCompanyWithTheProfileAndTheWorkPoint() throws Exception {
        String cui = "RO" + digits();
        mockMvc.perform(submission("Colectare Buna SRL", cui, "COLLECTOR"))
                .andExpect(status().isAccepted());
        AccountRequest request = latest(cui);

        mockMvc.perform(post("/api/v1/account-requests/" + request.getId() + "/approve")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cui", is(cui)))
                .andExpect(jsonPath("$.type", is("COLLECTOR")))
                // the profile travels, so the new account opens already narrowed
                .andExpect(jsonPath("$.authorizedOperationCodes", hasItem("R3")))
                // "ce tip de generator" travels too: it decides the packaging declaration, and
                // asking the client twice is how the two answers end up different.
                .andExpect(jsonPath("$.marketRoles", hasItem("TRADER")))
                .andExpect(jsonPath("$.transportLicenseNumber", is("LTM-777")))
                // The annual declaration's header travels as well. Asked once, at intake, by the
                // only party that knows them: the CAEN code cannot be derived from the CUI, and
                // support would otherwise have to chase both on the phone.
                .andExpect(jsonPath("$.caenCode", is("4677")))
                .andExpect(jsonPath("$.contactRole", is("Manager Mediu")));

        AccountRequest handled = accountRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(handled.getStatus()).isEqualTo(AccountRequestStatus.APPROVED);
        assertThat(handled.getCreatedCompany()).isNotNull();
        assertThat(handled.getHandledAt()).isNotNull();

        UUID companyId = handled.getCreatedCompany().getId();
        assertThat(workPointRepository.findAllByCompany_Id(companyId))
                .singleElement()
                .satisfies(wp -> {
                    assertThat(wp.getName()).isEqualTo("Hala Florești");
                    assertThat(wp.getAddress()).isEqualTo("Str. Depozitelor nr. 4, Florești");
                });
    }

    /**
     * The two declaration rubrics are optional, and a request without them still becomes a
     * company. Nothing is invented in their place: the sheet prints the rubric empty, which is
     * the whole module's rule — a missing figure must be visible as missing.
     */
    @Test
    void theDeclarationHeaderIsOptional() throws Exception {
        String cui = "RO" + digits();
        String body = """
                {
                  "companyName": "Fara Antet SRL",
                  "cui": "%s",
                  "companyType": "GENERATOR",
                  "contactEmail": "fara.antet@example.ro"
                }
                """.formatted(cui);
        mockMvc.perform(post("/api/v1/account-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/account-requests/" + latest(cui).getId() + "/approve")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caenCode").doesNotExist())
                .andExpect(jsonPath("$.contactRole").doesNotExist());
    }

    @Test
    void aRequestIsHandledOnce() throws Exception {
        String cui = "RO" + digits();
        mockMvc.perform(submission("Odata SRL", cui, "GENERATOR")).andExpect(status().isAccepted());
        UUID id = latest(cui).getId();

        mockMvc.perform(post("/api/v1/account-requests/" + id + "/approve")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/account-requests/" + id + "/reject")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("account.request.already.handled")));
    }

    // ---------- helpers ----------

    private AccountRequest latest(String cui) {
        return accountRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(r -> r.getCui().equals(cui))
                .findFirst()
                .orElseThrow();
    }

    /** A CUI unique per run: the company created on approval has to be new every time. */
    private String digits() {
        return String.valueOf(Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100_000_000L));
    }

    private MockHttpServletRequestBuilder submission(String name, String cui, String type) {
        String body = """
                {
                  "companyName": "%s",
                  "cui": "%s",
                  "companyType": "%s",
                  "companyAddress": "Str. Principala nr. 1, Cluj-Napoca",
                  "workPointName": "Hala Florești",
                  "workPointAddress": "Str. Depozitelor nr. 4, Florești",
                  "contactName": "Ion Popescu",
                  "contactEmail": "ion.popescu@example.ro",
                  "contactPhone": "0740111222",
                  "contactRole": "Manager Mediu",
                  "caenCode": "4677",
                  "environmentalAuthNumber": "AM-2026-14",
                  "transportMeans": "Autoutilitară 3,5 t",
                  "transportLicenseNumber": "LTM-777",
                  "marketRoles": ["TRADER"],
                  "operationCodes": ["R3", "R13"],
                  "wasteCodesText": "carton, folie de plastic",
                  "notes": "Colectăm de la trei magazine."
                }
                """.formatted(name, cui, type);
        return post("/api/v1/account-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
