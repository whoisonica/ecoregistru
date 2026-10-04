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
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.AuditLogRepository;
import ro.ecoregistru.repository.CompanyRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.mockito.InOrder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ro.ecoregistru.service.CloudinaryStorageService;
import ro.ecoregistru.service.CloudinaryStorageService.StoredFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The energy sheet: carriers, monthly cells, declaration with measures, contact. */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class EnergyIT {

    private static final int YEAR = 2025;

    @MockitoBean CloudinaryStorageService storage;
    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired AuditLogRepository auditLogRepository;

    private String admin;
    private String viewer;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Energie " + suffix).cui("ROE" + suffix)
                .type(CompanyType.GENERATOR)
                .address("Cluj-Napoca, str. Exemplu nr. 1")
                .caenCode("4677")
                .contactName("Ion Popescu").contactRole("Manager Mediu")
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        admin = jwtService.generateToken(user(company, "adm", suffix, Role.ADMIN));
        viewer = jwtService.generateToken(user(company, "vw", suffix, Role.CLIENT_VIEWER));
    }

    private AppUser user(Company company, String tag, String suffix, Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(tag + "+" + suffix + "@demo.ro").password("x")
                .role(role).company(company).enabled(true)
                .createdAt(Instant.now()).build());
    }

    private ResultActions putTo(String path, String token, String body) throws Exception {
        return mockMvc.perform(put("/api/v1/energy" + path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions sheet(int year) throws Exception {
        return mockMvc.perform(get("/api/v1/energy?year=" + year).header("Authorization", "Bearer " + admin));
    }

    private void tick(String... carriers) throws Exception {
        putTo("/carriers", admin, "{\"carriers\":[\"" + String.join("\",\"", carriers) + "\"]}")
                .andExpect(status().isOk());
    }

    private ResultActions cell(String carrier, int month, String quantity, String tep) throws Exception {
        return putTo("/consumption", admin, "{\"year\":%d,\"carrier\":\"%s\",\"month\":%d,\"quantity\":%s,\"tep\":%s}"
                .formatted(YEAR, carrier, month, quantity, tep));
    }

    @Test
    void anEmptyYearReturnsAnEmptySheet() throws Exception {
        sheet(2024).andExpect(status().isOk())
                .andExpect(jsonPath("$.year", is(2024)))
                .andExpect(jsonPath("$.carriers", hasSize(0)))
                .andExpect(jsonPath("$.cells", hasSize(0)))
                .andExpect(jsonPath("$.totals", hasSize(0)))
                .andExpect(jsonPath("$.monthsComplete", is(0)))
                .andExpect(jsonPath("$.overThreshold", is(false)))
                .andExpect(jsonPath("$.declaration.sme", nullValue()))
                .andExpect(jsonPath("$.declaration.measures", hasSize(0)))
                .andExpect(jsonPath("$.receipt", nullValue()));
    }

    @Test
    void theYearMustBeBetween2020AndNow() throws Exception {
        sheet(2019).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("energy.year.invalid")));
        sheet(LocalDate.now().getYear() + 1).andExpect(status().isUnprocessableEntity());
        putTo("/consumption", admin, "{\"year\":2019,\"carrier\":\"ELECTRICITY\",\"month\":1,\"quantity\":1}")
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cellsAddUpToTheYear() throws Exception {
        tick("ELECTRICITY");
        for (int m = 1; m <= 12; m++) {
            cell("ELECTRICITY", m, "10", "null").andExpect(status().isOk());
        }
        sheet(YEAR).andExpect(status().isOk())
                .andExpect(jsonPath("$.carriers[0]", is("ELECTRICITY")))
                .andExpect(jsonPath("$.cells", hasSize(12)))
                .andExpect(jsonPath("$.totals[0].quantity", is(120.0)))
                .andExpect(jsonPath("$.totals[0].tep", is(10.32)))
                .andExpect(jsonPath("$.totalTep", is(10.32)))
                .andExpect(jsonPath("$.monthsComplete", is(12)))
                .andExpect(jsonPath("$.overThreshold", is(false)));
    }

    @Test
    void aNullQuantityDeletesTheCell() throws Exception {
        tick("ELECTRICITY");
        cell("ELECTRICITY", 3, "5", "null").andExpect(jsonPath("$.cells", hasSize(1)));
        cell("ELECTRICITY", 3, "null", "null").andExpect(status().isOk())
                .andExpect(jsonPath("$.cells", hasSize(0)));
        cell("ELECTRICITY", 3, "null", "null").andExpect(status().isOk())
                .andExpect(jsonPath("$.cells", hasSize(0)));
    }

    @Test
    void tepIsRefusedOnElectricity() throws Exception {
        tick("ELECTRICITY", "COAL");
        cell("ELECTRICITY", 1, "5", "1").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("energy.tep.not.allowed")));
        cell("COAL", 1, "5", "4.5").andExpect(status().isOk())
                .andExpect(jsonPath("$.cells[0].tep", is(4.5)));
    }

    @Test
    void aCellOnAnUntickedCarrierIsRefused() throws Exception {
        cell("ELECTRICITY", 1, "5", "null").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("energy.carrier.not.used")));
    }

    @Test
    void untickingKeepsTheRowsAndTickingBringsThemBack() throws Exception {
        tick("ELECTRICITY");
        cell("ELECTRICITY", 1, "5", "null").andExpect(status().isOk());
        tick("HEAT");
        sheet(YEAR).andExpect(jsonPath("$.carriers[0]", is("HEAT")))
                .andExpect(jsonPath("$.cells", hasSize(0)))
                .andExpect(jsonPath("$.totals", hasSize(1)));
        tick("ELECTRICITY", "HEAT");
        sheet(YEAR).andExpect(jsonPath("$.cells", hasSize(1)))
                .andExpect(jsonPath("$.cells[0].quantity", is(5.0)));
    }

    @Test
    void negativeQuantityIs422() throws Exception {
        tick("ELECTRICITY");
        cell("ELECTRICITY", 1, "-1", "null").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void twentyOneMeasuresAre422() throws Exception {
        String measures = String.join(",", java.util.Collections.nCopies(21, "{\"name\":\"m\"}"));
        putTo("/declaration", admin, "{\"year\":%d,\"measures\":[%s]}".formatted(YEAR, measures))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void theDeclarationReplacesEverythingAndRenumbersTheMeasures() throws Exception {
        putTo("/declaration", admin, ("{\"year\":%d,\"sme\":true,\"auditor\":\"X\",\"auditSharePct\":12.5,"
                + "\"measures\":[{\"name\":\"a\",\"costEstimated\":10},{\"name\":\"b\"},{\"name\":\"c\"}]}")
                .formatted(YEAR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.declaration.sme", is(true)))
                .andExpect(jsonPath("$.declaration.measures", hasSize(3)))
                .andExpect(jsonPath("$.declaration.measures[2].position", is(3)))
                .andExpect(jsonPath("$.declaration.measures[2].name", is("c")));
        putTo("/declaration", admin, "{\"year\":%d,\"measures\":[{\"name\":\"z\"},{\"name\":\"y\"}]}".formatted(YEAR))
                .andExpect(jsonPath("$.declaration.sme", nullValue()))
                .andExpect(jsonPath("$.declaration.auditor", nullValue()))
                .andExpect(jsonPath("$.declaration.measures", hasSize(2)))
                .andExpect(jsonPath("$.declaration.measures[0].name", is("z")))
                .andExpect(jsonPath("$.declaration.measures[1].position", is(2)));
    }

    @Test
    void contactIsWrittenOnTheCompanyAndAudited() throws Exception {
        long before = auditLogRepository.findAll().stream()
                .filter(l -> "Company".equals(l.getEntityType()) && tenantId.equals(l.getEntityId())).count();
        putTo("/contact", admin, "{\"fax\":\"021\",\"website\":\"exemplu.ro\",\"activitySector\":\"Comerț\","
                + "\"name\":\"Maria\",\"email\":\"maria@exemplu.ro\",\"phone\":\"0211\",\"mobile\":\"0722\","
                + "\"attestedOn\":\"2026-10-01\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contact.name", is("Maria")))
                .andExpect(jsonPath("$.contact.attestedOn", is("2026-10-01")));
        Company company = companyRepository.findById(tenantId).orElseThrow();
        assertThat(company.getEnergyContactEmail()).isEqualTo("maria@exemplu.ro");
        assertThat(company.getFax()).isEqualTo("021");
        long after = auditLogRepository.findAll().stream()
                .filter(l -> "Company".equals(l.getEntityType()) && tenantId.equals(l.getEntityId())).count();
        assertThat(after).isGreaterThan(before);
        putTo("/contact", admin, "{\"email\":\"nu-e-email\"}").andExpect(status().isUnprocessableEntity());
    }

    private static final byte[] PDF = "%PDF-1.4 test".getBytes();

    private ResultActions postReceipt(int year, String name, String type, byte[] bytes) throws Exception {
        return mockMvc.perform(multipart("/api/v1/energy/recipisa?year=" + year)
                .file(new MockMultipartFile("file", name, type, bytes))
                .header("Authorization", "Bearer " + admin));
    }

    private void stub(String publicId) {
        when(storage.upload(any(), anyString())).thenReturn(
                new StoredFile("u", publicId, "image", "authenticated", "pdf"));
    }

    @Test
    void aNullCarrierIs422() throws Exception {
        putTo("/carriers", admin, "{\"carriers\":[null]}").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aPdfReceiptIsStoredAndServed() throws Exception {
        stub("p1");
        when(storage.signedUrl("p1", "image", "authenticated", "pdf")).thenReturn("http://signed");
        when(storage.fetch("http://signed")).thenReturn(PDF);
        postReceipt(YEAR, "recipisa.pdf", "application/pdf", PDF).andExpect(status().isOk())
                .andExpect(jsonPath("$.receipt.fileName", is("recipisa.pdf")))
                .andExpect(jsonPath("$.receipt.sizeBytes", is(PDF.length)));
        verify(storage).upload(any(), eq("energy/" + tenantId + "/" + YEAR));
        sheet(YEAR).andExpect(jsonPath("$.receipt.fileName", is("recipisa.pdf")));
        mockMvc.perform(get("/api/v1/energy/recipisa/continut?year=" + YEAR)
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentType("application/pdf"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().bytes(PDF));
        mockMvc.perform(get("/api/v1/energy/recipisa/continut?year=" + (YEAR - 1))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void aNonPdfIs400AndKeepsTheOldReceipt() throws Exception {
        stub("p1");
        postReceipt(YEAR, "a.pdf", "application/pdf", PDF).andExpect(status().isOk());
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2};
        postReceipt(YEAR, "fals.pdf", "application/pdf", png).andExpect(status().isBadRequest());
        postReceipt(YEAR, "b.png", "image/png", PDF).andExpect(status().isBadRequest());
        verify(storage, never()).delete(any(), any(), any());
        sheet(YEAR).andExpect(jsonPath("$.receipt.fileName", is("a.pdf")));
    }

    @Test
    void replacingDeletesTheOldFileAfterTheUpload() throws Exception {
        stub("old");
        postReceipt(YEAR, "a.pdf", "application/pdf", PDF).andExpect(status().isOk());
        stub("new");
        postReceipt(YEAR, "b.pdf", "application/pdf", PDF).andExpect(status().isOk());
        InOrder order = inOrder(storage);
        order.verify(storage, org.mockito.Mockito.times(2)).upload(any(), anyString());
        order.verify(storage).delete("old", "image", "authenticated");
        sheet(YEAR).andExpect(jsonPath("$.receipt.fileName", is("b.pdf")));
    }

    @Test
    void elevenMegabytesIsRefused() throws Exception {
        byte[] big = new byte[11 * 1024 * 1024];
        System.arraycopy(PDF, 0, big, 0, 4);
        postReceipt(YEAR, "mare.pdf", "application/pdf", big).andExpect(status().isBadRequest());
        verify(storage, never()).upload(any(), anyString());
    }

    @Test
    void aClientViewerReadsButCannotWrite() throws Exception {
        mockMvc.perform(get("/api/v1/energy?year=" + YEAR).header("Authorization", "Bearer " + viewer))
                .andExpect(status().isOk());
        putTo("/carriers", viewer, "{\"carriers\":[]}").andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/v1/energy/recipisa?year=" + YEAR)
                        .file(new MockMultipartFile("file", "a.pdf", "application/pdf", PDF))
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(status().isForbidden());
        putTo("/consumption", viewer, "{\"year\":2025,\"carrier\":\"HEAT\",\"month\":1,\"quantity\":1}")
                .andExpect(status().isForbidden());
        putTo("/declaration", viewer, "{\"year\":2025}").andExpect(status().isForbidden());
        putTo("/contact", viewer, "{}").andExpect(status().isForbidden());
    }
}
