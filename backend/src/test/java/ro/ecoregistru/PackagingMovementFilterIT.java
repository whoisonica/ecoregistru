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
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.WorkPoint;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.time.Instant;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tastele de ambalaje de pe „Mișcări" — {@code ?packaging=ANY|ON_MARKET|INCOMPLETE}.
 *
 * <p>Există de pe 18.09.2026, când registrul de mișcări al tabului „Ambalaje" a fost scos:
 * aceleași rânduri se vedeau în două tabele, iar al doilea nu putea nici căuta, nici sorta, nici
 * pagina la server. Întrebările lui au rămas, ca filtru al singurei liste de mișcări.
 *
 * <p>Ce probează, pe patru mișcări care se deosebesc tocmai prin ce întreabă filtrul: că nimic în
 * afara capitolului {@code 15 01} nu trece, că bifa „l-am pus noi pe piață" scoate rândul din
 * {@code ON_MARKET} dar nu și din {@code ANY}, și că {@code INCOMPLETE} arată exact rândurile
 * cărora le lipsește ceva ce se completează pe mișcare — nici mai multe, nici mai puține.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PackagingMovementFilterIT {

    private static final int YEAR = 2026;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;

    private String token;
    private UUID workPointId;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Tipografia " + suffix).cui("ROF" + suffix)
                .type(CompanyType.GENERATOR)
                .address("Cluj-Napoca, str. Tipografilor nr. 1").caenCode("1812")
                .active(true).createdAt(Instant.now()).build());
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("amb+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true)
                .createdAt(Instant.now()).build());
        token = jwtService.generateToken(admin);
        workPointId = workPointRepository.save(WorkPoint.builder()
                .company(company).name("Atelier").active(true).createdAt(Instant.now()).build())
                .getId();

        // (1) Completă: codul decide materialul (15 01 01 → Hârtie carton), felul e răspuns.
        exit("15 01 01", "100", ", \"packagingCategory\": \"SECONDARY\"");
        // (2) Fără material: 15 01 04 acoperă și aluminiul, și oțelul — se alege pe mișcare.
        exit("15 01 04", "200", ", \"packagingCategory\": \"SECONDARY\"");
        // (3) Ambalaj pus pe piață de furnizor: e ambalaj, dar nu hrănește Anexa 1.
        exit("15 01 01", "300", ", \"packagingCategory\": \"SECONDARY\", \"packagingOnMarket\": false");
        // (4) Nu e ambalaj deloc: cartonul de birou pe 20 01 01 rămâne în evidența gestiunii.
        exit("20 01 01", "400", "");
    }

    @Test
    void everyKeyAsksItsOwnQuestion() throws Exception {
        // Fără tastă: toate cele patru, ca până acum.
        list(null).andExpect(jsonPath("$.totalElements").value(4));
        // „Ambalaje": tot capitolul 15 01, inclusiv ce n-am pus noi pe piață.
        list("ANY").andExpect(jsonPath("$.totalElements").value(3));
        // „Pus de noi pe piață": fără rândul bifat cu „nu".
        list("ON_MARKET").andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.quantity == 300)]").doesNotExist());
        // „De completat": numai rândul fără material.
        list("INCOMPLETE").andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].wasteCode").value("15 01 04"));
    }

    /** Banda de totaluri stă deasupra listei, deci descrie aceleași rânduri, nu altele. */
    @Test
    void theTotalsStripFollowsTheSameKey() throws Exception {
        mockMvc.perform(get("/api/v1/movements/totals")
                        .header("Authorization", "Bearer " + token)
                        .param("year", String.valueOf(YEAR))
                        .param("packaging", "ON_MARKET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").value(2))
                .andExpect(jsonPath("$.quantityKg").value(300.0));
    }

    // ---------- helpers ----------

    private org.springframework.test.web.servlet.ResultActions list(String packaging) throws Exception {
        var request = get("/api/v1/movements")
                .header("Authorization", "Bearer " + token)
                .param("year", String.valueOf(YEAR));
        if (packaging != null) {
            request = request.param("packaging", packaging);
        }
        return mockMvc.perform(request).andExpect(status().isOk());
    }

    private void exit(String code, String quantity, String extra) throws Exception {
        UUID codeId = wasteCodeRepository.findByCode(code).orElseThrow().getId();
        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workPointId": "%s", "date": "%d-05-10", "wasteCodeId": "%s",
                                 "unit": "KG", "quantity": %s, "operation": "RECOVERED",
                                 "wasteDestination": "Vr", "operationCode": "R3"%s}
                                """.formatted(workPointId, YEAR, codeId, quantity, extra)))
                .andExpect(status().isOk());
    }
}
