package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WorkPointRepository;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.service.Anexa3RegisterService;
import ro.ecoregistru.service.export.Anexa3Register;
import ro.ecoregistru.security.TenantContext;

import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ro.ecoregistru.Golden.flat;
import static ro.ecoregistru.Golden.pdfText;

/**
 * Registrul Anexa 3 (transport) — centralizatorul formularelor de transport emise (Andreea, 29.09.2026:
 * „număr, data, seria nr doc, cantitate, cod deșeu, denumire deșeu, către cine s-a predat și codul de
 * valorificare — un document care centralizează anexele 3 transport”).
 *
 * <p>Regulile probate: intră numai predările cărora li s-a alocat un număr de Anexa 3 (o predare
 * netipărită nu e un formular emis), în ordinea numerelor; cantitatea e în kg oricum a fost scrisă
 * mișcarea și rămâne goală la „se cântărește la descărcare”; anul fără formulare tipărește nota lui, nu
 * cade; documentul se serveşte ca PDF şi refuză punctul de lucru al altei firme.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class Anexa3RegisterIT {

    private static final int YEAR = 2026;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired Anexa3RegisterService service;

    private String token;
    private UUID tenantId;
    private UUID workPointId;
    private UUID wasteCodeId;
    private UUID partnerId;
    private String partnerName;
    private String seriesBefore;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        token = jwtService.generateToken(admin);
        tenantId = admin.getCompany().getId();
        workPointId = workPointRepository.findAllByCompany_Id(tenantId).get(0).getId();
        wasteCodeId = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
        var partner = partnerRepository.findAllByCompany_Id(tenantId).get(0);
        partnerId = partner.getId();
        partnerName = partner.getName();
        Company company = companyRepository.findById(tenantId).orElseThrow();
        seriesBefore = company.getAnexa3Series();
        company.setAnexa3Series("RTS");
        companyRepository.save(company);
    }

    @AfterEach
    void tearDown() {
        Company company = companyRepository.findById(tenantId).orElseThrow();
        company.setAnexa3Series(seriesBefore);
        companyRepository.save(company);
        TenantContext.clear();
    }

    /**
     * Trei predări, două tipărite: registrul are exact două rânduri, în ordinea numerelor alocate, cu
     * seria firmei, data, codul, denumirea, partenerul şi codul R. A treia, netipărită, nu e un formular
     * emis şi nu apare.
     */
    @Test
    void onlyHandoversWithAnAllocatedNumberAreListedInNumberOrder() throws Exception {
        UUID first = handover("\"quantity\": 100");
        UUID second = handover("\"quantity\": 200");
        handover("\"quantity\": 333");
        print(first);
        print(second);
        int firstNumber = number(first);
        int secondNumber = number(second);
        assertThat(secondNumber).isGreaterThan(firstNumber);

        TenantContext.set(tenantId);
        Anexa3Register register = service.build(YEAR, null);
        // Contul demo are deja formulare tipărite de seeder: se probează ordinea şi numerotarea, nu lista întreagă.
        assertThat(register.rows()).extracting(Anexa3Register.Row::number).containsSubsequence(firstNumber, secondNumber);
        assertThat(register.rows()).extracting(Anexa3Register.Row::number).doesNotHaveDuplicates();
        assertThat(register.rows()).extracting(Anexa3Register.Row::position)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, register.rows().size()).boxed().toList());
        Anexa3Register.Row row = rowOf(register, firstNumber);
        assertThat(row.series()).isEqualTo("RTS");
        assertThat(row.kg()).isEqualByComparingTo("100");
        assertThat(row.wasteCode()).isEqualTo("20 01 01");
        assertThat(row.wasteName()).isNotBlank();
        assertThat(row.recipient()).isEqualTo(partnerName);
        assertThat(row.operationCode()).isEqualTo("R13");

        String text = flat(pdfText(service.render(YEAR, null)));
        assertThat(text).contains(flat("Registrul Anexa 3 (transport)"))
                .contains(flat("RTS " + firstNumber)).contains(flat("RTS " + secondNumber))
                .contains(flat("05.07." + YEAR)).contains(flat("20 01 01")).contains(flat(partnerName))
                .contains("R13");
        // Predarea netipărită (333 kg) lipsește din rânduri; nu se caută „333” în textul PDF-ului, fiindcă
        // CUI-urile de probă sunt aleatoare și îl pot conține (vezi AuditFileIT, 29.09.2026).
        assertThat(register.rows()).extracting(Anexa3Register.Row::kg)
                .noneMatch(kg -> kg != null && kg.compareTo(new java.math.BigDecimal("333")) == 0);
        assertThat(text.indexOf(flat("RTS " + firstNumber))).isLessThan(text.indexOf(flat("RTS " + secondNumber)));
    }

    /** Predarea cântărită de destinatar n-are cifră: rândul există, cantitatea rămâne goală. */
    @Test
    void aLoadWeighedByTheRecipientKeepsAnEmptyQuantity() throws Exception {
        UUID id = handover("\"weighedAtUnloading\": true, \"volumeM3\": 2.0");
        print(id);

        TenantContext.set(tenantId);
        Anexa3Register.Row row = rowOf(service.build(YEAR, null), number(id));
        assertThat(row.kg()).isNull();
        assertThat(pdfText(service.render(YEAR, null))).isNotBlank();
    }

    /** Scrisă în tone, mişcarea iese în kg pe registru — ca pe toate ecranele generatorului. */
    @Test
    void tonsArePrintedAsKilograms() throws Exception {
        UUID id = handover("\"quantity\": 1.5", "TONS");
        print(id);

        TenantContext.set(tenantId);
        assertThat(rowOf(service.build(YEAR, null), number(id)).kg()).isEqualByComparingTo("1500");
    }

    /** Anul fără nicio Anexa 3 tipărită: antetul şi nota, nu o eroare. */
    @Test
    void aYearWithoutFormsPrintsTheEmptyNote() throws Exception {
        TenantContext.set(tenantId);
        String text = flat(pdfText(service.render(2031, null)));
        assertThat(text).contains(flat("Niciun formular emis în 2031"));
    }

    /** Documentul e servit ca PDF de descărcat, iar punctul de lucru al altei firme e 404. */
    @Test
    void theRegisterIsServedAsPdfAndAForeignWorkPointIs404() throws Exception {
        mockMvc.perform(get("/api/v1/evidences/registru-anexa3")
                        .param("year", String.valueOf(YEAR))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", containsString("registru-anexa3-" + YEAR + ".pdf")));

        Company other = companyRepository.save(Company.builder()
                .name("Altă firmă SRL").cui("RO" + UUID.randomUUID().toString().substring(0, 8))
                .type(ro.ecoregistru.enums.CompanyType.GENERATOR).active(true).afmObligation(false)
                .createdAt(java.time.Instant.now()).build());
        UUID foreign = workPointRepository.save(ro.ecoregistru.entity.WorkPoint.builder()
                .company(other).name("Punct străin").active(true).createdAt(java.time.Instant.now()).build()).getId();
        mockMvc.perform(get("/api/v1/evidences/registru-anexa3")
                        .param("year", String.valueOf(YEAR))
                        .param("workPointId", foreign.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private UUID handover(String extraJson) throws Exception {
        return handover(extraJson, "KG");
    }

    private UUID handover(String extraJson, String unit) throws Exception {
        String body = """
                {
                  "workPointId": "%s", "date": "%d-07-05", "wasteCodeId": "%s", "unit": "%s",
                  "operation": "RECOVERED", "register": "ANEXA_1", "physicalState": "SOLID", "storageType": "CT",
                  "transportMeans": "AN", "packagingCategory": "SECONDARY", "wasteDestination": "Vr", "operationCode": "R13",
                  "partnerId": "%s", %s
                }
                """.formatted(workPointId, YEAR, wasteCodeId, unit, partnerId, extraJson);
        String response = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(response, "$.id"));
    }

    /** Tipărirea alocă numărul: de-abia de acum predarea e un formular emis. */
    private void print(UUID id) throws Exception {
        mockMvc.perform(get("/api/v1/movements/" + id + "/anexa3")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private int number(UUID id) {
        return movementRepository.findById(id).orElseThrow().getAnexa3Number();
    }

    private static Anexa3Register.Row rowOf(Anexa3Register register, int number) {
        return register.rows().stream().filter(r -> r.number() == number).findFirst().orElseThrow();
    }
}
