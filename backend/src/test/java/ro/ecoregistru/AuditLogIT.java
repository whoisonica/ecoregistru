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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.enums.CompanyType;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.repository.PartnerRepository;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.repository.WorkPointRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Jurnalul de audit — P1.11: cine, ce, când.
 *
 * <p>Proba nu e „se scrie un rând". Un jurnal greşit arată exact ca unul corect până în ziua în
 * care cineva îl deschide ca să răspundă la o întrebare de la un control — şi atunci e prea târziu
 * să afli că ştergerile se scriau ca „deleted: false → true", că partenerul apărea ca un şir de
 * treizeci şi şase de caractere, sau că o scriere respinsă lăsase totuşi urmă. Probele de mai jos
 * sunt scrise pe întrebările pe care le pune cineva, nu pe metodele care le răspund.
 *
 * <p>⚠️ Fiecare probă îşi caută <b>propriul</b> rând, după {@code entityId}. Clasa nu rulează în
 * tranzacţii care se dau înapoi, deci rândurile scrise de o probă rămân în bază pentru următoarea —
 * iar un jurnal e, prin definiţie, o tabelă care creşte sub tine.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class AuditLogIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AppUserRepository appUserRepository;
    @Autowired CompanyRepository companyRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;

    private String adminToken;
    private String operatorToken;
    private UUID workPointId;
    private UUID partnerId;
    private UUID otherPartnerId;

    @BeforeEach
    void setUp() {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        adminToken = jwtService.generateToken(admin);
        operatorToken = jwtService.generateToken(appUserRepository.findByEmail("operator@demo.ro").orElseThrow());
        UUID companyId = admin.getCompany().getId();
        workPointId = workPointRepository.findAllByCompany_Id(companyId).get(0).getId();
        var partners = partnerRepository.findAllByCompany_Id(companyId);
        partnerId = partners.stream().filter(p -> p.getName().equals("Transport Deșeuri SRL"))
                .findFirst().orElseThrow().getId();
        otherPartnerId = partners.stream().filter(p -> p.getName().equals("Colector Autorizat SA"))
                .findFirst().orElseThrow().getId();
    }

    /** Crearea se scrie cu autorul ei şi cu un nume pe care îl recunoşti fără să deschizi rândul. */
    @Test
    void creatingAMovementIsRecordedWithItsAuthor() throws Exception {
        UUID id = createMovement("2031-01-05", "5.000", null);

        JsonNode row = onlyEntryFor(id);
        assertThat(row.get("action").asText()).isEqualTo("CREATE");
        assertThat(row.get("entityType").asText()).isEqualTo("WasteMovement");
        assertThat(row.get("actorEmail").asText()).isEqualTo("admin@demo.ro");
        assertThat(row.get("actorRole").asText()).isEqualTo("ADMIN");
        assertThat(row.get("label").asText()).contains("2031-01-05");
        // La creare nu se scrie lista celor patruzeci de rubrici: „a creat mişcarea" e tot.
        assertThat(row.get("changes")).isEmpty();
    }

    /**
     * Întrebarea din care s-a născut felia: „cine a modificat cantitatea asta şi când".
     *
     * <p>Se verifică amândouă valorile, nu doar numele câmpului. O modificare care spune doar „s-a
     * atins cantitatea" nu ajută pe nimeni la un control: acolo se compară cu cifra de pe hârtia
     * depusă, deci trebuie să se vadă de la ce la ce.
     */
    @Test
    void changingTheQuantityNamesTheFieldAndBothValues() throws Exception {
        UUID id = createMovement("2031-02-05", "5.000", null);
        updateMovement(id, "2031-02-05", "7.500", null);

        JsonNode change = onlyChangeOf(latestEntryFor(id), "quantity");
        assertThat(change.get("from").asText()).startsWith("5.0");
        assertThat(change.get("to").asText()).startsWith("7.5");
        // `updatedAt` se schimbă la fiecare scriere; dacă ar intra, ar fi pe fiecare rând de jurnal.
        assertThat(fieldsOf(latestEntryFor(id))).containsExactly("quantity");
    }

    /**
     * Câmpurile care trimit la alt rând se citesc cu numele, nu cu identificatorul.
     *
     * <p>Jurnalul le scrie ca `uuid` — în mijlocul unui flush, numele nu se poate lua fără să
     * iniţializezi un proxy —, iar rezolvarea se face la citire, pe toată pagina deodată. Proba e
     * chiar aici, fiindcă fără ea felia ar fi „corectă" şi ilizibilă: nimeni nu ştie pe de rost cine
     * e `3f2a…`.
     */
    @Test
    void changingThePartnerShowsNamesNotIdentifiers() throws Exception {
        UUID id = createMovement("2031-03-05", "5.000", partnerId);
        updateMovement(id, "2031-03-05", "5.000", otherPartnerId);

        JsonNode change = onlyChangeOf(latestEntryFor(id), "partner");
        assertThat(change.get("from").asText()).isEqualTo("Transport Deșeuri SRL");
        assertThat(change.get("to").asText()).isEqualTo("Colector Autorizat SA");
    }

    /**
     * Ştergerea se scrie ca ştergere.
     *
     * <p>În baza de date e un UPDATE pe un boolean — ştergerea e moale peste tot, fiindcă mişcările
     * vechi citează rândurile. Scrisă aşa, ar fi fost adevărată şi de negăsit: nimeni nu caută
     * „deleted: false → true".
     */
    @Test
    void deletingIsWrittenAsADeletionNotAsABooleanFlip() throws Exception {
        UUID id = createMovement("2031-04-05", "5.000", null);
        mockMvc.perform(delete("/api/v1/movements/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        JsonNode row = latestEntryFor(id);
        assertThat(row.get("action").asText()).isEqualTo("DELETE");
        // Şi eticheta rămâne: rândul nu mai poate fi întrebat cum îl chema.
        assertThat(row.get("label").asText()).contains("2031-04-05");
    }

    /** Dezactivarea şi reactivarea sunt fapte proprii, nu „modificări". */
    @Test
    void deactivatingAndReactivatingAreTheirOwnVerbs() throws Exception {
        UUID id = createPartner("Audit Probă SRL");
        mockMvc.perform(delete("/api/v1/partners/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        assertThat(latestEntryFor(id).get("action").asText()).isEqualTo("DEACTIVATE");

        mockMvc.perform(post("/api/v1/partners/" + id + "/reactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        assertThat(latestEntryFor(id).get("action").asText()).isEqualTo("REACTIVATE");
    }

    /**
     * O scriere respinsă nu lasă urmă.
     *
     * <p>E proba pentru care rândurile se scriu <b>înainte</b> de commit, în aceeaşi tranzacţie:
     * o modificare revenită nu s-a întâmplat, iar un jurnal care ar scrie-o ar minţi în direcţia
     * cea mai proastă — ar arăta o faptă care nu e în date, şi ar arăta-o tocmai cui a venit să
     * verifice datele.
     */
    @Test
    void arefusedWriteLeavesNoTrace() throws Exception {
        UUID id = createMovement("2031-05-05", "5.000", null);
        int before = entriesFor(id).size();

        // RECOVERED fără cod R e refuzat de serviciu, după ce cantitatea a fost deja pusă pe entitate.
        mockMvc.perform(put("/api/v1/movements/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementBody("2031-05-05", "9.999", null, "\"operation\": \"RECOVERED\"")))
                .andExpect(status().isBadRequest());

        assertThat(entriesFor(id)).hasSize(before);
    }

    /**
     * Regenerarea evidenţei e un rând, nu o mie.
     *
     * <p>`MonthlyEvidence` lipseşte dinadins din lista albă a interceptorului: e un cache
     * recalculabil, iar un an regenerat ar scrie mii de intrări despre o apăsare de buton.
     */
    @Test
    void regeneratingTheEvidenceIsOneRowNotThousands() throws Exception {
        int before = entriesOfType("MonthlyEvidence").size();
        mockMvc.perform(post("/api/v1/evidences/regenerate")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("year", "2031"))
                .andExpect(status().isOk());

        List<JsonNode> rows = entriesOfType("MonthlyEvidence");
        assertThat(rows).hasSize(before + 1);
        assertThat(rows.get(0).get("action").asText()).isEqualTo("REGENERATE");
        assertThat(rows.get(0).get("label").asText()).contains("Anul 2031");
    }

    /** Rândurile numesc oameni, deci jurnalul e al administratorului. */
    @Test
    void theJournalIsForAdministrators() throws Exception {
        mockMvc.perform(get("/api/v1/audit-log").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/audit-log"))
                .andExpect(status().isUnauthorized());
    }

    /** Şi, ca orice altceva în aplicaţie, se opreşte la marginea firmei. */
    @Test
    void anotherCompanySeesNothingOfOurs() throws Exception {
        createMovement("2031-06-05", "5.000", null);

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company other = companyRepository.save(Company.builder()
                .name("Audit Tenant B SRL").cui("ROA" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser theirAdmin = appUserRepository.save(AppUser.builder()
                .email("admin+" + suffix + "@auditb.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(other).enabled(true).createdAt(Instant.now()).build());

        JsonNode page = objectMapper.readTree(mockMvc.perform(get("/api/v1/audit-log")
                        .header("Authorization", "Bearer " + jwtService.generateToken(theirAdmin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(page.get("totalElements").asLong()).isZero();
    }

    // ---------- helpers ----------

    private String movementBody(String date, String quantity, UUID partner, String extra) throws Exception {
        UUID wasteCodeId = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
        return """
                {
                  "workPointId": "%s",
                  "date": "%s",
                  "wasteCodeId": "%s",
                  "quantity": %s,
                  "unit": "KG",
                  "physicalState": "SOLID",
                  %s%s
                }
                """.formatted(workPointId, date, wasteCodeId, quantity,
                extra == null ? "\"operation\": \"GENERATED\"" : extra,
                partner == null ? "" : ",\n  \"partnerId\": \"" + partner + "\"");
    }

    private UUID createMovement(String date, String quantity, UUID partner) throws Exception {
        String created = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementBody(date, quantity, partner, null)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(created).get("id").asText());
    }

    private void updateMovement(UUID id, String date, String quantity, UUID partner) throws Exception {
        mockMvc.perform(put("/api/v1/movements/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementBody(date, quantity, partner, null)))
                .andExpect(status().isOk());
    }

    private UUID createPartner(String name) throws Exception {
        String created = mockMvc.perform(post("/api/v1/partners")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        // `client` nu e decor: serviciul cere un rol comercial (V7) — cine e
                        // partenerul pentru noi nu se poate deduce din tipul lui.
                        .content("""
                                { "name": "%s", "type": "COLLECTOR", "client": true }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(created).get("id").asText());
    }

    private List<JsonNode> entriesFor(UUID entityId) throws Exception {
        return rowsOf(get("/api/v1/audit-log").param("entityId", entityId.toString()));
    }

    private List<JsonNode> entriesOfType(String entityType) throws Exception {
        return rowsOf(get("/api/v1/audit-log").param("entityType", entityType));
    }

    /**
     * ⚠️ Citit pe <b>UTF-8</b> pe faţă. {@code getContentAsString()} fără argument decodează prin
     * Latin-1, iar „Transport Deşeuri SRL" iese „Transport DeÈ™euri SRL" — aici ar fi picat tocmai
     * proba care arată că jurnalul scrie nume, nu identificatori.
     */
    private List<JsonNode> rowsOf(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder r)
            throws Exception {
        JsonNode page = objectMapper.readTree(mockMvc.perform(r
                        .header("Authorization", "Bearer " + adminToken)
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        List<JsonNode> rows = new ArrayList<>();
        page.get("content").forEach(rows::add);
        return rows;
    }

    /** Cea mai nouă faptă despre rândul ăsta — lista vine deja cu cele noi întâi. */
    private JsonNode latestEntryFor(UUID entityId) throws Exception {
        List<JsonNode> rows = entriesFor(entityId);
        assertThat(rows).isNotEmpty();
        return rows.get(0);
    }

    private JsonNode onlyEntryFor(UUID entityId) throws Exception {
        List<JsonNode> rows = entriesFor(entityId);
        assertThat(rows).hasSize(1);
        return rows.get(0);
    }

    private List<String> fieldsOf(JsonNode row) {
        List<String> fields = new ArrayList<>();
        row.get("changes").forEach(change -> fields.add(change.get("field").asText()));
        return fields;
    }

    private JsonNode onlyChangeOf(JsonNode row, String field) {
        for (JsonNode change : row.get("changes")) {
            if (change.get("field").asText().equals(field)) {
                return change;
            }
        }
        throw new AssertionError("rândul de jurnal nu poartă câmpul „" + field + "”: " + row);
    }
}
