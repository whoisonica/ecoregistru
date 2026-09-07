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
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dezactivarea se poate lua înapoi.
 *
 * <p>Până acum nu se putea: prima greșeală era definitivă la punct de lucru, partener, secție și
 * șofer. Dezactivarea nu șterge niciun rând — dinadins, fiindcă mișcările vechi îl citează — deci
 * n-avea de ce să fie ireversibilă; pur și simplu nu exista drumul înapoi.
 *
 * <p>Proba merge pe toate patru, fiindcă simetria e chiar lucrul care se poate strica: fiecare
 * dintre ele are propriul controller, propriul serviciu și propria regulă de rol, iar una uitată
 * ar arăta pe ecran ca un buton care nu face nimic.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ReactivationIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired ObjectMapper objectMapper;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;

    private String token;
    private String viewerToken;
    private UUID workPointId;
    private UUID partnerId;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        viewerToken = jwtService.generateToken(appUserRepository.findByEmail("viewer@demo.ro").orElseThrow());
        UUID tenantId = admin.getCompany().getId();
        workPointId = workPointRepository.findAllByCompany_Id(tenantId).get(0).getId();
        partnerId = partnerRepository.findAllByCompany_Id(tenantId).get(0).getId();
    }

    @Test
    void aWorkPointComesBack() throws Exception {
        roundTrip("/api/v1/work-points", workPointId);
    }

    @Test
    void aPartnerComesBack() throws Exception {
        roundTrip("/api/v1/partners", partnerId);
    }

    /**
     * Secțiile sunt și ele semănate la crearea punctului de lucru („Birouri", „Producţie"), deci
     * există una fără să o creăm noi.
     */
    @Test
    void anInternalGeneratorComesBack() throws Exception {
        roundTrip("/api/v1/internal-generators", firstIdOf("/api/v1/internal-generators"));
    }

    @Test
    void aDriverComesBack() throws Exception {
        String body = """
                {"name": "Ion Popescu", "identification": "CJ 123456", "vehicleRegistration": "CJ 01 ABC"}
                """;
        String created = mockMvc.perform(post("/api/v1/drivers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(created).get("id").asText());
        roundTrip("/api/v1/drivers", id);
    }

    /** Aceeași regulă de rol ca dezactivarea: cine nu poate scoate un rând nu-l poate nici aduce. */
    @Test
    void aViewerCannotReactivate() throws Exception {
        mockMvc.perform(delete("/api/v1/work-points/" + workPointId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/work-points/" + workPointId + "/reactivate")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    /** Un id care nu e al firmei tale nu există, la fel ca peste tot. */
    @Test
    void anUnknownIdIsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/work-points/" + UUID.randomUUID() + "/reactivate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    /** Dezactivează, verifică că e stins, reactivează, verifică că e din nou aprins. */
    private void roundTrip(String path, UUID id) throws Exception {
        mockMvc.perform(delete(path + "/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(activeOf(path, id)).isFalse();

        mockMvc.perform(post(path + "/" + id + "/reactivate").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(activeOf(path, id)).isTrue();
    }

    private UUID firstIdOf(String path) throws Exception {
        JsonNode list = objectMapper.readTree(mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(list).isNotEmpty();
        return UUID.fromString(list.get(0).get("id").asText());
    }

    private boolean activeOf(String path, UUID id) throws Exception {
        JsonNode list = objectMapper.readTree(mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        for (JsonNode row : list) {
            if (row.get("id").asText().equals(id.toString())) return row.get("active").asBoolean();
        }
        throw new AssertionError("rândul %s a dispărut din %s — dezactivarea nu trebuie să șteargă".formatted(id, path));
    }
}
