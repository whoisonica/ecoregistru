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
import ro.ecoregistru.entity.Partner;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.PackagingService;
import ro.ecoregistru.service.export.PackagingDeclaration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The packaging module — <b>Anexa 1 Ambalaje</b> (Ordinul 794/2012, anexa nr. 1), built on
 * 25.08.2026 from the filled copy the specialist sent
 * ({@code documente oficiale/RAPORTARE AMBALAJE 2021_anexa 1_ HRR.xlsx}).
 *
 * <p>What these pin down:
 *
 * <ul>
 *   <li><b>Only 15 01 xx counts.</b> A shop's cardboard recorded under 20 01 01 belongs on the
 *       waste-management record and nowhere near this form — the distinction the specialist drew
 *       on 24.08.2026, and the one that decides how large the client's declared quantity is;</li>
 *   <li><b>one line per operator</b>, which nota 1 of tabelul 2 asks for in writing;</li>
 *   <li><b>tabelul 1 is answered, not computed</b>, and what nobody answered stays null all the way
 *       to the paper rather than turning into a zero;</li>
 *   <li>a code whose material the European List does not settle (15 01 04 — aluminium and steel
 *       share it) lands in "Altele" and is <em>named</em> as unresolved instead of parked quietly.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PackagingDeclarationIT {

    private static final int YEAR = 2026;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PackagingService packagingService;
    @Autowired ro.ecoregistru.repository.PartnerWorkPointRepository workPointRepositoryForPartner;

    private String token;
    private UUID tenantId;
    private UUID workPointId;
    private Partner collector;
    private Partner recycler;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Ambalaje " + suffix).cui("ROA" + suffix)
                .type(ro.ecoregistru.enums.CompanyType.GENERATOR)
                .address("Cluj-Napoca, str. Exemplu nr. 1")
                .caenCode("4677")
                .contactName("Ion Popescu").contactRole("Manager Mediu")
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("amb+" + suffix + "@demo.ro").password("x")
                .role(ro.ecoregistru.enums.Role.ADMIN).company(company).enabled(true)
                .createdAt(Instant.now()).build());
        token = jwtService.generateToken(admin);
        workPointId = workPointRepository.save(ro.ecoregistru.entity.WorkPoint.builder()
                .company(company).name("Sediu").active(true).createdAt(Instant.now()).build()).getId();

        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector Ambalaje SRL").cui("RO111" + suffix.substring(0, 3))
                .type(PartnerType.COLLECTOR).client(true).active(true)
                .createdAt(Instant.now()).build());
        // Un singur punct de lucru: atunci el e cel scris pe formular, fără să aleagă nimeni.
        workPointRepositoryForPartner.save(ro.ecoregistru.entity.PartnerWorkPoint.builder()
                .partner(collector).name("P.L. Ilfov").address("Şos. de Centură 2-8")
                .active(true).createdAt(Instant.now()).build());

        recycler = partnerRepository.save(Partner.builder()
                .company(company).name("Reciclator Hârtie SA").cui("RO222" + suffix.substring(0, 3))
                .type(PartnerType.RECOVERER).client(true).active(true)
                .createdAt(Instant.now()).build());
    }

    /**
     * Two operators took cardboard packaging in the same year, so tabelul 2 has two lines — nota 1:
     * "câte o rubrică distinctă pentru fiecare dintre operatorii care au preluat".
     */
    @Test
    void oneLinePerOperatorAndOnlyPackagingCodes() throws Exception {
        handover("15 01 01", "300", collector.getId(), "R13");
        handover("15 01 01", "200", recycler.getId(), "R3");
        // Not packaging: a shop's cardboard under the municipal code stays out of this form.
        handover("20 01 01", "900", collector.getId(), "R3");

        List<PackagingDeclaration.HandoverRow> rows = handovers();

        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(r ->
                assertThat(r.material()).isEqualTo(PackagingMaterial.HARTIE_CARTON));
        assertThat(rows).extracting(PackagingDeclaration.HandoverRow::operatorName)
                .containsExactlyInAnyOrder("Colector Ambalaje SRL", "Reciclator Hârtie SA");
        assertThat(rows).extracting(PackagingDeclaration.HandoverRow::operation)
                .containsExactlyInAnyOrder("R13", "R3");
        // The recipient's work point, not its head office — that is what the form asks for.
        assertThat(rows).anySatisfy(r ->
                assertThat(r.operatorAddress()).contains("Şos. de Centură"));
    }

    /**
     * 15 01 04 is "ambalaje metalice": aluminium cans and steel drums share it, and the form has a
     * row for each. We cannot tell, so the quantity goes to "Altele" and the code is named on the
     * paper — the client moves it if they know.
     */
    @Test
    void metalPackagingIsNamedAsUnresolvedRatherThanGuessed() throws Exception {
        handover("15 01 04", "120", collector.getId(), "R4");

        PackagingDeclaration declaration = declaration();

        assertThat(declaration.handoverRows()).singleElement()
                .satisfies(r -> assertThat(r.material()).isEqualTo(PackagingMaterial.ALTELE));
        assertThat(declaration.ambiguousCodes()).containsExactly("15 01 04");
    }

    /** Table 1 is answered, and an unanswered material stays null all the way to the paper. */
    @Test
    void theMarketTableIsAnsweredAndUnansweredMaterialsStayEmpty() throws Exception {
        mockMvc.perform(put("/api/v1/packaging/market")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"material": "OTEL", "year": %d, "salesPackaging": 5192,
                                 "secondaryTotal": 5192}
                                """.formatted(YEAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesPackaging", is(5192)));

        mockMvc.perform(get("/api/v1/packaging/market?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // Every material has a row, whether or not anyone answered for it.
                .andExpect(jsonPath("$.length()", is(PackagingMaterial.values().length)))
                .andExpect(jsonPath("$[?(@.material == 'OTEL')].salesPackaging",
                        is(List.of(5192.0))))
                .andExpect(jsonPath("$[?(@.material == 'STICLA')].salesPackaging",
                        is(java.util.Collections.singletonList(null))));

        // "Total (col. 3+5)" is a sum, never stored — and with only a secondary figure it is that.
        PackagingDeclaration d = declaration();
        assertThat(d.marketRows()).filteredOn(r -> r.material() == PackagingMaterial.OTEL)
                .singleElement()
                .satisfies(r -> assertThat(r.packagedGoodsTotal())
                        .usingComparator(java.math.BigDecimal::compareTo)
                        .isEqualTo(new java.math.BigDecimal("5192")));
        assertThat(d.marketRows()).filteredOn(r -> r.material() == PackagingMaterial.STICLA)
                .singleElement()
                .satisfies(r -> assertThat(r.packagedGoodsTotal()).isNull());
    }

    @Test
    void theDeclarationIsAPdfNamedAfterTheAnnex() throws Exception {
        handover("15 01 01", "300", collector.getId(), "R13");

        byte[] pdf = mockMvc.perform(get("/api/v1/packaging/anexa1?year=" + YEAR)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdf);
        String text = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();

        // Only what the extractor can see: OpenPDF's text extractor walks the page content but
        // not the cells of a PdfPTable, so the tables are checked on the model above and on a
        // rendered page by eye (docs/status.md), not by grepping bytes here.
        assertThat(text).contains("ANEXA Nr. 1");
        assertThat(text).contains("Tabel 1.");
        assertThat(text).contains("Se completeaz");   // nota 1 of tabelul 2
        assertThat(reader2Pages(pdf)).isEqualTo(1);
    }

    private int reader2Pages(byte[] pdf) throws Exception {
        com.lowagie.text.pdf.PdfReader r = new com.lowagie.text.pdf.PdfReader(pdf);
        int pages = r.getNumberOfPages();
        r.close();
        return pages;
    }

    // ---------- helpers ----------

    private List<PackagingDeclaration.HandoverRow> handovers() {
        TenantContext.set(tenantId);
        try {
            return packagingService.handovers(YEAR);
        } finally {
            TenantContext.clear();
        }
    }

    private PackagingDeclaration declaration() {
        TenantContext.set(tenantId);
        try {
            return packagingService.declaration(YEAR);
        } finally {
            TenantContext.clear();
        }
    }

    private void handover(String code, String quantity, UUID partnerId, String operationCode)
            throws Exception {
        UUID codeId = wasteCodeRepository.findByCode(code).orElseThrow().getId();
        String body = """
                {
                  "workPointId": "%s", "date": "%d-05-12", "wasteCodeId": "%s",
                  "unit": "KG", "quantity": %s,
                  "operation": "RECOVERED", "operationCode": "%s", "partnerId": "%s"
                }
                """.formatted(workPointId, YEAR, codeId, quantity, operationCode, partnerId);
        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }
}
