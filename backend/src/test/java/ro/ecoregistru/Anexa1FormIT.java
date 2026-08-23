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
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.service.EvidenceCalculator;
import ro.ecoregistru.service.export.Anexa1Sheet;
import ro.ecoregistru.security.TenantContext;

import java.math.BigDecimal;
import java.util.List;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ETAPA G5 — the Anexa 1 form itself: an identification header and four chapters, one page per
 * waste code per work point, after the filled sheets received from the specialist
 * ({@code deseuri generate_Cluj_2025_Iuhos Lorena.pdf} and the ten workbooks beside it).
 *
 * <p>What these pin down:
 *
 * <ul>
 *   <li>every sheet is twelve rows, whatever happened in the year — the form is a twelve-row
 *       table and the stock has to be readable on each line;</li>
 *   <li>chapter 1 is the evidence engine's, not a second implementation of the stock identity;</li>
 *   <li>chapter 2 counts as "treated" only what this company did itself, which is why the filled
 *       model shows zero there while chapter 3 shows the whole quantity;</li>
 *   <li>the header's opening stock is what the year started with, not what it ended with.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class Anexa1FormIT {

    private static final int YEAR = 2026;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired EvidenceCalculator evidenceCalculator;

    private String token;
    private AppUser admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        mockMvc.perform(post("/api/v1/evidences/regenerate?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void everySheetIsTwelveRows() {
        List<Anexa1Sheet> sheets = sheets();
        assertThat(sheets).isNotEmpty();
        assertThat(sheets).allSatisfy(s -> assertThat(s.rows()).hasSize(12));
    }

    /** The header line "Stoc/kg" is where the year opened, not where it closed. */
    @Test
    void theHeaderCarriesTheOpeningStock() {
        // The demo data starts in 2026 with nothing carried over from 2025.
        assertThat(sheets()).allSatisfy(s ->
                assertThat(s.openingStock()).usingComparator(BigDecimal::compareTo)
                        .isEqualTo(BigDecimal.ZERO));
    }

    /**
     * Chapter 2 describes what happened on our own site. A recovery performed by a partner is
     * treated at their place, so it belongs in chapter 3 and not in the "Tratare" column here —
     * exactly as the filled model shows it, with 0.000 treated and the full quantity recovered.
     */
    @Test
    void chapterTwoCountsOnlyWhatWeTreatedOurselves() {
        Anexa1Sheet paper = sheets().stream()
                .filter(s -> s.wasteCode().equals("20 01 01"))
                .findFirst()
                .orElseThrow();

        // February at Cluj: 100 kg generated, 60 kg recovered by the collector, nothing by us.
        Anexa1Sheet.Anexa1MonthRow february = paper.rows().get(1);
        assertThat(february.recovered()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(new BigDecimal("60.000"));
        assertThat(february.treatedQuantity()).usingComparator(BigDecimal::compareTo)
                .isEqualTo(BigDecimal.ZERO);
        assertThat(february.recoveryOperators()).isNotBlank();
    }

    @Test
    void theFormIsAPdfWithOnePagePerSheet() throws Exception {
        byte[] pdf = mockMvc.perform(get("/api/v1/evidences/anexa1?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        // Read the page tree rather than grepping the bytes: OpenPDF compresses it.
        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(sheets().size());
        reader.close();
    }

    private List<Anexa1Sheet> sheets() {
        TenantContext.set(admin.getCompany().getId());
        try {
            return evidenceCalculator.anexa1(YEAR, null);
        } finally {
            TenantContext.clear();
        }
    }
}
