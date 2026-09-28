package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.service.WasteCodeListScheduler;

import java.time.LocalDate;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V80 — lista deșeurilor are ediții. Decizia delegată (UE) 2025/934 se aplică de la 9.11.2026: aduce
 * 42 de coduri de baterii, scoate patru și îl face pe 16 06 04 periculos. Probele stau pe zilele de
 * graniță, 8 și 9 noiembrie, și nu depind de ziua în care rulează suita.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class WasteListEditionIT {

    static final LocalDate LAST_OLD_DAY = LocalDate.of(2026, 11, 8);
    static final LocalDate FIRST_NEW_DAY = LocalDate.of(2026, 11, 9);

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteCodeListScheduler scheduler;
    @Autowired JdbcTemplate jdbc;

    private String token;
    private UUID workPointId;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        WasteMovement seeded = movementRepository
                .findAllByCompany_IdAndDeletedFalse(admin.getCompany().getId()).get(0);
        workPointId = seeded.getWorkPoint().getId();
    }

    /** Amprenta ediției noi: 842 + 42 − 4 = 880 de coduri, 408 + 28 + 16 06 04 − 2 scoase = 435 periculoase. */
    @Test
    void theListInForceOnEachSideOfTheBorderHasItsOwnFingerprint() {
        assertThat(countValidOn(LAST_OLD_DAY, false)).isEqualTo(842);
        assertThat(countValidOn(FIRST_NEW_DAY, false)).isEqualTo(880);
        assertThat(jdbc.queryForObject("select count(*) from waste_codes", Integer.class)).isEqualTo(884);
    }

    @Test
    void thePickerFollowsTheMovementDate() throws Exception {
        mockMvc.perform(listCodes("20 01", LAST_OLD_DAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("20 01 33")))
                .andExpect(jsonPath("$[*].code", not(hasItem("20 01 42"))));
        mockMvc.perform(listCodes("20 01", FIRST_NEW_DAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("20 01 42")))
                .andExpect(jsonPath("$[*].code", not(hasItem("20 01 33"))));
        mockMvc.perform(listCodes("litiu", FIRST_NEW_DAY))
                .andExpect(jsonPath("$[*].code", hasItem("16 06 07")));
    }

    @Test
    void aMovementUsesOnlyTheCodeInForceOnItsDate() throws Exception {
        mockMvc.perform(postMovement(code("20 01 42"), LAST_OLD_DAY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("waste.code.not.yet.valid")));
        mockMvc.perform(postMovement(code("20 01 33"), FIRST_NEW_DAY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("waste.code.retired")));
    }

    /**
     * Schimbarea unui cod care rămâne se scrie în bază la ziua ei, nu mai devreme. Rulează în tranzacție,
     * ca nomenclatorul împărțit cu celelalte probe să revină la loc.
     */
    @Test
    @Transactional
    void onTheDayTheRenamedCodesChangeAndMirrorsFollow() {
        scheduler.apply(LAST_OLD_DAY);
        if (!alreadyApplied()) {
            assertThat(hazardous("16 06 04")).isFalse();
            // 161 din V37 + 12 noi (10 08 22/24/26, 16 06 12/15/23/25/27/29/31/33, 20 01 44).
            assertThat(mirrorCount()).isEqualTo(173);
        }

        scheduler.apply(FIRST_NEW_DAY);

        assertThat(hazardous("16 06 04")).isTrue();
        assertThat(name("16 06 04")).isEqualTo("deșeuri de baterii alcaline (altele decât cele menționate la 16 06 03)");
        assertThat(name("16 06 01")).isEqualTo("deșeuri de acumulatori cu plumb acid");
        // Periculos acum, deci nu mai e jumătatea nepericuloasă a unei oglinzi.
        assertThat(mirrorOf("16 06 04")).isNull();
        assertThat(mirrorOf("16 06 15")).isEqualTo("16 06 14");
        assertThat(mirrorOf("20 01 44")).isEqualTo("20 01 42, 20 01 43");
        assertThat(mirrorOf("16 06 35")).isNull();
        assertThat(countValidOn(FIRST_NEW_DAY, true)).isEqualTo(435);
        // Recalcularea pe tot tabelul dă exact regula din V37: pleacă doar 16 06 04.
        assertThat(mirrorCount()).isEqualTo(172);
        // A doua rulare nu mai are nimic de aplicat.
        assertThat(scheduler.apply(FIRST_NEW_DAY)).isZero();
    }

    private int mirrorCount() {
        return jdbc.queryForObject("select count(*) from waste_codes where mirror_of is not null", Integer.class);
    }

    private boolean alreadyApplied() {
        return jdbc.queryForObject("select pending_from is null from waste_codes where code = '16 06 04'", Boolean.class);
    }

    private int countValidOn(LocalDate day, boolean hazardousOnly) {
        return jdbc.queryForObject("""
                select count(*) from waste_codes
                where (valid_from is null or valid_from <= ?) and (valid_to is null or valid_to >= ?)
                  and (not ? or hazardous)
                """, Integer.class, day, day, hazardousOnly);
    }

    private UUID code(String code) {
        return wasteCodeRepository.findByCode(code).orElseThrow().getId();
    }

    private boolean hazardous(String code) {
        return jdbc.queryForObject("select hazardous from waste_codes where code = ?", Boolean.class, code);
    }

    private String name(String code) {
        return jdbc.queryForObject("select name from waste_codes where code = ?", String.class, code);
    }

    private String mirrorOf(String code) {
        return jdbc.queryForObject("select mirror_of from waste_codes where code = ?", String.class, code);
    }

    private MockHttpServletRequestBuilder listCodes(String q, LocalDate on) {
        return get("/api/v1/waste-codes")
                .param("q", q)
                .param("on", on.toString())
                .header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder postMovement(UUID wasteCodeId, LocalDate date) {
        String body = """
                {
                  "workPointId": "%s",
                  "date": "%s",
                  "wasteCodeId": "%s",
                  "quantity": 5.000,
                  "unit": "KG",
                  "operation": "RECOVERED",
                  "physicalState": "SOLID",
                  "storageType": "CT", "transportMeans": "AN", "wasteDestination": "Vr", "operationCode": "R4",
                  "register": "ANEXA_1"
                }
                """.formatted(workPointId, date, wasteCodeId);
        return post("/api/v1/movements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
