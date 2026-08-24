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
import ro.ecoregistru.util.Diacritics;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Searching the nomenclator without a Romanian keyboard.
 *
 * <p>The 842 names of the European List of Waste are spelled with diacritics, and the search used
 * to compare them literally: typing „deseuri" returned nothing at all. V17 stores a folded copy of
 * code + name in a generated column and {@link Diacritics} folds the query the same way, so the
 * three spellings a user can produce — none, comma-below, cedilla — are one search.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class WasteCodeSearchIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;

    private String token;

    @BeforeEach
    void setUp() {
        token = jwtService.generateToken(appUserRepository.findByEmail("admin@demo.ro").orElseThrow());
    }

    /** The bug as reported: a keyboard without diacritics finds nothing. */
    @Test
    void searchWithoutDiacriticsFindsNamesWrittenWithThem() throws Exception {
        mockMvc.perform(get("/api/v1/waste-codes").param("q", "deseuri")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)));
    }

    /** And the correct spelling keeps working, so the fix is not a swap of one failure for another. */
    @Test
    void searchWithDiacriticsStillWorks() throws Exception {
        mockMvc.perform(get("/api/v1/waste-codes").param("q", "deșeuri")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)));
    }

    /**
     * Official files mix the two Unicode spellings of ș and ț — comma below (correct) and cedilla
     * (legacy). Someone pasting a name out of one of them must not get an empty list.
     */
    @Test
    void theCedillaSpellingFindsTheSameRows() throws Exception {
        mockMvc.perform(get("/api/v1/waste-codes").param("q", "deşeuri")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)));
    }

    /** Codes are searched through the same folded column, so digits and spaces still match. */
    @Test
    void searchingByCodeStillWorks() throws Exception {
        mockMvc.perform(get("/api/v1/waste-codes").param("q", "20 01 01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("20 01 01"));
    }

    /**
     * The Java folding must produce exactly what the SQL generated column stores; this pins the
     * seven letters both halves promise to handle.
     */
    @Test
    void foldingCoversBothSpellingsOfEveryRomanianLetter() {
        assertThat(Diacritics.fold("ăâîșțŞŢ")).isEqualTo("aaistst");
        assertThat(Diacritics.fold("DEȘEURI")).isEqualTo("deseuri");
        assertThat(Diacritics.fold(null)).isNull();
    }
}
