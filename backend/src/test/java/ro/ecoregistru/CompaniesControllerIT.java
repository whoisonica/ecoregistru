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
import ro.ecoregistru.repository.AppUserRepository;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FAZA UI-1 / U4: the platform company directory (tenant switcher source of truth).
 * Only PLATFORM_ADMIN may enumerate all tenants; any tenant-scoped user is forbidden
 * from seeing other companies. Verifies the 403 error envelope too.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class CompaniesControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;

    private String platformToken;
    private String adminToken;
    private String operatorToken;
    private String viewerToken;

    @BeforeEach
    void setUp() {
        platformToken = tokenFor("platform@ecoregistru.ro");
        adminToken = tokenFor("admin@demo.ro");
        operatorToken = tokenFor("operator@demo.ro");
        viewerToken = tokenFor("viewer@demo.ro");
    }

    @Test
    void platformAdminSeesAtLeastTheDemoTenant() throws Exception {
        mockMvc.perform(get("/api/v1/companies")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].cui").exists());
    }

    @Test
    void adminIsForbidden() throws Exception {
        expectForbidden(adminToken);
    }

    @Test
    void operatorIsForbidden() throws Exception {
        expectForbidden(operatorToken);
    }

    @Test
    void viewerIsForbidden() throws Exception {
        expectForbidden(viewerToken);
    }

    @Test
    void unauthenticatedIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/companies"))
                .andExpect(status().isForbidden());
    }

    private void expectForbidden(String token) throws Exception {
        mockMvc.perform(get("/api/v1/companies")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$['error-code']", is("access.denied")));
    }

    private String tokenFor(String email) {
        return jwtService.generateToken(appUserRepository.findByEmail(email).orElseThrow());
    }
}
