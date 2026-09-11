package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.service.CloudinaryStorageService;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * G-7 — buletinele de analiză, legate de <b>cod</b>, nu de mișcare.
 *
 * <p>OUG 92/2021 art. 8 alin. (4) cere „o caracterizare a deșeurilor periculoase generate din
 * propria activitate", iar scopurile pe care le enumeră sunt proprietăți ale tipului de deșeu —
 * deci cheia e (firmă, cod). Art. 48 alin. (2) cere ca buletinele să fie <b>deținute</b>. Până pe
 * 11.09.2026 se putea atașa orice fișier la o mișcare, dar nimic nu lega un buletin de un cod și
 * nimic nu semnala absența lui: la un client cu coduri periculoase dosarul era incomplet legal
 * fără să se vadă, iar README-ul lui recunoștea asta în propriile cuvinte.
 *
 * <p>{@link CloudinaryStorageService} e mockuit din același motiv ca în {@code AttachmentAccessIT}:
 * seam-ul există ca suita să probeze regulile fără cont și fără rețea.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class AnalysisBulletinIT {

    private static final byte[] FILE_BYTES = "buletin de analiză".getBytes(StandardCharsets.UTF_8);

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired AnalysisBulletinRepository bulletinRepository;

    @MockBean CloudinaryStorageService storageService;

    private String adminToken;
    private String otherTenantToken;
    private AppUser admin;
    private WasteCode hazardous;

    @BeforeEach
    void setUp() throws Exception {
        when(storageService.upload(any(), anyString())).thenReturn(
                new CloudinaryStorageService.StoredFile(
                        "https://res.cloudinary.com/x/image/authenticated/s--sig--/v1/b.pdf",
                        "ecoregistru/bulletins/b", "image", "authenticated", "pdf"));
        when(storageService.signedUrl(anyString(), any(), any(), any()))
                .thenReturn("https://res.cloudinary.com/x/image/authenticated/s--sig--/v1/b.pdf");
        when(storageService.fetch(anyString())).thenReturn(FILE_BYTES);

        // Every test here shares the demo tenant, and several of them assert on "how many
        // bulletins does this company have" — including the one that asserts on none. Without
        // this, the rows of one test become the premise of the next and the suite poisons itself,
        // exactly as CompanyUsersIT did on 10.09.2026. The table is this slice's own, so clearing
        // it touches nothing in the seed.
        bulletinRepository.deleteAll();
        assertThat(bulletinRepository.findAll())
                .as("premisa fiecărui test: zero buletine înainte de el")
                .isEmpty();

        admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        adminToken = jwtService.generateToken(admin);
        hazardous = wasteCodeRepository.findAll().stream()
                .filter(WasteCode::isHazardous).findFirst().orElseThrow();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company other = companyRepository.save(Company.builder()
                .name("Tenant B SRL").cui("ROB" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser userB = appUserRepository.save(AppUser.builder()
                .email("admin+" + suffix + "@tenantb.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(other).enabled(true).createdAt(Instant.now()).build());
        otherTenantToken = jwtService.generateToken(userB);
    }

    // --- ce refuză, și de ce fiecare refuz e o regulă din act ---

    /**
     * Fișierul e obligatoriu, și ăsta e chiar obiectul. Art. 48 alin. (2) cere să
     * <em>deții</em> buletinul — hârtia —, nu să declari că există. Un rând fără fișier ar fi o
     * afirmație tipărită într-un dosar pe care îl citește un inspector, fără nimic în spate.
     */
    @Test
    void aBulletinWithoutItsFileIsRefused() throws Exception {
        mockMvc.perform(multipart("/api/v1/analysis-bulletins")
                        .param("wasteCodeId", hazardous.getId().toString())
                        .param("issueDate", LocalDate.now().minusDays(5).toString())
                        .param("laboratory", "Laborator SRL")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']").value("bulletin.file.required"));
        verify(storageService, never()).upload(any(), anyString());
    }

    @Test
    void aBulletinWithoutALaboratoryIsRefused() throws Exception {
        mockMvc.perform(multipart("/api/v1/analysis-bulletins")
                        .file(pdf())
                        .param("wasteCodeId", hazardous.getId().toString())
                        .param("issueDate", LocalDate.now().minusDays(5).toString())
                        .param("laboratory", "   ")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']").value("bulletin.laboratory.required"));
    }

    /**
     * Data e cea de pe hârtia laboratorului, iar o analiză nu se poate fi făcut mâine. Fără garda
     * asta, dosarul ar purta o dată pe care inspectorul o vede singur că e imposibilă.
     */
    @Test
    void anIssueDateInTheFutureIsRefused() throws Exception {
        mockMvc.perform(multipart("/api/v1/analysis-bulletins")
                        .file(pdf())
                        .param("wasteCodeId", hazardous.getId().toString())
                        .param("issueDate", LocalDate.now().plusDays(1).toString())
                        .param("laboratory", "Laborator SRL")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['error-code']").value("bulletin.issue.date.future"));
    }

    // --- drumul obișnuit ---

    @Test
    void anUploadedBulletinIsListedAgainstItsCode() throws Exception {
        upload(hazardous, LocalDate.now().minusMonths(2), "Laborator Alfa");

        mockMvc.perform(get("/api/v1/analysis-bulletins")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].wasteCode").value(hazardous.getCode() + "*"))
                .andExpect(jsonPath("$[0].laboratory").value("Laborator Alfa"))
                .andExpect(jsonPath("$[0].hazardous").value(true));
    }

    /**
     * Mai multe buletine pe același cod sunt <b>istoricul</b> lui, nu un duplicat de curățat: o
     * reanaliză nu șterge buletinul vechi, fiindcă o fișă depusă într-un an anterior s-a sprijinit
     * pe el, iar art. 48 alin. (5) cere păstrarea evidenței cel puțin 3 ani. Cel mai recent stă
     * primul, fiindcă el răspunde la „ai caracterizarea?".
     */
    @Test
    void aSecondBulletinOnTheSameCodeIsHistoryNotAReplacement() throws Exception {
        upload(hazardous, LocalDate.now().minusYears(2), "Laborator Vechi");
        upload(hazardous, LocalDate.now().minusMonths(1), "Laborator Nou");

        mockMvc.perform(get("/api/v1/analysis-bulletins")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].laboratory").value("Laborator Nou"))
                .andExpect(jsonPath("$[1].laboratory").value("Laborator Vechi"));
    }

    /** Aceeași regulă ca la 11-bis: fișierul costă o sesiune și apartenența la firmă. */
    @Test
    void theFileIsTenantScoped() throws Exception {
        UUID id = upload(hazardous, LocalDate.now().minusDays(3), "Laborator Alfa");
        String url = "/api/v1/analysis-bulletins/" + id + "/continut";

        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(url).header("Authorization", "Bearer " + otherTenantToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(url).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(content().bytes(FILE_BYTES));
    }

    // --- dosarul de control: constatarea pe care felia o face posibilă ---

    /**
     * Cazul care contează. Firma are un cod periculos în evidență și <b>niciun</b> buletin, deci
     * dosarul trebuie să numească lipsa — nu doar obligația. Până la G-7, README-ul spunea
     * „aplicația nu ține încă buletinele de analiză" și se oprea acolo.
     */
    @Test
    void theDossierNamesTheCodesWithoutABulletin() throws Exception {
        recordHazardousMovement();

        String readme = readme();
        assertThat(readme)
                .contains("3. Caracterizarea deșeurilor periculoase generate — art. 8 alin. (4)")
                .contains("Buletine încărcate: 0 din")
                .contains("LIPSESC pentru:")
                .contains(hazardous.getCode() + "*")
                // Termenul pe care actul NU îl dă nu se inventează.
                .contains("nu are termen de valabilitate scris în act");
    }

    /**
     * Iar când buletinele există pentru <b>toate</b> codurile periculoase din evidență, aceeași
     * secțiune o spune.
     *
     * <p>Codurile se citesc din dosarul însuși, nu se scriu de mână: tenantul demo are trei coduri
     * periculoase în seed, nu unul, iar o listă fixată aici s-ar rupe tăcut la prima schimbare de
     * seed — testul ar trece pe „lipsesc", adică din motivul greșit.
     */
    @Test
    void theDossierConfirmsFullCoverage() throws Exception {
        recordHazardousMovement();

        for (String code : hazardousCodesNamedInDossier()) {
            upload(wasteCodeRepository.findByCode(code).orElseThrow(),
                    LocalDate.now().minusMonths(3), "Laborator Alfa");
        }

        assertThat(readme())
                .contains("Ai buletin de analiză încărcat pentru toate")
                .contains("buletine-analiza/")
                .doesNotContain("LIPSESC pentru:");
    }

    /**
     * Folderul se scrie chiar gol, și e deliberat: un folder absent se citește ca o funcție pe care
     * n-a folosit-o nimeni, iar unul care spune „nu există niciun buletin încărcat" e constatarea.
     * Același principiu ca la persoana desemnată.
     */
    @Test
    void theBulletinFolderIsWrittenEvenWhenEmpty() throws Exception {
        List<String> entries = zipEntryNames(dossier());
        assertThat(entries).contains("buletine-analiza/index.txt");

        String index = new String(readEntry(dossier(), "buletine-analiza/index.txt"),
                StandardCharsets.UTF_8);
        assertThat(index)
                .contains("Nu există niciun buletin de analiză încărcat")
                .contains("art. 8 alin. (4)");
    }

    /** Iar când există, fișierul chiar intră în arhivă, grupat pe cod. */
    @Test
    void theBulletinFileGoesIntoTheArchive() throws Exception {
        upload(hazardous, LocalDate.now().minusMonths(3), "Laborator Alfa");

        byte[] zip = dossier();
        String index = new String(readEntry(zip, "buletine-analiza/index.txt"), StandardCharsets.UTF_8);
        assertThat(index)
                .contains("Cod " + hazardous.getCode() + "*")
                .contains("Laborator Alfa")
                .contains("inclus în arhivă: da");
        assertThat(zipEntryNames(zip).stream().anyMatch(n -> n.startsWith("buletine-analiza/1-")))
                .isTrue();
    }

    // --- helpers ---

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "buletin.pdf", "application/pdf", FILE_BYTES);
    }

    private UUID upload(WasteCode code, LocalDate issued, String laboratory) throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/analysis-bulletins")
                        .file(pdf())
                        .param("wasteCodeId", code.getId().toString())
                        .param("issueDate", issued.toString())
                        .param("laboratory", laboratory)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(body.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1"));
    }

    /** A movement on a hazardous code, so the dossier has something to report on. */
    private void recordHazardousMovement() {
        WorkPoint wp = workPointRepository
                .findAllByCompany_IdAndActiveTrue(admin.getCompany().getId()).get(0);
        movementRepository.save(WasteMovement.builder()
                .company(admin.getCompany()).workPoint(wp).date(LocalDate.of(2026, 5, 10))
                .wasteCode(hazardous).quantity(new BigDecimal("12.000")).unit(Unit.KG)
                .operation(WasteOperation.GENERATED).register(WasteRegister.ANEXA_1)
                .deleted(false).createdBy(admin.getId()).build());
    }

    private byte[] dossier() throws Exception {
        return mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private String readme() throws Exception {
        return new String(readEntry(dossier(), "README.txt"), StandardCharsets.UTF_8);
    }

    /**
     * The hazardous codes the dossier itself reports for this tenant, read off the line it prints
     * ("Codurile: 05 01 08*, 13 02 08*, ..."), with the asterisk stripped back off.
     */
    private List<String> hazardousCodesNamedInDossier() throws Exception {
        String readme = readme();
        int from = readme.indexOf("3. Caracterizarea");
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("Codurile: ([^.]+)\\.")
                .matcher(readme.substring(from));
        assertThat(m.find()).as("dosarul numește codurile periculoase").isTrue();
        return java.util.Arrays.stream(m.group(1).split(","))
                .map(s -> s.trim().replace("*", ""))
                .filter(s -> !s.isBlank())
                .toList();
    }

    private static List<String> zipEntryNames(byte[] zip) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                names.add(e.getName());
            }
        }
        return names;
    }

    private static byte[] readEntry(byte[] zip, String name) throws Exception {
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                if (e.getName().equals(name)) {
                    return in.readAllBytes();
                }
            }
        }
        throw new AssertionError("Entry not found in archive: " + name);
    }
}
