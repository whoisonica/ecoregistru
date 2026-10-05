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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MvcResult;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;

import java.time.Instant;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Din 05.10.2026: Anexa 1 și Declarația se deschid și ca PDF ({@code format=pdf}), în tab, pentru că Chrome nu arată
 * un .xlsx sau un .docx. Textul se citește de pe pagina tipărită (ca în testele golden), iar fără {@code format}
 * descărcarea de azi rămâne exact cum era. Firme și cifre inventate.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class EnergyPdfIT {

    private static final int YEAR = 2025;
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private String admin;

    @BeforeEach
    void setUp() {
        Company company = companyRepository.save(Company.builder()
                .name("Ștefănescu Țesături " + suffix).cui("RO" + suffix).tradeRegisterNumber("J12/345/2001")
                .type(CompanyType.GENERATOR)
                .address("Cluj-Napoca, str. Exemplu nr. 1")
                .caenCode("3811")
                .contactPhone("0264 000 000").contactEmail("office@exemplu.ro")
                .contactName("Ion Popescu").contactRole("Manager Mediu")
                .energyContactName("Maria Exemplu")
                .active(true).createdAt(Instant.now()).build());
        admin = jwtService.generateToken(appUserRepository.save(AppUser.builder()
                .email("adm+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true)
                .createdAt(Instant.now()).build()));
    }

    @Test
    void annex1PdfOpensInlineWithTheFigures() throws Exception {
        tick("ELECTRICITY", "DIESEL", "HEAT");
        twelve("ELECTRICITY", "12.5", null);
        twelve("DIESEL", "1.25", null);
        for (int m = 1; m <= 11; m++) {
            cell("HEAT", m, "1", null);
        }
        MvcResult result = download("/anexa1", "pdf")
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();
        String disposition = result.getResponse().getHeader("Content-Disposition");
        assertThat(disposition).startsWith("inline").contains("Anexa%201%20energie%202025.pdf");
        byte[] pdf = result.getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

        String text = Golden.flat(Golden.pdfText(pdf));
        assertThat(text)
                .contains(Golden.flat("CAP. I – DATE DE CONTACT ALE CONSUMATORULUI DE ENERGIE"))
                .contains(Golden.flat("pentru anul 2025"))
                .contains("RO" + suffix)
                .contains(Golden.flat("Ștefănescu Țesături " + suffix))
                .contains(Golden.flat("CAP. II – DATE STATISTICE"))
                .contains(Golden.flat("CAP III. Încadrarea operatorului economic"))
                .contains(Golden.flat("CONSUM DE ENERGIE TOTAL ANUAL")).contains("TOTAL")
                // 12 × 12,5 MWh = 150 MWh → 12,9 tep; 12 × 1,25 t motorină = 15 t → 15,225 tep.
                .contains("150.000").contains("12.900").contains("15.000").contains("15.225")
                // Căldura are o lună lipsă: cantitatea rămâne goală, deci totalul anual nu se poate tipări.
                .doesNotContain("11.000");
    }

    @Test
    void annex1PdfTotalIsPrintedWhenEveryMonthIsIn() throws Exception {
        tick("ELECTRICITY", "DIESEL");
        twelve("ELECTRICITY", "12.5", null);
        twelve("DIESEL", "1.25", null);
        String text = Golden.flat(Golden.pdfText(pdf("/anexa1")));
        // 12,9 + 15,225 = 28,125 tep
        assertThat(text).contains("28.125");
    }

    @Test
    void annex1PdfOverTheThresholdIsRefusedLikeTheXlsx() throws Exception {
        tick("COAL");
        for (int m = 1; m <= 12; m++) {
            cell("COAL", m, "1", m == 12 ? "120" : "80");
        }
        download("/anexa1", "pdf")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("energy.over.threshold")));
    }

    @Test
    void declarationPdfOpensInlineWithTheSentence() throws Exception {
        MvcResult result = download("/declaratie", "pdf")
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .startsWith("inline").contains("Declaratie%20energie%202025.pdf");
        byte[] pdf = result.getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

        String text = Golden.flat(Golden.pdfText(pdf));
        assertThat(text)
                .contains(Golden.flat("Ștefănescu Țesături " + suffix))
                .contains(Golden.flat("C.U.I.: RO" + suffix))
                .contains("J12/345/2001")
                .contains(Golden.flat("Declarație"))
                .contains(Golden.flat("prin prezenta declarăm faptul că informațiile prezentate în Anexa de "
                        + "raportare sunt corecte și conforme cu realitatea."))
                .contains(Golden.flat("NUMELE ÎN CLAR ŞI SEMNĂTURA CONDUCĂTORULUI UNITĂŢII"))
                .contains(Golden.flat("DATA TRANSMITERII"))
                .contains("MariaExemplu");
    }

    @Test
    void withoutFormatTheDownloadsStayXlsxAndDocx() throws Exception {
        tick("ELECTRICITY");
        cell("ELECTRICITY", 1, "10", null);
        download("/anexa1", null).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"anexa1-energie-2025.xlsx\""));
        download("/anexa1", "xlsx").andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX));
        download("/declaratie", null).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", DOCX))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"declaratie-energie-2025.docx\""));
        download("/declaratie", "docx").andExpect(status().isOk())
                .andExpect(header().string("Content-Type", DOCX));
    }

    @Test
    void anUnknownFormatIs400() throws Exception {
        download("/anexa1", "docx").andExpect(status().isBadRequest());
        download("/declaratie", "xlsx").andExpect(status().isBadRequest());
        download("/anexa1", "csv").andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------------------------------------

    private ResultActions download(String path, String format) throws Exception {
        var request = get("/api/v1/energy" + path).param("year", String.valueOf(YEAR))
                .header("Authorization", "Bearer " + admin);
        if (format != null) {
            request.param("format", format);
        }
        return mockMvc.perform(request);
    }

    private byte[] pdf(String path) throws Exception {
        return download(path, "pdf").andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
    }

    private ResultActions putTo(String path, String body) throws Exception {
        return mockMvc.perform(put("/api/v1/energy" + path)
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void tick(String... carriers) throws Exception {
        putTo("/carriers", "{\"year\":%d,\"carriers\":[\"%s\"]}".formatted(YEAR, String.join("\",\"", carriers)))
                .andExpect(status().isOk());
    }

    private void cell(String carrier, int month, String quantity, String tep) throws Exception {
        putTo("/consumption", "{\"year\":%d,\"carrier\":\"%s\",\"month\":%d,\"quantity\":%s,\"tep\":%s}"
                .formatted(YEAR, carrier, month, quantity, tep)).andExpect(status().isOk());
    }

    private void twelve(String carrier, String quantity, String tep) throws Exception {
        for (int m = 1; m <= 12; m++) {
            cell(carrier, m, quantity, tep);
        }
    }
}
