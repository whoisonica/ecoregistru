package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.repository.AppUserRepository;

import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BUG-043 — CNP-ul șoferului pleacă întreg doar la cine îl poate scrie pe un document (ADMIN, OPERATOR);
 * „Vizualizare” îl primește mascat în {@code GET /drivers}. Controlul: adminul îl primește întreg,
 * altfel formularul de mișcare n-ar mai putea copia CNP-ul pe aviz.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class DriverCnpMaskingIT {

    private static final String CNP = "1900101123457";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;

    @Test
    void aViewerGetsTheCnpMaskedAndAnAdminWhole() throws Exception {
        String name = "Masca " + UUID.randomUUID().toString().substring(0, 6);
        mockMvc.perform(post("/api/v1/drivers").header("Authorization", "Bearer " + token("admin@demo.ro"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"identification\":\"CJ 43\",\"cnp\":\"" + CNP + "\"}"))
                .andExpect(status().isOk());

        String asViewer = list("viewer@demo.ro");
        assertThat(asViewer).contains(name).doesNotContain(CNP).contains("190********57");
        assertThat(list("admin@demo.ro")).contains(CNP);
    }

    private String list(String email) throws Exception {
        return mockMvc.perform(get("/api/v1/drivers").header("Authorization", "Bearer " + token(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String token(String email) {
        return jwtService.generateToken(appUserRepository.findByEmail(email).orElseThrow());
    }
}
