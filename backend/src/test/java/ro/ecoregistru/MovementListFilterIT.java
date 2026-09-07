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
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ce înseamnă `year` și `month` pe lista de mișcări.
 *
 * <p>Anul fără lună nu însemna nimic: se cerea `?year=2026` și veneau înapoi toate mișcările, din
 * toți anii. O filtrare ignorată în tăcere e mai rea decât una respinsă — ecranul arăta un an și
 * afișa tot.
 *
 * <p>Acum e treapta de mijloc de care are nevoie ecranul de mișcări: filtrul lui pornește pe luna
 * curentă, ca să nu aducă toată istoria firmei la fiecare deschidere, iar „tot anul" e drumul
 * înapoi către o predare de acum trei luni fără să-i nimerești luna din prima.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class MovementListFilterIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired ObjectMapper objectMapper;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;

    private String token;
    private UUID workPointId;
    private UUID wasteCodeId;

    /** Trei mișcări așezate anume: două în ani diferiți, două în luni diferite ale aceluiași an. */
    private UUID inJanuary2024;
    private UUID inMarch2024;
    private UUID inMarch2025;

    @BeforeEach
    void setUp() throws Exception {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        workPointId = workPointRepository.findAllByCompany_Id(admin.getCompany().getId()).get(0).getId();
        wasteCodeId = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();

        inJanuary2024 = createMovement("2024-01-15");
        inMarch2024 = createMovement("2024-03-20");
        inMarch2025 = createMovement("2025-03-20");
    }

    @Test
    void yearAndMonthNarrowToThatMonth() throws Exception {
        List<UUID> ids = list("year", "2024", "month", "3");
        assertThat(ids).contains(inMarch2024)
                .doesNotContain(inJanuary2024)
                .doesNotContain(inMarch2025);
    }

    @Test
    void yearAloneMeansTheWholeYear() throws Exception {
        List<UUID> ids = list("year", "2024");
        assertThat(ids).contains(inJanuary2024, inMarch2024)
                .doesNotContain(inMarch2025);
    }

    /** Fără niciun parametru, nu se filtrează nimic — cum era și înainte. */
    @Test
    void noYearMeansEverything() throws Exception {
        List<UUID> ids = list();
        assertThat(ids).contains(inJanuary2024, inMarch2024, inMarch2025);
    }

    // ---------- helpers ----------

    private UUID createMovement(String date) throws Exception {
        String body = """
                {
                  "workPointId": "%s",
                  "date": "%s",
                  "wasteCodeId": "%s",
                  "quantity": 5.000,
                  "unit": "KG",
                  "physicalState": "SOLID",
                  "operation": "GENERATED"
                }
                """.formatted(workPointId, date, wasteCodeId);
        String created = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(created).get("id").asText());
    }

    private List<UUID> list(String... params) throws Exception {
        var request = get("/api/v1/movements").header("Authorization", "Bearer " + token);
        for (int i = 0; i < params.length; i += 2) {
            request = request.param(params[i], params[i + 1]);
        }
        JsonNode rows = objectMapper.readTree(mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        List<UUID> ids = new ArrayList<>();
        for (JsonNode row : rows) ids.add(UUID.fromString(row.get("id").asText()));
        return ids;
    }
}
