package ro.ecoregistru;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.service.EmailService;

import java.util.Map;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CLIENTS slice: platform-admin company management (create/update) + user invitation.
 * Verifies the strict PLATFORM_ADMIN gate, CUI validation/uniqueness, and that inviting a
 * user reuses the reset-password email flow (EmailService is mocked to avoid real SMTP).
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class CompanyManagementIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired ObjectMapper objectMapper;

    /** Mocked so no real mail is sent and we can assert the invite triggered an email. */
    @MockBean EmailService emailService;

    private String platformToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        platformToken = tokenFor("platform@ecoregistru.ro");
        adminToken = tokenFor("admin@demo.ro");
    }

    @Test
    void platformAdminCreatesCompany() throws Exception {
        String cui = uniqueCui();
        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("Firmă Nouă SRL", cui, "GENERATOR", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Firmă Nouă SRL")))
                .andExpect(jsonPath("$.cui", is(cui)))
                .andExpect(jsonPath("$.afmObligation", is(true)))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    void nonPlatformAdminIsForbiddenToCreate() throws Exception {
        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("Ilegal SRL", uniqueCui(), "GENERATOR", false)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$['error-code']", is("access.denied")));
    }

    @Test
    void invalidCuiIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("CUI Prost SRL", "not-a-cui", "GENERATOR", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("company.cui.invalid")));
    }

    @Test
    void duplicateCuiIsRejected() throws Exception {
        String cui = uniqueCui();
        String body = companyBody("Prima SRL", cui, "GENERATOR", false);
        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("A Doua SRL", cui, "COLLECTOR", false)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("company.cui.exists")));
    }

    @Test
    void platformAdminUpdatesCompany() throws Exception {
        String cui = uniqueCui();
        String id = objectMapper.readTree(
                mockMvc.perform(post("/api/v1/companies")
                                .header("Authorization", "Bearer " + platformToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(companyBody("Editabilă SRL", cui, "GENERATOR", false)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/companies/" + id)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("Redenumită SRL", cui, "BOTH", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Redenumită SRL")))
                .andExpect(jsonPath("$.type", is("BOTH")))
                .andExpect(jsonPath("$.afmObligation", is(true)));
    }

    @Test
    void platformAdminInvitesUser() throws Exception {
        String id = createCompany();
        String email = "invited+" + UUID.randomUUID().toString().substring(0, 8) + "@client.ro";

        mockMvc.perform(post("/api/v1/companies/" + id + "/users")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteBody(email, "OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.role", is("OPERATOR")))
                .andExpect(jsonPath("$.enabled", is(false)));

        AppUser created = appUserRepository.findByEmail(email).orElseThrow();
        assertEquals(Role.OPERATOR, created.getRole());
        assertNotNull(created.getCompany());
        assertEquals(id, created.getCompany().getId().toString());
        assertFalse(created.isEnabled());
        verify(emailService, times(1)).sendPasswordResetEmail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void inviteRejectsPlatformAdminRole() throws Exception {
        String id = createCompany();
        mockMvc.perform(post("/api/v1/companies/" + id + "/users")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteBody("boss+" + UUID.randomUUID() + "@client.ro", "PLATFORM_ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("invite.role.invalid")));
    }

    @Test
    void inviteRejectsDuplicateEmail() throws Exception {
        String id = createCompany();
        String email = "dup+" + UUID.randomUUID().toString().substring(0, 8) + "@client.ro";
        String body = inviteBody(email, "CLIENT_VIEWER");
        mockMvc.perform(post("/api/v1/companies/" + id + "/users")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/companies/" + id + "/users")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("account.already.exists")));
    }

    @Test
    void nonPlatformAdminIsForbiddenToInvite() throws Exception {
        String id = createCompany();
        mockMvc.perform(post("/api/v1/companies/" + id + "/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteBody("x+" + UUID.randomUUID() + "@client.ro", "OPERATOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$['error-code']", is("access.denied")));
    }

    // --- helpers ---

    private String createCompany() throws Exception {
        String response = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("Client " + UUID.randomUUID().toString().substring(0, 6) + " SRL",
                                uniqueCui(), "GENERATOR", false)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String companyBody(String name, String cui, String type, boolean afm) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "name", name, "cui", cui, "type", type, "afmObligation", afm));
    }

    private String inviteBody(String email, String role) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email, "role", role, "firstName", "Test", "lastName", "User"));
    }

    /** A unique, well-formed CUI (RO + 8 digits) so methods don't collide on the unique column. */
    private String uniqueCui() {
        long n = Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000L);
        return String.format("RO%08d", n);
    }

    private String tokenFor(String email) {
        return jwtService.generateToken(appUserRepository.findByEmail(email).orElseThrow());
    }
}
