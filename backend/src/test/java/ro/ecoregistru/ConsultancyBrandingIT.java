package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.service.CloudinaryStorageService;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P2.14 — antetul cabinetului pe rapoartele neoficiale ale firmelor lui.
 *
 * <p><b>Scena.</b> Cabinetul „Antet Verde" are firma „Client Antet"; cabinetul „Vecin Albastru" are
 * firma „Client Vecin" și antetul lui; „Direct Antet" nu e în niciun cabinet. Numele nu se conțin unul
 * pe altul, ca o scurgere să se vadă în text.
 *
 * <p><b>Ce se probează pe pagina tipărită, nu pe DTO:</b> rezumatul (.pdf și .xlsx) și dosarul poartă
 * antetul; fișa oficială și evidența centralizată din același dosar nu îl poartă; firma directă și
 * firma altui cabinet nu primesc antetul ăstuia; un antet golit nu mai tipărește nimic.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ConsultancyBrandingIT {

    private static final String CABINET = "Cabinet Antet Verde";
    private static final String LINE = "Tel. 0722 111 222 · Oradea";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired ConsultancyRepository consultancyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired PartnerRepository partnerRepository;

    @MockitoBean CloudinaryStorageService storageService;

    private final int year = LocalDate.now().getYear();

    private Company client;
    private Company neighbourClient;
    private Company direct;
    private String anaToken;
    private String vladToken;
    private String clientAdminToken;
    private String directAdminToken;

    @BeforeEach
    void setUp() {
        Consultancy cabinet = consultancy(CABINET);
        Consultancy neighbour = consultancy("Cabinet Vecin Albastru");
        anaToken = jwtService.generateToken(consultant("ana", cabinet));
        vladToken = jwtService.generateToken(consultant("vlad", neighbour));

        client = company("Client Antet SRL", cabinet);
        neighbourClient = company("Client Vecin SRL", neighbour);
        direct = company("Direct Antet SRL", null);
        clientAdminToken = jwtService.generateToken(admin(client));
        directAdminToken = jwtService.generateToken(admin(direct));

        partnerRepository.save(Partner.builder()
                .company(client).name("Colector Antet").cui("RO" + suffix()).client(true).supplier(false)
                .carrier(false).type(PartnerType.COLLECTOR).active(true).createdAt(Instant.now()).build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ecranul consultantului
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void theConsultantSetsLogoAndLineAndReadsThemBack() throws Exception {
        byte[] png = png();
        brand(anaToken, png, LINE);

        mockMvc.perform(get("/api/v1/consultancy/branding").header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultancyName", is(CABINET)))
                .andExpect(jsonPath("$.headerLine", is(LINE)))
                .andExpect(jsonPath("$.hasLogo", is(true)));

        byte[] served = mockMvc.perform(get("/api/v1/consultancy/branding/logo")
                        .header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(served).isEqualTo(png);
    }

    @Test
    void onlyAnImageUnderTheLimitIsTakenAndOnlyAConsultantMayAsk() throws Exception {
        MockMultipartFile renamedText = new MockMultipartFile("file", "logo.png", "image/png",
                "nu sunt o imagine".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/consultancy/branding/logo").file(renamedText)
                        .header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("branding.logo.invalid")));

        byte[] huge = new byte[600 * 1024];
        System.arraycopy(png(), 0, huge, 0, 8);
        mockMvc.perform(multipart("/api/v1/consultancy/branding/logo")
                        .file(new MockMultipartFile("file", "mare.png", "image/png", huge))
                        .header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("branding.logo.too.large")));

        mockMvc.perform(put("/api/v1/consultancy/branding").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"headerLine\":\"" + "x".repeat(201) + "\"}")
                        .header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("branding.header.too.long")));

        mockMvc.perform(get("/api/v1/consultancy/branding").header("Authorization", "Bearer " + clientAdminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/consultancy/branding").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"headerLine\":\"furat\"}")
                        .header("Authorization", "Bearer " + clientAdminToken))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pe hârtie
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void theSummaryOfACabinetCompanyCarriesTheCabinetHeader() throws Exception {
        brand(anaToken, png(), LINE);

        byte[] pdf = export(anaToken, client, "pdf");
        assertThat(Golden.flat(Golden.pdfText(pdf)))
                .contains(Golden.flat("Pregătit de " + CABINET))
                .contains(Golden.flat("Tel. 0722 111 222"));
        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).as("logoul e în PDF").contains("/Subtype/Image");

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(export(anaToken, client, "xlsx")))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("Pregătit de " + CABINET + " · " + LINE);
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Client Antet SRL");
            assertThat(((XSSFSheet) sheet).getDrawingPatriarch().getShapes()).as("logoul e în foaie").hasSize(1);
        }
    }

    /** Antetul e al firmei, nu al celui care descarcă: adminul firmei primește aceeași pagină. */
    @Test
    void theCompanysOwnAdminGetsTheSameHeader() throws Exception {
        brand(anaToken, null, LINE);

        assertThat(Golden.flat(Golden.pdfText(export(clientAdminToken, null, "pdf"))))
                .contains(Golden.flat("Pregătit de " + CABINET));
    }

    @Test
    void aDirectClientAndAnotherCabinetsClientDoNotGetThisHeader() throws Exception {
        brand(anaToken, png(), LINE);
        brand(vladToken, null, "Linia vecinului");

        String directPdf = Golden.pdfText(export(directAdminToken, null, "pdf"));
        assertThat(directPdf).doesNotContain("Antet Verde").doesNotContain("Pregătit").doesNotContain("Pregªtit");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(export(directAdminToken, null, "xlsx")))) {
            assertThat(wb.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("Direct Antet SRL");
        }

        String neighbourPdf = Golden.flat(Golden.pdfText(export(vladToken, neighbourClient, "pdf")));
        assertThat(neighbourPdf).contains(Golden.flat("Pregătit de Cabinet Vecin Albastru"))
                .doesNotContain("AntetVerde");
    }

    /** Un antet golit și un logo șters lasă pagina exact cum era — nu „Pregătit de" pe un rând gol. */
    @Test
    void aClearedHeaderPrintsNothing() throws Exception {
        brand(anaToken, png(), LINE);
        mockMvc.perform(delete("/api/v1/consultancy/branding/logo").header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasLogo", is(false)));
        brand(anaToken, null, "   ");

        String text = Golden.pdfText(export(anaToken, client, "pdf"));
        assertThat(text).doesNotContain("Antet Verde");
        mockMvc.perform(get("/api/v1/consultancy/branding/logo").header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void theDossierBrandsTheWorkingPackAndLeavesTheOfficialSheetsAlone() throws Exception {
        brand(anaToken, png(), LINE);

        Map<String, byte[]> zip = unzip(mockMvc.perform(get("/api/v1/audit-file").param("year", String.valueOf(year))
                        .header("Authorization", "Bearer " + anaToken).header("X-Tenant-Id", client.getId().toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());

        assertThat(new String(zip.get("00-cuprins.txt"), StandardCharsets.UTF_8))
                .contains("DOSAR DE CONTROL — Client Antet SRL\nPregătit de " + CABINET + " · " + LINE);
        assertThat(Golden.flat(Golden.pdfText(zip.get("autorizatii-parteneri.pdf"))))
                .contains(Golden.flat("Pregătit de " + CABINET));

        for (String official : new String[]{
                "rapoarte/evidenta-gestiunii-deseurilor-" + year + ".pdf", "rapoarte/evidenta-centralizata-" + year + ".pdf"}) {
            assertThat(zip).containsKey(official);
            assertThat(Golden.flat(Golden.pdfText(zip.get(official)))).as(official)
                    .doesNotContain("AntetVerde").doesNotContain("Pregătit");
            assertThat(new String(zip.get(official), StandardCharsets.ISO_8859_1)).as(official)
                    .doesNotContain("/Subtype/Image");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void brand(String token, byte[] logo, String line) throws Exception {
        if (logo != null) {
            mockMvc.perform(multipart("/api/v1/consultancy/branding/logo")
                            .file(new MockMultipartFile("file", "logo.png", "image/png", logo))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasLogo", is(true)));
        }
        mockMvc.perform(put("/api/v1/consultancy/branding").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"headerLine\":\"" + line + "\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    /** {@code tenant} null = the caller's own company (an admin); a consultant names the firm. */
    private byte[] export(String token, Company tenant, String format) throws Exception {
        var request = get("/api/v1/evidences/export").param("year", String.valueOf(year)).param("format", format)
                .header("Authorization", "Bearer " + token);
        if (tenant != null) {
            request.header("X-Tenant-Id", tenant.getId().toString());
        }
        return mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
    }

    private static Map<String, byte[]> unzip(byte[] bytes) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for (ZipEntry e; (e = zip.getNextEntry()) != null; ) {
                entries.put(e.getName(), zip.readAllBytes());
            }
        }
        return entries;
    }

    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(240, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0x15, 0x80, 0x3D));
        g.fillRect(0, 0, 240, 80);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private Consultancy consultancy(String name) {
        return consultancyRepository.save(Consultancy.builder()
                .name(name).cui("RO" + ThreadLocalRandom.current().nextLong(10_000_000L, 9_999_999_999L))
                .createdAt(Instant.now()).build());
    }

    private Company company(String name, Consultancy consultancy) {
        return companyRepository.save(Company.builder()
                .name(name).cui("RO" + ThreadLocalRandom.current().nextLong(10_000_000L, 9_999_999_999L))
                .type(CompanyType.GENERATOR).consultancy(consultancy)
                .afmObligation(false).active(true).createdAt(Instant.now()).build());
    }

    private AppUser consultant(String name, Consultancy consultancy) {
        return appUserRepository.save(AppUser.builder()
                .email(name + "+" + suffix() + "@cabinet.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.CONSULTANT).consultancy(consultancy).enabled(true).createdAt(Instant.now()).build());
    }

    private AppUser admin(Company company) {
        return appUserRepository.save(AppUser.builder()
                .email("admin+" + suffix() + "@firma.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
