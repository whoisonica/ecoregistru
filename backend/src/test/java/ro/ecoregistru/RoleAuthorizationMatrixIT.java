package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.service.CloudinaryStorageService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Autorizarea pe roluri, pe toată matricea endpoint × rol — P0.3 din planul de audit.
 *
 * <p><b>De ce separat de izolarea tenantului.</b> {@code TenantIsolationMatrixIT} răspunde la
 * „poate A să atingă datele lui B". Asta răspunde la cealaltă întrebare, care e la fel de
 * fierbinte pentru un client: <em>înăuntrul aceleiaşi firme</em>, poate cineva să facă mai mult
 * decât are voie. Cele două riscuri arată la fel din afară — date schimbate de cine nu trebuia —
 * dar au cauze diferite şi se strică separat.
 *
 * <p><b>Aplicaţia are patru roluri şi trei praguri.</b> Pragurile sunt şiruri de caractere
 * duplicate în treisprezece controllere, fiecare cu propria constantă ({@code CAN_WRITE},
 * {@code CAN_MANAGE}, {@code PLATFORM_ONLY}) — nu există un loc unic unde regula se citeşte, deci
 * nici un loc unic unde se poate verifica prin citire. Un endpoint nou scris fără adnotare nu
 * strică nimic vizibil şi nu cade niciun test existent: pur şi simplu e deschis. De-asta proba se
 * face endpoint cu endpoint, prin stiva HTTP reală.
 *
 * <p><b>403, nu 404 — şi de ce diferă de clasa vecină.</b> Aici resursa e a firmei tale şi id-ul
 * e real; nu există nimic de ascuns prin „nu există". Răspunsul corect e „nu ai voie", iar un 404
 * ar fi de fapt o minciună care ar trimite clientul în suport.
 *
 * <p><b>Ce se verifică dincolo de cod.</b> Trei lucruri pe care un test de status singur nu le
 * vede: (1) după fiecare scriere refuzată se citeşte rândul din baza de date şi se compară
 * <em>valoarea</em>; (2) un rol slab trebuie să poată în continuare <em>citi</em> — altfel proba
 * de mai sus ar trece şi pe o aplicaţie stricată, care refuză tot; (3) autorizarea se ia din rândul
 * utilizatorului, nu din claim-ul tokenului — probat cu un token emis înainte de retrogradare.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class RoleAuthorizationMatrixIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired DriverRepository driverRepository;
    @Autowired InternalGeneratorRepository internalGeneratorRepository;
    @Autowired AnalysisBulletinRepository analysisBulletinRepository;

    @MockBean CloudinaryStorageService storageService;

    /** O cerere gata de trimis, cu eticheta ei — ca eşecul să spună care endpoint a cedat. */
    private record Call(String label, Supplier<MockHttpServletRequestBuilder> request) {}

    private Company company;
    private AppUser admin;
    private AppUser operator;
    private AppUser viewer;
    private String adminToken;
    private String operatorToken;
    private String viewerToken;
    private String platformToken;

    private WorkPoint workPoint;
    private Partner partner;
    private Driver driver;
    private InternalGenerator generator;
    private WasteMovement movement;
    private AnalysisBulletin bulletin;
    private UUID wasteCodeId;

    /** Toţi trei în <b>aceeaşi</b> firmă: aici nu se probează graniţa dintre firme, ci cea dintre roluri. */
    @BeforeEach
    void setUp() {
        WasteCode code = wasteCodeRepository.findAll().get(0);
        wasteCodeId = code.getId();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        company = companyRepository.save(Company.builder()
                .name("Roluri SRL").cui("RO" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        admin = user("admin+" + suffix + "@roluri.ro", Role.ADMIN);
        operator = user("operator+" + suffix + "@roluri.ro", Role.OPERATOR);
        viewer = user("viewer+" + suffix + "@roluri.ro", Role.CLIENT_VIEWER);
        adminToken = jwtService.generateToken(admin);
        operatorToken = jwtService.generateToken(operator);
        viewerToken = jwtService.generateToken(viewer);
        platformToken = jwtService.generateToken(
                appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow());

        workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Roluri").active(true).createdAt(Instant.now()).build());
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Partener Roluri").client(true).supplier(false).carrier(false)
                .type(PartnerType.COLLECTOR).active(true).createdAt(Instant.now()).build());
        driver = driverRepository.save(Driver.builder()
                .company(company).name("Sofer Roluri").identification("AX" + suffix)
                .active(true).createdAt(Instant.now()).build());
        generator = internalGeneratorRepository.save(InternalGenerator.builder()
                .company(company).workPoint(workPoint).name("Generator Roluri")
                .active(true).createdAt(Instant.now()).build());
        movement = movementRepository.save(WasteMovement.builder()
                .company(company).workPoint(workPoint).date(LocalDate.now()).wasteCode(code)
                .quantity(new BigDecimal("42.000")).unit(Unit.KG).operation(WasteOperation.GENERATED)
                .deleted(false).createdBy(admin.getId()).build());
        bulletin = analysisBulletinRepository.save(AnalysisBulletin.builder()
                .company(company).wasteCode(code).issueDate(LocalDate.now()).laboratory("Laborator Roluri")
                .url("https://res.cloudinary.com/x/authenticated/b.pdf")
                .publicId("ecoregistru/bulletins/" + suffix)
                .resourceType("image").deliveryType("authenticated").format("pdf")
                .fileName("buletin.pdf").contentType("application/pdf")
                .createdAt(Instant.now()).createdBy(admin.getId()).build());
    }

    private AppUser user(String email, Role role) {
        return appUserRepository.save(AppUser.builder()
                .email(email).password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(role).company(company).enabled(true).createdAt(Instant.now()).build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CAN_WRITE — scrierea, refuzată celui care doar se uită
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fiecare endpoint {@code CAN_WRITE} din aplicaţie, cerut cu un {@code CLIENT_VIEWER}.
     *
     * <p>Un singur test pentru douăzeci şi şase de endpointuri, fiindcă un eşec pe oricare e
     * acelaşi defect şi are aceeaşi gravitate — iar lista se extinde cu o linie când apare un
     * endpoint nou, ceea ce e exact ce trebuie să se întâmple.
     *
     * <p><b>Corpurile sunt valide</b>, deliberat. Un corp stricat ar fi respins la 422 de
     * validare, înainte să se ajungă la autorizare, iar testul ar trece verde fără să fi probat
     * nimic. Aşa, singurul lucru care stă între cerere şi scriere e rolul.
     */
    @Test
    void aViewerIsRefusedOnEveryWriteEndpoint() throws Exception {
        refusedFor("CLIENT_VIEWER", viewerToken, writeEndpoints());
    }

    /**
     * Perechea obligatorie a testului de mai sus: după toate refuzurile, <b>nimic nu s-a
     * schimbat</b>. Un 403 care totuşi scrie ar fi mai rău decât un 200 cinstit, fiindcă nimeni
     * nu se uită după el.
     *
     * <p>Se compară valorile, nu existenţa: numele partenerului, cantitatea mişcării, steagurile
     * de activ. Şi numărul de rânduri, pentru cererile de creare — o scriere scăpată ar lăsa un
     * rând în plus fără să atingă niciunul dintre cele vechi.
     */
    @Test
    void nothingTheViewerAskedForWasWritten() throws Exception {
        int movementsBefore = movementRepository.findAllByCompany_IdAndDeletedFalse(company.getId()).size();
        int partnersBefore = partnerRepository.findAllByCompany_Id(company.getId()).size();
        int workPointsBefore = workPointRepository.findAllByCompany_Id(company.getId()).size();
        int driversBefore = driverRepository.findAllByCompany_IdOrderByNameAsc(company.getId()).size();
        int generatorsBefore = internalGeneratorRepository.findAllByCompany_IdOrderByNameAsc(company.getId()).size();

        refusedFor("CLIENT_VIEWER", viewerToken, writeEndpoints());

        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(company.getId()))
                .as("nicio mişcare creată şi niciuna ştearsă").hasSize(movementsBefore);
        assertThat(partnerRepository.findAllByCompany_Id(company.getId())).hasSize(partnersBefore);
        assertThat(workPointRepository.findAllByCompany_Id(company.getId())).hasSize(workPointsBefore);
        assertThat(driverRepository.findAllByCompany_IdOrderByNameAsc(company.getId())).hasSize(driversBefore);
        assertThat(internalGeneratorRepository.findAllByCompany_IdOrderByNameAsc(company.getId()))
                .hasSize(generatorsBefore);

        WasteMovement untouched = movementRepository.findById(movement.getId()).orElseThrow();
        assertThat(untouched.getQuantity()).as("cantitatea, nu doar rândul").isEqualByComparingTo("42.000");
        assertThat(untouched.isDeleted()).isFalse();
        assertThat(partnerRepository.findById(partner.getId()).orElseThrow().getName())
                .isEqualTo("Partener Roluri");
        assertThat(partnerRepository.findById(partner.getId()).orElseThrow().isActive()).isTrue();
        assertThat(driverRepository.findById(driver.getId()).orElseThrow().isActive()).isTrue();
        assertThat(internalGeneratorRepository.findById(generator.getId()).orElseThrow().isActive()).isTrue();
        assertThat(analysisBulletinRepository.findById(bulletin.getId())).isPresent();

        // Fişierele nici măcar nu pleacă spre furnizorul de stocare: refuzul e înainte de serviciu,
        // nu o curăţenie după. Altfel ar rămâne obiecte plătite şi orfane la fiecare încercare.
        verify(storageService, never()).upload(any(), anyString());
    }

    /**
     * Controlul negativ, şi partea cea mai importantă a clasei. Exact aceeaşi cerere, acelaşi
     * corp, singura diferenţă e tokenul — şi trece.
     *
     * <p>Fără el, toate testele de mai sus ar fi la fel de verzi pe o aplicaţie care refuză tot
     * (un filtru stricat, o adnotare pusă pe clasă din greşeală), iar raportul ar spune „autorizare
     * corectă" despre un produs inutilizabil.
     */
    @Test
    void theSameRequestSucceedsForAnOperator() throws Exception {
        int before = movementRepository.findAllByCompany_IdAndDeletedFalse(company.getId()).size();

        int status = statusOf(post("/api/v1/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(movementJson()), operatorToken);

        assertThat(status).as("OPERATOR pe POST /api/v1/movements — corpul e acelaşi ca la viewer")
                .isBetween(200, 299);
        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(company.getId()))
                .hasSize(before + 1);
    }

    /**
     * Cealaltă jumătate a controlului: un {@code CLIENT_VIEWER} trebuie să <b>poată citi</b>. Rolul
     * există fiindcă cineva — contabilul, consultantul de mediu, inspectorul invitat — are nevoie
     * să vadă evidenţa fără să o poată atinge. Dacă refuzurile de mai sus s-ar extinde peste
     * citire, rolul n-ar mai avea niciun rost, iar clientul ar da unui contabil cont de OPERATOR.
     */
    @Test
    void aViewerCanStillReadEverythingTheRoleExistsFor() throws Exception {
        String[] reads = {
                "/api/v1/movements",
                "/api/v1/movements/" + movement.getId(),
                "/api/v1/partners",
                "/api/v1/work-points",
                "/api/v1/drivers",
                "/api/v1/internal-generators",
                "/api/v1/analysis-bulletins",
                "/api/v1/waste-codes?q=15",
                "/api/v1/companies/current",
                "/api/v1/deadlines?year=" + LocalDate.now().getYear(),
                "/api/v1/evidences?year=" + LocalDate.now().getYear(),
                "/api/v1/packaging/market?year=" + LocalDate.now().getYear(),
                "/api/v1/packaging/table1?year=" + LocalDate.now().getYear(),
        };
        for (String url : reads) {
            assertThat(statusOf(get(url), viewerToken)).as("CLIENT_VIEWER pe GET %s", url).isEqualTo(200);
        }
    }

    /**
     * Distincţia dintre cele două formulare de transport, pironită fiindcă arată ca o scăpare şi
     * nu e: {@code GET /{id}/anexa3} e singurul GET din aplicaţie gatuit pe {@code CAN_WRITE},
     * pentru că generarea <em>alocă numărul formularului</em> — adică scrie. Anexa 2 nu alocă
     * nimic (numărul ei îl dă agenţia de mediu), deci e o citire ca oricare alta.
     *
     * <p>Dacă cineva „uniformizează" cândva cele două adnotări, testul ăsta spune care e motivul.
     */
    @Test
    void printingAnexa3IsAWriteButPrintingAnexa2IsNot() throws Exception {
        assertThat(statusOf(get("/api/v1/movements/" + movement.getId() + "/anexa3"), viewerToken))
                .as("anexa 3 alocă numărul formularului — e scriere").isEqualTo(403);
        assertThat(statusOf(get("/api/v1/movements/" + movement.getId() + "/anexa2"), viewerToken))
                .as("anexa 2 nu alocă nimic — e citire").isNotEqualTo(403);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CAN_MANAGE — administrarea firmei
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * {@code CAN_MANAGE} e pragul de deasupra scrierii: utilizatorii, punctele de lucru şi
     * jurnalul de audit. Se probează cu <b>amândouă</b> rolurile de dedesubt, fiindcă sunt două
     * praguri diferite şi o adnotare greşită ar putea să lase să treacă numai unul.
     *
     * <p>Cel mai greu dintre ele e {@code PUT /users/{id}/role}: cu el, un OPERATOR care ar
     * ajunge dincolo şi-ar putea da singur ADMIN, şi de-acolo firma întreagă.
     */
    @Test
    void neitherOperatorNorViewerMayManageTheFirm() throws Exception {
        refusedFor("OPERATOR", operatorToken, manageEndpoints());
        refusedFor("CLIENT_VIEWER", viewerToken, manageEndpoints());

        AppUser adminAfter = appUserRepository.findById(admin.getId()).orElseThrow();
        assertThat(adminAfter.getRole()).as("rolul adminului, neatins").isEqualTo(Role.ADMIN);
        assertThat(adminAfter.isEnabled()).as("adminul, nedezactivat").isTrue();
        assertThat(appUserRepository.findById(operator.getId()).orElseThrow().getRole())
                .as("operatorul nu s-a promovat singur").isEqualTo(Role.OPERATOR);
        assertThat(appUserRepository.findById(viewer.getId()).orElseThrow().getRole())
                .isEqualTo(Role.CLIENT_VIEWER);
        assertThat(workPointRepository.findById(workPoint.getId()).orElseThrow().getName())
                .isEqualTo("PL Roluri");
        assertThat(workPointRepository.findById(workPoint.getId()).orElseThrow().isActive()).isTrue();
    }

    /**
     * Controlul negativ al pragului de administrare, şi nu e o formalitate: fără el, un endpoint
     * gatuit din greşeală pe {@code PLATFORM_ONLY} în loc de {@code CAN_MANAGE} ar lăsa testul de
     * mai sus verde — operatorul şi vizualizatorul tot 403 ar primi — în timp ce adminul firmei nu
     * şi-ar mai putea administra oamenii, iar produsul ar fi rupt exact pentru cine plăteşte.
     */
    @Test
    void theCompanyAdminGoesThroughOnTheSameEndpoints() throws Exception {
        assertThat(statusOf(get("/api/v1/users"), adminToken)).isEqualTo(200);
        assertThat(statusOf(get("/api/v1/audit-log"), adminToken)).isEqualTo(200);
        assertThat(statusOf(post("/api/v1/work-points")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"PL Deschis de admin"}"""), adminToken)).isBetween(200, 299);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PLATFORM_ONLY — ce ţine de platformă, nu de firmă
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Endpointurile platformei, cerute de fiecare rol din interiorul unei firme — inclusiv de
     * {@code ADMIN}, care înăuntrul firmei lui poate tot.
     *
     * <p>Miza: {@code GET /companies} întoarce <b>lista tuturor clienţilor</b>, cu CUI şi date de
     * contact. Nu e o scurgere de rânduri dintr-o firmă în alta, e catalogul complet — iar
     * {@code POST /companies/{id}/users} e mai rău: cine ajunge acolo îşi bagă un cont în firma
     * oricui. Verificarea lui {@code TenantContext} nu le acoperă, fiindcă aici firma e
     * <em>parametru</em>, nu context; singurul lucru care le ţine e adnotarea.
     */
    @Test
    void nobodyInsideACompanyReachesThePlatformEndpoints() throws Exception {
        int companiesBefore = companyRepository.findAll().size();

        refusedFor("ADMIN", adminToken, platformEndpoints());
        refusedFor("OPERATOR", operatorToken, platformEndpoints());
        refusedFor("CLIENT_VIEWER", viewerToken, platformEndpoints());

        assertThat(companyRepository.findAll())
                .as("nicio firmă creată de cine n-avea voie").hasSize(companiesBefore);
        assertThat(appUserRepository.findAllByCompany_Id(company.getId()))
                .as("niciun cont strecurat în firmă").hasSize(3);
    }

    /** Controlul negativ al pragului de platformă: aceleaşi citiri, cu tokenul care are voie. */
    @Test
    void thePlatformAdminGoesThrough() throws Exception {
        assertThat(statusOf(get("/api/v1/companies"), platformToken)).isEqualTo(200);
        assertThat(statusOf(get("/api/v1/account-requests"), platformToken)).isEqualTo(200);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Escaladare verticală
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Escaladarea pe sine, pe drumul cel mai scurt: adminul unei firme cere
     * {@code PUT /users/{propriul id}/role} cu {@code PLATFORM_ADMIN}. Are voie pe endpoint —
     * {@code CAN_MANAGE} îl lasă să intre — deci adnotarea nu-l opreşte. Ce-l opreşte e regula din
     * serviciu, şi asta se probează aici.
     *
     * <p>Sunt <b>două</b> opriri, nu una, şi ambele contează fiindcă fiecare acoperă o gaură a
     * celeilalte: rolul {@code PLATFORM_ADMIN} e refuzat la intrare oricui, iar acţiunea asupra
     * propriului cont e refuzată indiferent de rolul cerut. Proba se uită la rândul din baza de
     * date, fiindcă numai el decide ce se poate la următoarea cerere.
     */
    @Test
    void anAdminCannotPromoteThemselves() throws Exception {
        int status = statusOf(put("/api/v1/users/" + admin.getId() + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"role":"PLATFORM_ADMIN"}"""), adminToken);

        assertThat(status).as("refuzat, nu 2xx").isBetween(400, 499);
        assertThat(appUserRepository.findById(admin.getId()).orElseThrow().getRole())
                .as("rolul din baza de date e singurul care contează").isEqualTo(Role.ADMIN);
    }

    /**
     * Acelaşi atac pe ocolite: adminul promovează un coleg la {@code PLATFORM_ADMIN}, urmând să
     * folosească acel cont. Refuzat la intrare, înainte de orice altă regulă.
     */
    @Test
    void anAdminCannotMintAPlatformAdminEither() throws Exception {
        int status = statusOf(put("/api/v1/users/" + operator.getId() + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"role":"PLATFORM_ADMIN"}"""), adminToken);

        assertThat(status).isBetween(400, 499);
        assertThat(appUserRepository.findById(operator.getId()).orElseThrow().getRole())
                .isEqualTo(Role.OPERATOR);
    }

    /**
     * Contul de platformă nu e nici măcar <em>vizibil</em> din interiorul unei firme: rândul lui
     * are compania {@code null}, iar toate căutările din {@code CompanyUserService} sunt legate de
     * firma sesiunii. Deci nu e o verificare care se poate uita — e o imposibilitate structurală.
     *
     * <p>Aşteptat 404, nu 403: din firmă, id-ul ăla chiar nu există.
     */
    @Test
    void aPlatformAdminAccountCannotBeTouchedFromInsideACompany() throws Exception {
        AppUser platform = appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow();

        assertThat(statusOf(put("/api/v1/users/" + platform.getId() + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"role":"CLIENT_VIEWER"}"""), adminToken)).isEqualTo(404);
        assertThat(statusOf(delete("/api/v1/users/" + platform.getId()), adminToken)).isEqualTo(404);

        AppUser after = appUserRepository.findById(platform.getId()).orElseThrow();
        assertThat(after.getRole()).isEqualTo(Role.PLATFORM_ADMIN);
        assertThat(after.isEnabled()).isTrue();
    }

    /**
     * Rolul călătoreşte în token ca un claim, dar autorizarea citeşte rândul utilizatorului la
     * fiecare cerere ({@code JwtAuthenticationFilter} → {@code UserDetailsService} → coloana
     * {@code role}). Proba: un token emis cât omul era OPERATOR, folosit <b>după</b> retrogradarea
     * lui la {@code CLIENT_VIEWER}.
     *
     * <p>De ce contează: retrogradarea nu invalidează tokenul — nici nu bumpează
     * {@code tokenVersion}, pe bună dreptate, fiindcă omul rămâne angajat şi trebuie să-şi vadă
     * ecranele. Dacă rolul s-ar lua din claim, retrogradarea ar fi o decizie care intră în vigoare
     * peste cel mult treizeci de zile, adică deloc. Testul e şi asigurarea că nimeni nu
     * „optimizează" cândva citirea rândului, punând rolul din token.
     */
    @Test
    void theRoleIsReadFromTheRowNotFromTheToken() throws Exception {
        String tokenIssuedWhileOperator = jwtService.generateToken(operator);
        assertThat(statusOf(post("/api/v1/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(movementJson()), tokenIssuedWhileOperator))
                .as("înainte de retrogradare").isBetween(200, 299);

        operator.setRole(Role.CLIENT_VIEWER);
        appUserRepository.save(operator);

        assertThat(statusOf(post("/api/v1/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(movementJson()), tokenIssuedWhileOperator))
                .as("acelaşi token, rol nou în baza de date").isEqualTo(403);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Listele de endpointuri şi ajutoarele
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Toate endpointurile {@code CAN_WRITE} din aplicaţie, în ordinea controllerelor. Id-urile
     * sunt reale acolo unde o scriere scăpată ar strica ceva real; unde e vorba de rânduri care
     * n-au legătură cu proba (un termen, un ataşament) e destul un UUID oarecare, fiindcă
     * autorizarea se decide înainte ca serviciul să caute rândul.
     */
    private List<Call> writeEndpoints() {
        int year = LocalDate.now().getYear();
        UUID any = UUID.randomUUID();
        return List.of(
                new Call("POST /movements", () -> post("/api/v1/movements")
                        .contentType(MediaType.APPLICATION_JSON).content(movementJson())),
                new Call("PUT /movements/{id}", () -> put("/api/v1/movements/" + movement.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(movementJson())),
                new Call("POST /movements/{id}/weight", () -> post("/api/v1/movements/" + movement.getId() + "/weight")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"quantity":7.5,"unit":"KG"}""")),
                new Call("DELETE /movements/{id}", () -> delete("/api/v1/movements/" + movement.getId())),
                new Call("GET /movements/{id}/anexa3", () -> get("/api/v1/movements/" + movement.getId() + "/anexa3")),
                new Call("POST /movements/{id}/attachments", () -> multipart("/api/v1/movements/" + movement.getId() + "/attachments")
                        .file(new MockMultipartFile("file", "aviz.pdf", "application/pdf", "%PDF-1.4".getBytes()))),
                new Call("DELETE /movements/{id}/attachments/{attId}",
                        () -> delete("/api/v1/movements/" + movement.getId() + "/attachments/" + any)),

                new Call("POST /partners", () -> post("/api/v1/partners")
                        .contentType(MediaType.APPLICATION_JSON).content(partnerJson("Partener Nou"))),
                new Call("PUT /partners/{id}", () -> put("/api/v1/partners/" + partner.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(partnerJson("Redenumit de viewer"))),
                new Call("DELETE /partners/{id}", () -> delete("/api/v1/partners/" + partner.getId())),
                new Call("POST /partners/{id}/reactivate", () -> post("/api/v1/partners/" + partner.getId() + "/reactivate")),

                new Call("POST /drivers", () -> post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Sofer Nou","identification":"XX999"}""")),
                new Call("PUT /drivers/{id}", () -> put("/api/v1/drivers/" + driver.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Redenumit de viewer","identification":"XX999"}""")),
                new Call("DELETE /drivers/{id}", () -> delete("/api/v1/drivers/" + driver.getId())),
                new Call("POST /drivers/{id}/reactivate", () -> post("/api/v1/drivers/" + driver.getId() + "/reactivate")),

                new Call("POST /internal-generators", () -> post("/api/v1/internal-generators")
                        .contentType(MediaType.APPLICATION_JSON).content(generatorJson("Generator Nou"))),
                new Call("PUT /internal-generators/{id}", () -> put("/api/v1/internal-generators/" + generator.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(generatorJson("Redenumit de viewer"))),
                new Call("DELETE /internal-generators/{id}", () -> delete("/api/v1/internal-generators/" + generator.getId())),
                new Call("POST /internal-generators/{id}/reactivate",
                        () -> post("/api/v1/internal-generators/" + generator.getId() + "/reactivate")),

                new Call("POST /analysis-bulletins", () -> multipart("/api/v1/analysis-bulletins")
                        .file(new MockMultipartFile("file", "buletin.pdf", "application/pdf", "%PDF-1.4".getBytes()))
                        .param("wasteCodeId", wasteCodeId.toString())
                        .param("issueDate", LocalDate.now().toString())
                        .param("laboratory", "Laborator Nou")),
                new Call("DELETE /analysis-bulletins/{id}", () -> delete("/api/v1/analysis-bulletins/" + bulletin.getId())),

                new Call("POST /deadlines/regenerate", () -> post("/api/v1/deadlines/regenerate").param("year", String.valueOf(year))),
                new Call("POST /deadlines/{id}/complete", () -> post("/api/v1/deadlines/" + any + "/complete")),
                new Call("POST /deadlines/{id}/reopen", () -> post("/api/v1/deadlines/" + any + "/reopen")),

                new Call("POST /evidences/regenerate", () -> post("/api/v1/evidences/regenerate").param("year", String.valueOf(year))),

                new Call("PUT /packaging/market", () -> put("/api/v1/packaging/market")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"material":"PET","year":%d,"primaryTotal":10}""".formatted(year)))
        );
    }

    /** Endpointurile {@code CAN_MANAGE}, plus jurnalul de audit, care e {@code CAN_READ} — acelaşi prag. */
    private List<Call> manageEndpoints() {
        UUID any = UUID.randomUUID();
        return List.of(
                new Call("POST /work-points", () -> post("/api/v1/work-points")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"PL Nou"}""")),
                new Call("PUT /work-points/{id}", () -> put("/api/v1/work-points/" + workPoint.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Redenumit fara drept"}""")),
                new Call("DELETE /work-points/{id}", () -> delete("/api/v1/work-points/" + workPoint.getId())),
                new Call("POST /work-points/{id}/reactivate", () -> post("/api/v1/work-points/" + workPoint.getId() + "/reactivate")),

                new Call("GET /users", () -> get("/api/v1/users")),
                new Call("POST /users", () -> post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"email":"strecurat@roluri.ro","role":"ADMIN"}""")),
                new Call("POST /users/{id}/resend-invite", () -> post("/api/v1/users/" + any + "/resend-invite")),
                new Call("PUT /users/{id}/role — pe sine", () -> put("/api/v1/users/" + operator.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"role":"ADMIN"}""")),
                new Call("PUT /users/{id}/role — pe admin", () -> put("/api/v1/users/" + admin.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"role":"CLIENT_VIEWER"}""")),
                new Call("DELETE /users/{id}", () -> delete("/api/v1/users/" + admin.getId())),
                new Call("DELETE /users/{id}/invitation", () -> delete("/api/v1/users/" + any + "/invitation")),
                new Call("POST /users/{id}/reactivate", () -> post("/api/v1/users/" + any + "/reactivate")),

                new Call("GET /audit-log", () -> get("/api/v1/audit-log"))
        );
    }

    /** Endpointurile {@code PLATFORM_ONLY}. */
    private List<Call> platformEndpoints() {
        UUID any = UUID.randomUUID();
        return List.of(
                new Call("GET /companies", () -> get("/api/v1/companies")),
                new Call("POST /companies", () -> post("/api/v1/companies")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Firma Strecurata SRL","cui":"RO99999999","type":"GENERATOR"}""")),
                new Call("PUT /companies/{id}", () -> put("/api/v1/companies/" + company.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"Redenumita","cui":"RO99999998","type":"BOTH"}""")),
                new Call("POST /companies/{id}/users", () -> post("/api/v1/companies/" + company.getId() + "/users")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"email":"strecurat2@roluri.ro","role":"ADMIN"}""")),

                new Call("GET /account-requests", () -> get("/api/v1/account-requests")),
                new Call("POST /account-requests/{id}/approve", () -> post("/api/v1/account-requests/" + any + "/approve")),
                new Call("POST /account-requests/{id}/reject", () -> post("/api/v1/account-requests/" + any + "/reject"))
        );
    }

    private void refusedFor(String role, String token, List<Call> calls) throws Exception {
        for (Call call : calls) {
            assertThat(statusOf(call.request().get(), token))
                    .as("%s pe %s trebuie refuzat cu 403", role, call.label())
                    .isEqualTo(403);
        }
    }

    private int statusOf(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
    }

    private String movementJson() {
        return """
                {"workPointId":"%s","date":"%s","wasteCodeId":"%s","quantity":1.000,
                 "unit":"KG","operation":"GENERATED","physicalState":"SOLID"}
                """.formatted(workPoint.getId(), LocalDate.now(), wasteCodeId);
    }

    private String partnerJson(String name) {
        return """
                {"name":"%s","client":true,"supplier":false,"carrier":false,"type":"COLLECTOR"}
                """.formatted(name);
    }

    private String generatorJson(String name) {
        return """
                {"workPointId":"%s","name":"%s"}
                """.formatted(workPoint.getId(), name);
    }
}
