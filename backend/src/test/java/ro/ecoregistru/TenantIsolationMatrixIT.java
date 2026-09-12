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
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Izolarea tenantului pe toată matricea resursă × verb — auditul QA de dinaintea lansării.
 *
 * <p><b>De ce încă o clasă, când există {@code TenantIsolationIT}.</b> Aceea are patru teste și
 * acoperă o singură resursă — mişcările — şi un singur verb, {@code GET}. Aplicaţia are
 * şaisprezece controllere şi vreo şaptezeci de endpointuri. Codul <em>arată</em> corect la citire:
 * fiecare serviciu de domeniu cheamă {@code TenantContext.require()}, iar repository-urile poartă
 * tenantul în semnătură. Dar un comportament neprobat e un risc, nu o garanţie, iar proprietatea
 * asta e cea de care depinde tot produsul: dacă pică, un client vede datele altui client.
 *
 * <p><b>Ce se verifică, dincolo de codul de răspuns.</b> Un 404 care totuşi scrie e mai rău decât
 * un 200 cinstit, fiindcă nimeni nu-l caută. Aşa că fiecare scriere refuzată e urmată de o citire
 * din baza de date care arată că rândul celuilalt tenant a rămas exact cum era. Şi fiecare citire
 * refuzată se uită în corpul răspunsului după numele celuilalt, nu doar după status.
 *
 * <p><b>404, nu 403.</b> Peste tot, aşteptarea e „nu există", nu „nu ai voie": un 403 ar confirma
 * că identificatorul e valid, ceea ce e deja o scurgere — cineva poate enumera id-uri până
 * găseşte unul care răspunde altfel.
 *
 * <p><b>Cele două atacuri sunt diferite.</b> Pe citire şi ştergere, id-ul celuilalt tenant e în
 * cale. Pe scriere e mai subtil: calea e a mea, dar <em>corpul</em> cererii trimite spre o entitate
 * a celuilalt — punctul lui de lucru, partenerul lui, generatorul lui intern. Acolo nu e destul ca
 * mişcarea să se scrie în firma mea; trebuie ca cererea să fie refuzată, altfel o mişcare de-a mea
 * ar sta legată de un punct de lucru care nu e al meu şi ar apărea pe fişa lui.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class TenantIsolationMatrixIT {

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

    private Tenant a;
    private Tenant b;
    private UUID wasteCodeId;

    /** Tot ce are nevoie un tenant ca să fie atacat pe fiecare resursă. */
    private record Tenant(Company company, AppUser admin, String token, WorkPoint workPoint,
                          Partner partner, Driver driver, InternalGenerator generator,
                          WasteMovement movement, AnalysisBulletin bulletin) {}

    @BeforeEach
    void setUp() {
        WasteCode code = wasteCodeRepository.findAll().get(0);
        wasteCodeId = code.getId();
        // Numele sunt deliberat foarte diferite: o scurgere se vede în corpul răspunsului fără să
        // fie nevoie să ştim ce câmp a scăpat-o.
        a = buildTenant("Alfa Salubritate SRL", "PL Alfa", "Partener Alfa", "Sofer Alfa",
                "Generator Alfa", "Laborator Alfa", code);
        b = buildTenant("Beta Reciclare SRL", "PL Beta", "Partener Beta", "Sofer Beta",
                "Generator Beta", "Laborator Beta", code);
    }

    private Tenant buildTenant(String companyName, String workPointName, String partnerName,
                               String driverName, String generatorName, String laboratory,
                               WasteCode code) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name(companyName).cui("RO" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("admin+" + suffix + "@izolare.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        WorkPoint workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name(workPointName).active(true).createdAt(Instant.now()).build());
        Partner partner = partnerRepository.save(Partner.builder()
                .company(company).name(partnerName).client(true).supplier(false).carrier(false)
                .type(PartnerType.COLLECTOR).active(true).createdAt(Instant.now()).build());
        Driver driver = driverRepository.save(Driver.builder()
                .company(company).name(driverName).identification("AX" + suffix)
                .active(true).createdAt(Instant.now()).build());
        InternalGenerator generator = internalGeneratorRepository.save(InternalGenerator.builder()
                .company(company).workPoint(workPoint).name(generatorName)
                .active(true).createdAt(Instant.now()).build());
        WasteMovement movement = movementRepository.save(WasteMovement.builder()
                .company(company).workPoint(workPoint).date(LocalDate.now()).wasteCode(code)
                .quantity(new BigDecimal("42.000")).unit(Unit.KG).operation(WasteOperation.GENERATED)
                .deleted(false).createdBy(admin.getId()).build());
        AnalysisBulletin bulletin = analysisBulletinRepository.save(AnalysisBulletin.builder()
                .company(company).wasteCode(code).issueDate(LocalDate.now()).laboratory(laboratory)
                .url("https://res.cloudinary.com/x/authenticated/b.pdf")
                .publicId("ecoregistru/bulletins/" + suffix)
                .resourceType("image").deliveryType("authenticated").format("pdf")
                .fileName("buletin.pdf").contentType("application/pdf")
                .createdAt(Instant.now()).createdBy(admin.getId()).build());
        return new Tenant(company, admin, jwtService.generateToken(admin), workPoint, partner,
                driver, generator, movement, bulletin);
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder req, Tenant t) {
        return req.header("Authorization", "Bearer " + t.token());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Citirea unui rând al celuilalt, după id
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Aceeaşi întrebare pe fiecare resursă cu id în cale. Un singur test, fiindcă un eşec pe
     * oricare dintre ele e acelaşi defect şi are aceeaşi gravitate — iar aşa lista se poate
     * extinde cu o linie când apare o resursă nouă.
     *
     * <p>Lista e scurtă fiindcă aplicaţia n-are citire după id decât pe două resurse: partenerii,
     * punctele de lucru, şoferii şi generatorii interni se citesc numai prin listă. Pentru ele,
     * atacul pe id se probează mai jos, pe {@code PUT} şi {@code DELETE}.
     */
    @Test
    void readingAnotherTenantsRowByIdIsAlwaysANotFound() throws Exception {
        String[] urls = {
                "/api/v1/movements/" + b.movement().getId(),
                "/api/v1/analysis-bulletins/" + b.bulletin().getId() + "/continut",
        };
        for (String url : urls) {
            mockMvc.perform(as(get(url), a))
                    .andExpect(status().isNotFound());
        }
    }

    /**
     * Listele. Fiecare e citită cu tokenul lui A şi nu are voie să poarte nici numele, nici
     * identificatorul niciunui rând al lui B. Numele contează la fel de mult ca id-ul: un ecran
     * care afişează „Partener Beta" a scurs deja, chiar dacă id-ul lipseşte.
     */
    @Test
    void noListEverCarriesAnotherTenantsRows() throws Exception {
        String[] lists = {
                "/api/v1/movements",
                "/api/v1/partners",
                "/api/v1/work-points",
                "/api/v1/drivers",
                "/api/v1/internal-generators",
                "/api/v1/analysis-bulletins",
                "/api/v1/deadlines?year=" + LocalDate.now().getYear(),
                "/api/v1/audit-log",
        };
        for (String url : lists) {
            mockMvc.perform(as(get(url), a))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString(b.movement().getId().toString()))))
                    .andExpect(content().string(not(containsString(b.partner().getId().toString()))))
                    .andExpect(content().string(not(containsString("Beta"))));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scrierea: calea e a mea, corpul trimite la entităţile celuilalt
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Atacul cel mai util pentru un atacator, fiindcă nu cere niciun id în cale: îmi creez o
     * mişcare în firma mea, dar o leg de punctul de lucru al lui B. Dacă trece, mişcarea mea intră
     * în evidenţa lui — şi apare pe fişa lui de gestiune, care e un document oficial.
     */
    @Test
    void aMovementCannotBeAttachedToAnotherTenantsWorkPoint() throws Exception {
        long before = movementRepository.findAllByCompany_IdAndDeletedFalse(a.company().getId()).size();

        mockMvc.perform(as(post("/api/v1/movements"), a)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(b.workPoint().getId(), null, null)))
                .andExpect(status().isNotFound());

        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(a.company().getId()))
                .as("cererea refuzată nu are voie să lase un rând în urmă")
                .hasSize((int) before);
    }

    /** Acelaşi atac, prin partener: destinatarul tipărit pe Anexa 3. */
    @Test
    void aMovementCannotPointAtAnotherTenantsPartner() throws Exception {
        mockMvc.perform(as(post("/api/v1/movements"), a)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(a.workPoint().getId(), b.partner().getId(), null)))
                .andExpect(status().isNotFound());
    }

    /** Şi prin generatorul intern, care pe fişă spune „unde s-a născut deşeul". */
    @Test
    void aMovementCannotPointAtAnotherTenantsInternalGenerator() throws Exception {
        mockMvc.perform(as(post("/api/v1/movements"), a)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(a.workPoint().getId(), null, b.generator().getId())))
                .andExpect(status().isNotFound());
    }

    /** Un generator intern nu se poate agăţa de punctul de lucru al altcuiva. */
    @Test
    void anInternalGeneratorCannotBeCreatedOnAnotherTenantsWorkPoint() throws Exception {
        mockMvc.perform(as(post("/api/v1/internal-generators"), a)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workPointId":"%s","name":"Furat"}
                                """.formatted(b.workPoint().getId())))
                .andExpect(status().isNotFound());

        assertThat(internalGeneratorRepository.findAllByCompany_IdOrderByNameAsc(b.company().getId()))
                .as("nimic nou nu are voie să apară în firma lui B")
                .hasSize(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Modificarea şi ştergerea rândurilor celuilalt
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Modificarea. Contează la fel de mult ca citirea şi e mai uşor de trecut cu vederea: o
     * scurgere se vede, o rescriere tăcută nu. După fiecare refuz se citeşte rândul lui B din baza
     * de date şi se verifică valoarea, nu doar existenţa.
     */
    @Test
    void updatingAnotherTenantsRowsChangesNothing() throws Exception {
        mockMvc.perform(as(put("/api/v1/partners/" + b.partner().getId()), a)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rescris de Alfa","client":true,"supplier":false,"carrier":false,"type":"COLLECTOR"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(as(put("/api/v1/work-points/" + b.workPoint().getId()), a)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rescris de Alfa"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(as(put("/api/v1/movements/" + b.movement().getId()), a)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movementJson(a.workPoint().getId(), null, null)))
                .andExpect(status().isNotFound());

        assertThat(partnerRepository.findById(b.partner().getId()).orElseThrow().getName())
                .isEqualTo("Partener Beta");
        assertThat(workPointRepository.findById(b.workPoint().getId()).orElseThrow().getName())
                .isEqualTo("PL Beta");
        WasteMovement untouched = movementRepository.findById(b.movement().getId()).orElseThrow();
        assertThat(untouched.getQuantity()).isEqualByComparingTo("42.000");
        assertThat(untouched.getWorkPoint().getId())
                .as("mişcarea lui B nu are voie să fie mutată în punctul de lucru al lui A")
                .isEqualTo(b.workPoint().getId());
    }

    /**
     * Ştergerea. Toate resursele astea folosesc dezactivarea, nu ştergerea fizică — deci proba e
     * că rândul lui B a rămas <b>activ</b>, nu doar că a rămas.
     */
    @Test
    void deletingAnotherTenantsRowsChangesNothing() throws Exception {
        String[] urls = {
                "/api/v1/partners/" + b.partner().getId(),
                "/api/v1/work-points/" + b.workPoint().getId(),
                "/api/v1/drivers/" + b.driver().getId(),
                "/api/v1/internal-generators/" + b.generator().getId(),
                "/api/v1/movements/" + b.movement().getId(),
                "/api/v1/analysis-bulletins/" + b.bulletin().getId(),
        };
        for (String url : urls) {
            mockMvc.perform(as(delete(url), a)).andExpect(status().isNotFound());
        }

        assertThat(partnerRepository.findById(b.partner().getId()).orElseThrow().isActive()).isTrue();
        assertThat(workPointRepository.findById(b.workPoint().getId()).orElseThrow().isActive()).isTrue();
        assertThat(driverRepository.findById(b.driver().getId()).orElseThrow().isActive()).isTrue();
        assertThat(internalGeneratorRepository.findById(b.generator().getId()).orElseThrow().isActive()).isTrue();
        assertThat(movementRepository.findById(b.movement().getId()).orElseThrow().isDeleted()).isFalse();
        assertThat(analysisBulletinRepository.findById(b.bulletin().getId())).isPresent();
    }

    /**
     * Reactivarea — celălalt capăt al dezactivării, şi uşor de uitat fiindcă e un {@code POST} pe
     * o cale care seamănă cu o resursă. Dacă ar trece, ar fi o scriere în firma altuia.
     */
    @Test
    void reactivatingAnotherTenantsRowsIsRefused() throws Exception {
        String[] urls = {
                "/api/v1/partners/" + b.partner().getId() + "/reactivate",
                "/api/v1/work-points/" + b.workPoint().getId() + "/reactivate",
                "/api/v1/drivers/" + b.driver().getId() + "/reactivate",
                "/api/v1/internal-generators/" + b.generator().getId() + "/reactivate",
        };
        for (String url : urls) {
            mockMvc.perform(as(post(url), a)).andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Documentele şi fişierele — o scurgere aici e completă, nu parţială
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Documentele oficiale generate pe mişcarea altcuiva. Un PDF sau un XLS nu scurge un câmp, ci
     * tot: denumirea firmei, codul deşeului, cantităţile, partenerul, şoferul.
     */
    @Test
    void officialDocumentsCannotBeGeneratedOnAnotherTenantsMovement() throws Exception {
        String[] urls = {
                "/api/v1/movements/" + b.movement().getId() + "/anexa3",
                "/api/v1/movements/" + b.movement().getId() + "/anexa2",
                "/api/v1/movements/" + b.movement().getId() + "/anexa2/prag",
        };
        for (String url : urls) {
            mockMvc.perform(as(get(url), a))
                    .andExpect(status().isNotFound());
        }
    }

    /**
     * Urcarea unui fişier pe mişcarea altcuiva. Se verifică şi că storage-ul <b>nu</b> e chemat:
     * un refuz dat după upload ar lăsa fişierul urcat la furnizor, plătit şi orfan.
     */
    @Test
    void aFileCannotBeUploadedOntoAnotherTenantsMovement() throws Exception {
        var file = new MockMultipartFile("file", "aviz.pdf", "application/pdf", "x".getBytes());

        mockMvc.perform(as(multipart("/api/v1/movements/" + b.movement().getId() + "/attachments")
                        .file(file), a))
                .andExpect(status().isNotFound());

        verify(storageService, never()).upload(any(), anyString());
    }

    /**
     * Dosarul de control. Nu poartă id în cale — iese din {@code TenantContext} —, deci proba nu e
     * un cod de răspuns, ci <b>conţinutul</b>: un zip întreg cu evidenţa, autorizaţiile
     * partenerilor şi buletinele. Dacă tenantul s-ar amesteca aici, ar fi cea mai completă
     * scurgere posibilă din aplicaţie, şi ar pleca direct spre un inspector.
     *
     * <p>Se despachetează şi se citeşte {@code README.txt}, singura intrare în text simplu:
     * într-un PDF textul e comprimat, deci o căutare de şir pe octeţii bruţi ar trece şi când
     * numele chiar e acolo.
     */
    @Test
    void theAuditDossierCarriesOnlyItsOwnTenant() throws Exception {
        byte[] zip = mockMvc.perform(as(get("/api/v1/audit-file")
                        .param("year", String.valueOf(LocalDate.now().getYear())), a))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        StringBuilder readme = new StringBuilder();
        try (var in = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zip))) {
            java.util.zip.ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                assertThat(entry.getName())
                        .as("nici măcar numele unei intrări nu poate numi alt tenant")
                        .doesNotContain("Beta");
                if (entry.getName().endsWith("README.txt")) {
                    readme.append(new String(in.readAllBytes()));
                }
            }
        }

        assertThat(readme.toString())
                .as("dosarul lui A se numeşte pe el şi numai pe el")
                .contains("Alfa")
                .doesNotContain("Beta");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilizatorii — cea mai scumpă scriere din aplicaţie
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Un ADMIN al lui A peste utilizatorii lui B. Dacă ar trece schimbarea de rol, ar fi
     * escaladare completă: îşi face un cont în firma altuia şi îl ridică.
     */
    @Test
    void anAdminCannotTouchAnotherTenantsUsers() throws Exception {
        mockMvc.perform(as(put("/api/v1/users/" + b.admin().getId() + "/role"), a)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"CLIENT_VIEWER"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(as(delete("/api/v1/users/" + b.admin().getId()), a))
                .andExpect(status().isNotFound());

        AppUser untouched = appUserRepository.findById(b.admin().getId()).orElseThrow();
        assertThat(untouched.getRole()).isEqualTo(Role.ADMIN);
        assertThat(untouched.isEnabled()).isTrue();
        assertThat(untouched.getDeactivatedAt()).isNull();
    }

    /** Iar lista de utilizatori a lui A nu are voie să-l poarte pe administratorul lui B. */
    @Test
    void theUserListStopsAtTheTenantBoundary() throws Exception {
        mockMvc.perform(as(get("/api/v1/users"), a))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(a.admin().getEmail())))
                .andExpect(content().string(not(containsString(b.admin().getEmail()))));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String movementJson(UUID workPointId, UUID partnerId, UUID generatorId) {
        return """
                {"workPointId":"%s","date":"%s","wasteCodeId":"%s","quantity":1.000,
                 "unit":"KG","operation":"GENERATED","physicalState":"SOLID"%s%s}
                """.formatted(workPointId, LocalDate.now(), wasteCodeId,
                partnerId == null ? "" : ",\"partnerId\":\"" + partnerId + "\"",
                generatorId == null ? "" : ",\"internalGeneratorId\":\"" + generatorId + "\"");
    }
}
