package ro.ecoregistru;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantFilter;
import ro.ecoregistru.service.EmailService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P2.13, felia 1 — contul de consultant: un cabinet care vede firmele lui şi numai pe ele.
 *
 * <p><b>Ce e nou în graniţă.</b> Până acum antetul {@code X-Tenant-Id} era citit pentru un singur
 * rol, al nostru, care are voie peste tot. Consultantul e primul rol care trimite antetul <em>şi</em>
 * poate trimite unul greşit. Toată felia stă pe o singură întrebare din {@code TenantFilter} — „firma
 * asta e a cabinetului tău?" —, deci clasa asta o pune din fiecare parte pe care un consultant o are
 * la îndemână: antetul, id-ul din cale, CUI-ul la creare, rolul la invitaţie, colegii.
 *
 * <p><b>Scena.</b> Două cabinete. Primul (consultanţii Ana şi Andrei) gestionează „Xenon" şi „Yoda";
 * al doilea (Bogdan) gestionează „Zulu". Numele sunt alese să nu apară unul în altul, ca o scurgere să
 * se vadă în corpul răspunsului fără să ştim ce câmp a dus-o.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class ConsultantAccessIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired ConsultancyRepository consultancyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired PartnerRepository partnerRepository;

    @MockBean EmailService emailService;

    private Consultancy first;
    private Consultancy second;
    private Company xenon;
    private Company yoda;
    private Company zulu;
    private WorkPoint zuluWorkPoint;
    private AppUser ana;
    private AppUser andrei;
    private AppUser bogdan;
    private String anaToken;
    private String xenonAdminToken;
    private String platformToken;

    @BeforeEach
    void setUp() {
        first = consultancy("Cabinet Unu");
        second = consultancy("Cabinet Doi");
        xenon = company("Xenon SRL", "Partener Xenon", first);
        yoda = company("Yoda SRL", "Partener Yoda", first);
        zulu = company("Zulu SRL", "Partener Zulu", second);
        zuluWorkPoint = workPointRepository.findAll().stream()
                .filter(wp -> wp.getCompany().getId().equals(zulu.getId())).findFirst().orElseThrow();

        ana = consultant("ana", first);
        andrei = consultant("andrei", first);
        bogdan = consultant("bogdan", second);
        anaToken = jwtService.generateToken(ana);

        AppUser xenonAdmin = appUserRepository.save(AppUser.builder()
                .email("admin+" + suffix() + "@xenon.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(xenon).enabled(true).createdAt(Instant.now()).build());
        xenonAdminToken = jwtService.generateToken(xenonAdmin);
        platformToken = jwtService.generateToken(
                appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Antetul
    // ─────────────────────────────────────────────────────────────────────────

    /** Xenon → Yoda → Xenon: tenantul se recalculează la fiecare cerere, nu doar la prima. */
    @Test
    void aConsultantSwitchesBetweenTheirOwnCompanies() throws Exception {
        seesOnly(xenon, "Xenon");
        seesOnly(yoda, "Yoda");
        seesOnly(xenon, "Xenon");
    }

    private void seesOnly(Company tenant, String mine) throws Exception {
        mockMvc.perform(as(get("/api/v1/partners"), anaToken, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Partener " + mine)))
                .andExpect(content().string(not(containsString(mine.equals("Xenon") ? "Yoda" : "Xenon"))))
                .andExpect(content().string(not(containsString("Zulu"))));
    }

    /**
     * <b>Cel mai important test din clasă.</b> Firma altui cabinet trebuie să primească exact
     * răspunsul unui id care nu există nicăieri — altfel antetul devine un oracol: un consultant ar
     * putea încerca id-uri până află care sunt reale.
     */
    @Test
    void anotherConsultancysCompanyAnswersExactlyLikeACompanyThatDoesNotExist() throws Exception {
        MvcResult foreign = mockMvc.perform(as(get("/api/v1/partners"), anaToken, zulu.getId()))
                .andExpect(content().string(not(containsString("Zulu"))))
                .andReturn();
        MvcResult unknown = mockMvc.perform(as(get("/api/v1/partners"), anaToken, UUID.randomUUID()))
                .andReturn();

        assertThat(foreign.getResponse().getStatus()).isEqualTo(400);
        assertThat(foreign.getResponse().getStatus()).isEqualTo(unknown.getResponse().getStatus());
        assertThat(errorCode(foreign)).isEqualTo("tenant.required").isEqualTo(errorCode(unknown));
    }

    @Test
    void withoutTheHeaderAConsultantHasNoTenant() throws Exception {
        mockMvc.perform(get("/api/v1/partners").header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("tenant.required")));
    }

    /**
     * O scriere refuzată se verifică în bază, nu doar prin cod: un refuz care totuşi scrie e mai rău
     * decât un 200 cinstit. Două drumuri: antetul străin, şi antetul propriu cu id-ul străin în cale.
     */
    @Test
    void aWriteAimedAtAnotherConsultancysCompanyChangesNothing() throws Exception {
        String path = "/api/v1/work-points/" + zuluWorkPoint.getId();

        mockMvc.perform(as(delete(path), anaToken, zulu.getId()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(delete(path), anaToken, xenon.getId()))
                .andExpect(status().isNotFound());

        assertThat(workPointRepository.findById(zuluWorkPoint.getId()).orElseThrow().isActive())
                .as("punctul de lucru al lui Zulu a rămas activ").isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Directorul de firme
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void theCompanyListIsThePortfolioAndNothingElse() throws Exception {
        mockMvc.perform(get("/api/v1/companies").header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(content().string(containsString("Xenon SRL")))
                .andExpect(content().string(containsString("Yoda SRL")))
                .andExpect(content().string(not(containsString("Zulu"))));
    }

    /** O firmă creată de consultant intră în cabinetul lui — altfel n-ar putea s-o mai aleagă. */
    @Test
    void aCompanyCreatedByAConsultantJoinsTheirConsultancyAndIsSelectable() throws Exception {
        String body = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + anaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("Wolfram SRL", digitsCui())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultancyId", is(first.getId().toString())))
                .andExpect(jsonPath("$.consultancyName", is("Cabinet Unu")))
                .andReturn().getResponse().getContentAsString();
        UUID created = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        mockMvc.perform(as(get("/api/v1/work-points"), anaToken, created))
                .andExpect(status().isOk());
        mockMvc.perform(as(get("/api/v1/work-points"), jwtService.generateToken(bogdan), created))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherConsultancysCompanyIsNotFoundByIdAndStaysUntouched() throws Exception {
        mockMvc.perform(put("/api/v1/companies/" + zulu.getId())
                        .header("Authorization", "Bearer " + anaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("Furat SRL", zulu.getCui())))
                .andExpect(status().isNotFound());

        String email = "strecurat+" + suffix() + "@zulu.ro";
        mockMvc.perform(post("/api/v1/companies/" + zulu.getId() + "/users")
                        .header("Authorization", "Bearer " + anaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "role", "ADMIN"))))
                .andExpect(status().isNotFound());

        assertThat(companyRepository.findById(zulu.getId()).orElseThrow().getName()).isEqualTo("Zulu SRL");
        assertThat(appUserRepository.findByEmail(email)).as("niciun cont strecurat în Zulu").isEmpty();
    }

    /**
     * CUI-ul e unic pe toată aplicaţia, deci refuzul în sine nu se poate evita. Ce se poate evita e ca
     * mesajul să spună mai mult decât refuzul: nici numele firmei, nici că e „a altcuiva".
     */
    @Test
    void aTakenCuiIsRefusedWithoutSayingWhoseItIs() throws Exception {
        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + anaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("Alta SRL", zulu.getCui())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("company.cui.unavailable")))
                .andExpect(content().string(not(containsString("Zulu"))));

        // Controlul: platforma primeşte în continuare mesajul direct.
        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("Alta SRL", zulu.getCui())))
                .andExpect(jsonPath("$['error-code']", is("company.cui.exists")));
    }

    /** Un cont de consultant e al unui cabinet; nicio uşă de firmă nu-l poate crea sau acorda. */
    @Test
    void theConsultantRoleCannotBeGrantedFromInsideACompany() throws Exception {
        mockMvc.perform(as(post("/api/v1/users"), anaToken, xenon.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "c1+" + suffix() + "@x.ro", "role", "CONSULTANT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("invite.role.invalid")));
        mockMvc.perform(post("/api/v1/companies/" + xenon.getId() + "/users")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "c2+" + suffix() + "@x.ro", "role", "CONSULTANT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("invite.role.invalid")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Echipa cabinetului
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void theTeamIsTheConsultancysOwnConsultants() throws Exception {
        mockMvc.perform(get("/api/v1/consultancy/users").header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(andrei.getEmail())))
                .andExpect(content().string(not(containsString(bogdan.getEmail()))));

        mockMvc.perform(get("/api/v1/consultancy/users").header("Authorization", "Bearer " + xenonAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void aConsultantInvitesAColleagueIntoTheirOwnConsultancy() throws Exception {
        String email = "coleg+" + suffix() + "@unu.ro";
        mockMvc.perform(post("/api/v1/consultancy/users")
                        .header("Authorization", "Bearer " + anaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("CONSULTANT")))
                .andExpect(jsonPath("$.status", is("PENDING_INVITE")));

        AppUser invited = appUserRepository.findByEmail(email).orElseThrow();
        assertThat(invited.getConsultancy().getId()).isEqualTo(first.getId());
        assertThat(invited.getCompany()).isNull();
    }

    /**
     * Omul care pleacă din cabinet: sesiunea lui cade la cererea următoare. Colegul altui cabinet nu
     * se poate atinge, iar pe sine nu se dezactivează nimeni.
     */
    @Test
    void deactivatingAColleagueEndsTheirSessionAndStopsAtTheConsultancy() throws Exception {
        String andreiToken = jwtService.generateToken(andrei);

        mockMvc.perform(delete("/api/v1/consultancy/users/" + bogdan.getId())
                        .header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isNotFound());
        assertThat(appUserRepository.findById(bogdan.getId()).orElseThrow().isEnabled()).isTrue();

        mockMvc.perform(delete("/api/v1/consultancy/users/" + ana.getId())
                        .header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']", is("user.cannot.manage.self")));

        mockMvc.perform(delete("/api/v1/consultancy/users/" + andrei.getId())
                        .header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/companies").header("Authorization", "Bearer " + andreiToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/consultancy/users/" + andrei.getId() + "/reactivate")
                        .header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isNoContent());
        // Reactivarea dă înapoi contul, nu sesiunile de dinainte.
        mockMvc.perform(get("/api/v1/companies").header("Authorization", "Bearer " + andreiToken))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Platforma
    // ─────────────────────────────────────────────────────────────────────────

    /** Mutarea unei firme între cabinete e a noastră: schimbă cine îi citeşte evidenţa. */
    @Test
    void onlyThePlatformAdminMovesACompanyBetweenConsultancies() throws Exception {
        String toFirst = json(Map.of("consultancyId", first.getId().toString()));

        mockMvc.perform(put("/api/v1/companies/" + zulu.getId() + "/consultancy")
                        .header("Authorization", "Bearer " + anaToken)
                        .contentType(MediaType.APPLICATION_JSON).content(toFirst))
                .andExpect(status().isForbidden());
        assertThat(companyRepository.findById(zulu.getId()).orElseThrow().getConsultancy().getId())
                .isEqualTo(second.getId());

        mockMvc.perform(put("/api/v1/companies/" + zulu.getId() + "/consultancy")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON).content(toFirst))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultancyName", is("Cabinet Unu")));
        mockMvc.perform(as(get("/api/v1/partners"), anaToken, zulu.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Partener Zulu")));

        Map<String, Object> direct = new HashMap<>();
        direct.put("consultancyId", null);
        mockMvc.perform(put("/api/v1/companies/" + zulu.getId() + "/consultancy")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON).content(json(direct)))
                .andExpect(status().isOk());
        mockMvc.perform(as(get("/api/v1/partners"), anaToken, zulu.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void thePlatformAdminCreatesAConsultancyAndInvitesItsFirstConsultant() throws Exception {
        String body = mockMvc.perform(post("/api/v1/consultancies")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Cabinet Trei", "cui", digitsCui()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyCount", is(0)))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();

        String email = "primul+" + suffix() + "@trei.ro";
        mockMvc.perform(post("/api/v1/consultancies/" + id + "/users")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("CONSULTANT")));
        assertThat(appUserRepository.findByEmail(email).orElseThrow().getConsultancy().getId().toString())
                .isEqualTo(id);

        for (String token : new String[]{anaToken, xenonAdminToken}) {
            mockMvc.perform(get("/api/v1/consultancies").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    /** Scanarea din 17.09.2026: același cabinet cu și fără „RO” nu se creează de două ori. */
    @Test
    void theSameConsultancyCuiWithOrWithoutRoIsADuplicate() throws Exception {
        String cui = digitsCui();
        mockMvc.perform(post("/api/v1/consultancies")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Cabinet Patru", "cui", cui))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/consultancies")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Cabinet Patru bis", "cui", cui.substring(2)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$['error-code']", is("consultancy.cui.exists")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Baza de date
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Perechea rol ↔ proprietar e ţinută de V40, nu doar de servicii: un rând care le amestecă ar fi
     * citit de {@code TenantFilter} altfel decât crede cine l-a scris.
     */
    @Test
    void theDatabaseRefusesAnAccountThatBelongsToBothOrToTheWrongOwner() {
        assertThatThrownBy(() -> appUserRepository.saveAndFlush(AppUser.builder()
                .email("amestec+" + suffix() + "@x.ro").password("x").role(Role.CONSULTANT)
                .company(xenon).consultancy(first).enabled(true).createdAt(Instant.now()).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> appUserRepository.saveAndFlush(AppUser.builder()
                .email("admincabinet+" + suffix() + "@x.ro").password("x").role(Role.ADMIN)
                .company(xenon).consultancy(first).enabled(true).createdAt(Instant.now()).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> appUserRepository.saveAndFlush(AppUser.builder()
                .email("fara+" + suffix() + "@x.ro").password("x").role(Role.CONSULTANT)
                .enabled(true).createdAt(Instant.now()).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Jurnalul de audit
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Jurnalul scria orice faptă pe <b>firma selectată</b>. Un consultant care stă pe Xenon şi creează
     * firma Wolfram lăsa „Creare · Wolfram SRL" în jurnalul lui Xenon — citit de adminul lui Xenon,
     * care n-are ce şti despre alţi clienţi ai cabinetului. La fel un coleg invitat din cabinet.
     *
     * <p>O firmă se scrie în jurnalul ei; un cont de cabinet nu e al niciunei firme.
     */
    @Test
    void factsAboutACompanyOrAConsultantNeverLandInTheSelectedCompanysJournal() throws Exception {
        String body = mockMvc.perform(as(post("/api/v1/companies"), anaToken, xenon.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("Wolfram SRL", digitsCui())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID wolfram = UUID.fromString(objectMapper.readTree(body).get("id").asText());

        String colleague = "jurnal+" + suffix() + "@unu.ro";
        mockMvc.perform(as(post("/api/v1/consultancy/users"), anaToken, xenon.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", colleague))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/audit-log").header("Authorization", "Bearer " + xenonAdminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Wolfram"))))
                .andExpect(content().string(not(containsString(colleague))));

        mockMvc.perform(as(get("/api/v1/audit-log"), anaToken, wolfram))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Wolfram SRL")));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder req, String token, UUID tenant) {
        return req.header("Authorization", "Bearer " + token)
                .header(TenantFilter.TENANT_HEADER, tenant.toString());
    }

    private Consultancy consultancy(String name) {
        return consultancyRepository.save(Consultancy.builder()
                .name(name).cui(digitsCui()).createdAt(Instant.now()).build());
    }

    private Company company(String name, String partnerName, Consultancy consultancy) {
        Company company = companyRepository.save(Company.builder()
                .name(name).cui(digitsCui()).type(CompanyType.GENERATOR).consultancy(consultancy)
                .active(true).createdAt(Instant.now()).build());
        workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL " + name).active(true).createdAt(Instant.now()).build());
        partnerRepository.save(Partner.builder()
                .company(company).name(partnerName).client(true).supplier(false).carrier(false)
                .type(PartnerType.COLLECTOR).active(true).createdAt(Instant.now()).build());
        return company;
    }

    private AppUser consultant(String name, Consultancy consultancy) {
        return appUserRepository.save(AppUser.builder()
                .email(name + "+" + suffix() + "@cabinet.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.CONSULTANT).consultancy(consultancy)
                .enabled(true).createdAt(Instant.now()).build());
    }

    private String companyBody(String name, String cui) throws Exception {
        return json(Map.of("name", name, "cui", cui, "type", "GENERATOR", "afmObligation", false));
    }

    private String json(Map<String, ?> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private String errorCode(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("error-code").asText();
    }

    /** CUI-ul trece prin validarea de format la creare, deci numai cifre. */
    private static String digitsCui() {
        return "RO" + TestCui.random();
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
