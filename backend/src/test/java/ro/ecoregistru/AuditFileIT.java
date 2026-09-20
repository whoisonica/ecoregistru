package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
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
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FAZA DOSAR / D1: the control-dossier ZIP over the real HTTP stack. Verifies the archive is a
 * valid, non-empty ZIP with the expected entries, that a read-only viewer may download it, and
 * that it is tenant-scoped (a fresh tenant's dossier never contains another tenant's evidence).
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class AuditFileIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    ro.ecoregistru.service.PackagingService packagingService;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired AttachmentRepository attachmentRepository;

    private String adminToken;
    private String viewerToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtService.generateToken(appUserRepository.findByEmail("admin@demo.ro").orElseThrow());
        viewerToken = jwtService.generateToken(appUserRepository.findByEmail("viewer@demo.ro").orElseThrow());
    }

    @Test
    void downloadsZipWithExpectedEntries() throws Exception {
        MockHttpServletResponse res = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/zip")))
                .andExpect(header().string("Content-Disposition", containsString("dosar-control-2026.zip")))
                .andReturn().getResponse();

        List<String> entries = zipEntryNames(res.getContentAsByteArray());
        assertThat(entries).contains(
                "00-cuprins.txt",
                // The regulated document: four chapters per waste code, asked for by the
                // specialist on 23.08.2026 ("când dă print la dosar control să respecte
                // structura de 4 tabele pe care o am de la Andreea").
                "rapoarte/evidenta-gestiunii-deseurilor-2026.pdf",
                "rapoarte/evidenta-centralizata-2026.pdf",
                "autorizatii-parteneri.pdf",
                "atasamente/index.txt");
        // Rezumatul neoficial a ieșit din dosar pe 17.09.2026 (proprietarul); rămâne pe Evidențe.
        assertThat(entries).noneMatch(name -> name.matches("(.*/)?evidenta-2026\\.(xlsx|pdf)"));
    }

    @Test
    void theAnexa1SheetInTheDossierIsARealPdf() throws Exception {
        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        byte[] pdf = readEntryBytes(zip, "rapoarte/evidenta-gestiunii-deseurilor-2026.pdf");
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void theReadmeNamesTheSheetAndItsDeadline() throws Exception {
        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String readme = new String(readEntryBytes(zip, "00-cuprins.txt"), StandardCharsets.UTF_8);
        assertThat(readme)
                .contains("Evidența gestiunii deșeurilor generate 2026")
                .contains("HG 856/2002, anexa 1")
                // 15 March of the FOLLOWING year — the term of OUG 92/2021 art. 48 alin. (1).
                .contains("Termen de depunere: 15 martie 2027");
    }

    // --- Etapa 6: the dossier sized to the retention period ---

    /**
     * OUG 92/2021 art. 48 alin. (5) keeps the evidence "cel putin 3 ani", so a dossier may reach
     * three years back. Several years cannot share the flat layout - the file names repeat - so
     * each year gets its own folder, and the archive is named after the range.
     */
    @Test
    void threeYearsAreFoldered_oneYearStaysFlat() throws Exception {
        MockHttpServletResponse res = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .param("years", "3")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("dosar-control-2024-2026.zip")))
                .andReturn().getResponse();

        List<String> entries = zipEntryNames(res.getContentAsByteArray());
        assertThat(entries).contains(
                "00-cuprins.txt",
                "2024/rapoarte/evidenta-gestiunii-deseurilor-2024.pdf",
                "2025/rapoarte/evidenta-gestiunii-deseurilor-2025.pdf",
                "2026/rapoarte/evidenta-gestiunii-deseurilor-2026.pdf",
                "2026/rapoarte/evidenta-centralizata-2026.pdf",
                "2026/atasamente/index.txt",
                // One snapshot for the whole dossier: the status is read against today, not
                // against a reporting year.
                "autorizatii-parteneri.pdf");
        assertThat(entries).doesNotContain("rapoarte/evidenta-gestiunii-deseurilor-2026.pdf");
        assertThat(entries).doesNotContain("2026/autorizatii-parteneri.pdf");
    }

    @Test
    void theReadmeOfAMultiYearDossierNamesTheRetentionRule() throws Exception {
        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .param("years", "3")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String readme = new String(readEntryBytes(zip, "00-cuprins.txt"), StandardCharsets.UTF_8);
        assertThat(readme)
                .contains("Anii de raportare: 2024\u20132026")
                .contains("OUG 92/2021, art. 48")
                .contains("Termen de depunere: 15 martie 2027");
    }

    /**
     * A year nobody regenerated prints blank sheets. The dossier says so rather than handing over
     * an empty official form that looks like lost data - the demo tenant has no 2024 evidence.
     */
    @Test
    void aYearWithoutEvidenceLinesIsCalledOutInTheReadme() throws Exception {
        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .param("years", "3")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String readme = new String(readEntryBytes(zip, "00-cuprins.txt"), StandardCharsets.UTF_8);
        assertThat(readme).contains("nu exist\u0103 nicio linie de eviden\u021b\u0103 calculat\u0103");
    }

    /**
     * Five years, not three. The law's floor is three (OUG 92/2021, art. 48 alin. (5)); the
     * specialist asked for the margin on 24.08.2026 — "sunt 3 în lege dar de safety". A year the
     * application never kept comes out empty and named as such in README.txt, so the margin
     * cannot quietly ship a blank official sheet.
     */
    @Test
    void fiveYearsIsAllowedAndSixIsRefused() throws Exception {
        mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .param("years", "5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("dosar-control-2022-2026.zip")));

        mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .param("years", "6")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                // QA-TRACE point 11: the text, not only the code. It used to read "cel mult 3 ani",
                // presenting art. 48 alin. (5)'s floor as a ceiling — wrong twice, since the range
                // had grown to five. Five is our limit; three is the law's minimum.
                .andExpect(jsonPath("$['error-message']", containsString("cel mult 5 ani")))
                .andExpect(jsonPath("$['error-message']", containsString("3 ani")))
                .andExpect(jsonPath("$['error-message']", containsString("art. 48 alin. (5)")))
                .andExpect(jsonPath("$['error-message']", not(containsString("cel mult 3"))));
    }

    @Test
    void viewerMayDownloadBecauseItIsReadOnly() throws Exception {
        mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/zip")));
    }

    /**
     * Cât cântărește dosarul, înainte de descărcare: numără atașamentele firmei din interval, adună mărimile
     * știute și le numără separat pe cele fără mărime. Mișcarea ștearsă, anul din afara intervalului și
     * firma vecină nu intră.
     */
    @Test
    void sizeCountsOnlyThisTenantsLiveAttachmentsInTheRange() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company own = companyRepository.save(Company.builder()
                .name("Cantar SRL").cui("ROS" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("marime+" + suffix + "@demo.ro").password("x")
                .role(Role.CLIENT_VIEWER).company(own).enabled(true).createdAt(Instant.now()).build());
        WorkPoint wp = workPointRepository.save(WorkPoint.builder()
                .company(own).name("PL-" + suffix).active(true).createdAt(Instant.now()).build());
        WasteCode code = wasteCodeRepository.findAll().get(0);

        WasteMovement live2026 = movement(own, wp, code, user, LocalDate.of(2026, 3, 10), false);
        WasteMovement live2025 = movement(own, wp, code, user, LocalDate.of(2025, 6, 1), false);
        WasteMovement deleted2026 = movement(own, wp, code, user, LocalDate.of(2026, 4, 1), true);
        attachment(live2026, 1_000L);
        attachment(live2026, null);
        attachment(live2025, 5_000L);
        attachment(deleted2026, 9_000_000L);
        // Vecinul: demo are și el mișcări pe 2026; atașamentul lui nu trebuie să apară aici.
        WasteMovement neighbour = movementRepository.findAll().stream()
                .filter(m -> !m.getCompany().getId().equals(own.getId())).findFirst().orElseThrow();
        attachment(neighbour, 7_000_000L);

        String token = jwtService.generateToken(user);
        mockMvc.perform(get("/api/v1/audit-file/size").param("year", "2026")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachments").value(2))
                .andExpect(jsonPath("$.attachmentBytes").value(1000))
                .andExpect(jsonPath("$.unknownSize").value(1));
        mockMvc.perform(get("/api/v1/audit-file/size").param("year", "2026").param("years", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachments").value(3))
                .andExpect(jsonPath("$.attachmentBytes").value(6000))
                .andExpect(jsonPath("$.unknownSize").value(1));
        mockMvc.perform(get("/api/v1/audit-file/size").param("year", "2026").param("years", "6")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    private WasteMovement movement(Company company, WorkPoint wp, WasteCode code, AppUser user,
                                   LocalDate date, boolean deleted) {
        return movementRepository.save(WasteMovement.builder()
                .company(company).workPoint(wp).date(date).wasteCode(code)
                .quantity(new BigDecimal("10.000")).unit(Unit.KG).operation(WasteOperation.GENERATED)
                .deleted(deleted).createdBy(user.getId()).build());
    }

    private void attachment(WasteMovement movement, Long size) {
        attachmentRepository.save(Attachment.builder()
                .movement(movement).url("https://example.test/a").publicId("p-" + UUID.randomUUID())
                .fileName("a.pdf").contentType("application/pdf").sizeBytes(size)
                .createdAt(Instant.now()).build());
    }

    @Test
    void dossierIsTenantScoped() throws Exception {
        // Fresh tenant with a single 2026 movement whose work point name is unique.
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company other = companyRepository.save(Company.builder()
                .name("Izolat SRL").cui("ROZ" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        AppUser otherUser = appUserRepository.save(AppUser.builder()
                .email("izolat+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(other).enabled(true).createdAt(Instant.now()).build());
        WorkPoint wp = workPointRepository.save(WorkPoint.builder()
                .company(other).name("PL-UNIC-" + suffix).active(true).createdAt(Instant.now()).build());
        WasteCode code = wasteCodeRepository.findAll().get(0);
        movementRepository.save(WasteMovement.builder()
                .company(other).workPoint(wp).date(LocalDate.of(2026, 3, 10)).wasteCode(code)
                .quantity(new BigDecimal("100.000")).unit(Unit.KG).operation(WasteOperation.GENERATED)
                .deleted(false).createdBy(otherUser.getId()).build());

        String otherToken = jwtService.generateToken(otherUser);
        // Evidence is a regenerable cache; compute this tenant's so its xlsx has a data row.
        mockMvc.perform(post("/api/v1/evidences/regenerate").param("year", "2026")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk());

        byte[] otherZip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        // The summary xlsx left the dossier on 17.09.2026; the centralized sheet prints the same
        // work points, one page each, so it carries the proof now: this tenant's point, never the demo's.
        String centralized = Golden.flat(Golden.pdfText(
                readEntryBytes(otherZip, "rapoarte/evidenta-centralizata-2026.pdf")));
        assertThat(centralized).contains(Golden.flat("PL-UNIC-" + suffix)).doesNotContain(Golden.flat("Demo Reciclare"));
    }

    /**
     * Anexa 3 Ambalaje în dosar (proprietarul, 16.09.2026): .xls şi PDF pe fiecare punct de lucru cu
     * ambalaje în an, şi nimic pe punctul fără ambalaje. Un generator are numai ieşirile.
     */
    @Test
    void theDossierCarriesAnexa3PackagingPerWorkPointThatMovedPackaging() throws Exception {
        org.mockito.Mockito.clearInvocations(packagingService);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company gen = companyRepository.save(Company.builder()
                .name("Ambalaje Dosar SRL").cui("ROA" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("dosar-a3+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(gen).enabled(true).createdAt(Instant.now()).build());
        WorkPoint withPackaging = workPointRepository.save(WorkPoint.builder()
                .company(gen).name("Hala Florești").active(true).createdAt(Instant.now()).build());
        workPointRepository.save(WorkPoint.builder()
                .company(gen).name("Birou Cluj").active(true).createdAt(Instant.now()).build());
        Partner recycler = partnerRepository.save(Partner.builder()
                .company(gen).name("Reciclator Dosar SA").cui("RO9" + suffix.substring(0, 5))
                .authorizationNumber("AM 3/2025").type(PartnerType.RECOVERER).client(true).active(true)
                .createdAt(Instant.now()).build());
        movementRepository.save(WasteMovement.builder()
                .company(gen).workPoint(withPackaging).date(LocalDate.of(2026, 4, 2))
                .wasteCode(wasteCodeRepository.findByCode("15 01 01").orElseThrow())
                .quantity(new BigDecimal("80.000")).unit(Unit.KG).operation(WasteOperation.RECOVERED)
                .operationCode(WasteOperationCode.R3).register(WasteRegister.ANEXA_1).partner(recycler)
                .deleted(false).createdBy(user.getId()).build());

        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        List<String> entries = zipEntryNames(zip);
        assertThat(entries).contains("rapoarte/anexa3-ambalaje-2026-hala-floresti.xls", "rapoarte/anexa3-ambalaje-2026-hala-floresti.pdf");
        assertThat(entries).noneMatch(n -> n.contains("birou-cluj"));
        assertThat(new String(readEntryBytes(zip, "rapoarte/anexa3-ambalaje-2026-hala-floresti.pdf"), 0, 5)).isEqualTo("%PDF-");
        String readme = new String(readEntryBytes(zip, "00-cuprins.txt"), StandardCharsets.UTF_8);
        assertThat(readme).contains("rapoarte/anexa3-ambalaje-2026-hala-floresti.xls / .pdf").contains("Hala Florești");
        // 17.09.2026, scanarea de conformitate, pct. 4: art. 4 alin. (1) din Ordinul 794/2012 nu numeşte
        // generatorul, deci README-ul nu-i pune termenul de 25 februarie pe o foaie tipărită la cerere.
        // Anexa 1 Ambalaje nu intră aici (profilul n-are rol de piaţă), deci „25 februarie" ar putea
        // veni numai de la Anexa 3.
        // <b>O dată pe punct de lucru, nu de trei ori</b> (20.09.2026). Planul chema `anexa3` ca să
        // afle dacă punctul are ce tipări, iar pe urmă fiecare dintre cele două formate o chema din
        // nou — trei calcule pe punct şi pe an, fiecare citind mişcările anului întreg, toate în
        // aceeaşi tranzacţie. Se cere numărul de calcule, nu timpul: ce s-a reparat e numărătoarea.
        // Firma are două puncte, deci două calcule; înainte erau patru (două + două randări).
        // Control negativ făcut: cu randarea întoarsă pe `(an, punct, format)`, cade cu 4.
        org.mockito.Mockito.verify(packagingService, org.mockito.Mockito.times(2))
                .anexa3(org.mockito.ArgumentMatchers.eq(2026), org.mockito.ArgumentMatchers.any());
        assertThat(readme).contains("tipărită la cerere").contains("fără termen")
                .doesNotContain("Termen: 25 februarie");

        // Ecranul spune același lucru ca arhiva: Anexa 3 pe Hala Florești, nimic pe Birou Cluj, numai ieșirile.
        mockMvc.perform(get("/api/v1/audit-file/contents").param("year", "2026")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years[0].anexa3WorkPoints[0]").value("Hala Florești"))
                .andExpect(jsonPath("$.years[0].anexa3WorkPoints.length()").value(1))
                .andExpect(jsonPath("$.years[0].anexa3RoleMissing").value(false))
                .andExpect(jsonPath("$.anexa3ExitsOnly").value(true));
    }

    // --- G-2: the four obligations the dossier used to pass over in silence ---

    /**
     * OUG 92/2021 art. 62 alin. (1) lit. a) sanctions four obligations with the same
     * 40.000–60.000 lei as the evidence itself, and until 11.09.2026 the dossier named none of
     * them. Same pattern as the designated person: a gap must be visible as a gap.
     */
    @Test
    void theReadmeNamesTheFourObligationsThatCarryTheSameFine() throws Exception {
        String readme = flat(readmeOfDemo2026());

        assertThat(readme)
                .contains("ALTE OBLIGAȚII PE CARE LE VERIFICĂ INSPECTORUL")
                .contains("art. 62 alin. (1) lit. a): 40.000–60.000 lei")
                .contains("Colectarea separată — art. 17 alin. (3)")
                .contains("Înscrierea în registrul ANMAP — art. 36 alin. (1)–(2)")
                .contains("Predarea uleiurilor uzate — art. 31 alin. (3)");
    }

    /**
     * The separate collection of art. 17 alin. (3) stays a statement of the obligation and
     * concludes nothing: it happens on site, and neither the presence nor the absence of a
     * fraction in the evidence says whether it is done.
     */
    @Test
    void separateCollectionNamesTheTextileDateAndDrawsNoConclusion() throws Exception {
        assertThat(flat(readmeOfDemo2026()))
                .contains("hârtie, metal, plastic și sticlă")
                .contains("de la 1 ianuarie 2025 și pentru textile");
    }

    /**
     * The demo tenant hands over waste oils on {@code 13 02 08*} in April 2026, so both derived
     * obligations turn from a general note into a finding that names the code it found — with the
     * asterisk, the way an official form spells it.
     */
    @Test
    void theDerivedObligationsNameTheCodesTheyFound() throws Exception {
        String readme = flat(readmeOfDemo2026());

        // The characterisation of art. 8 alin. (4) left the dossier on 14.09.2026 with the analysis
        // bulletins (the specialist: a generator is not asked for them). Nothing may still tell the
        // client it concerns them, nor point at a folder that is no longer written.
        assertThat(readme)
                .doesNotContain("Caracterizarea deșeurilor periculoase")
                .doesNotContain("buletine-analiza");
        // art. 31 alin. (3): the ENTIRE quantity, and where the recipients' authorisations sit.
        assertThat(readme)
                .contains("Te privește: în anii din dosar apar mișcări pe coduri de ulei uzat")
                .contains("ÎNTREAGA cantitate")
                .contains("autorizatii-parteneri.pdf");
    }

    /**
     * And the other direction, which is the one that matters: a tenant with neither a hazardous
     * code nor an oil code is told the two obligations do not activate on its data, rather than
     * being handed an assertion it has to disprove. An alert is a statement — the rule
     * {@code ReportType} keeps, and the reason V21 exists.
     */
    @Test
    void aTenantWithNoHazardousAndNoOilCodesIsToldSo() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company other = companyRepository.save(Company.builder()
                .name("Fără ulei SRL").cui("ROU" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        AppUser otherUser = appUserRepository.save(AppUser.builder()
                .email("faraulei+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(other).enabled(true).createdAt(Instant.now()).build());
        WorkPoint wp = workPointRepository.save(WorkPoint.builder()
                .company(other).name("PL-" + suffix).active(true).createdAt(Instant.now()).build());
        // Hârtie şi carton: non-hazardous, and nowhere near chapter 13.
        WasteCode paper = wasteCodeRepository.findByCode("20 01 01").orElseThrow();
        movementRepository.save(WasteMovement.builder()
                .company(other).workPoint(wp).date(LocalDate.of(2026, 3, 10)).wasteCode(paper)
                .quantity(new BigDecimal("100.000")).unit(Unit.KG).operation(WasteOperation.GENERATED)
                .deleted(false).createdBy(otherUser.getId()).build());

        String otherToken = jwtService.generateToken(otherUser);
        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String readme = new String(readEntryBytes(zip, "00-cuprins.txt"), StandardCharsets.UTF_8);
        assertThat(flat(readme))
                .contains("nu apare niciun cod de ulei uzat")
                // The obligations are still named — only the conclusion changes.
                .contains("Predarea uleiurilor uzate — art. 31 alin. (3)")
                .doesNotContain("Te privește");
    }

    // --- G-10: a cincea obligație, art. 44 — programul de prevenire ---

    /**
     * Art. 44 alin. (1) și (3) sunt în aceeași listă de amenzi ca evidența însăși
     * (art. 62 alin. (1) lit. a), 40.000–60.000 lei), și până pe 11.09.2026 nu erau nicăieri —
     * nici în dosar, nici în calendar. Firma demo are autorizație de mediu în profil, deci
     * propoziția e o <b>constatare</b> și numește autorizația pe care s-a sprijinit.
     */
    @Test
    void thePreventionProgrammeIsNamedAndTheFindingNamesThePermit() throws Exception {
        String readme = flat(readmeOfDemo2026());

        assertThat(readme)
                .contains("Toate patru sunt în aceeași listă sancționată")
                .contains("Programul de prevenire și reducere a deșeurilor — art. 44 alin. (1) și (3)")
                .contains("firma are autorizație de mediu (APM-CJ-123)")
                // Cele două jumătăți ale lui alin. (3) se spun separat: una e în calendar,
                // cealaltă nu se poate verifica din aplicație și rămâne a clientului.
                .contains("până la 31 mai")
                .contains("publicarea pe site nu se poate verifica de aici");
    }

    /**
     * Și cealaltă jumătate a articolului, cea pe care nu o putem observa: o firmă fără număr de
     * autorizație de mediu în profil primește obligația <em>numită</em>, nu o afirmație despre ea.
     * Aceeași regulă ca la codurile periculoase și la uleiuri.
     */
    @Test
    void aCompanyWithoutAnEnvironmentalPermitIsToldTheObligationDoesNotActivate() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company other = companyRepository.save(Company.builder()
                .name("Fără autorizație SRL").cui("ROA" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        AppUser otherUser = appUserRepository.save(AppUser.builder()
                .email("faraautorizatie+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(other).enabled(true).createdAt(Instant.now()).build());

        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + jwtService.generateToken(otherUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(flat(new String(readEntryBytes(zip, "00-cuprins.txt"), StandardCharsets.UTF_8)))
                .contains("Programul de prevenire și reducere a deșeurilor — art. 44 alin. (1) și (3)")
                .contains("nu e trecut niciun număr de autorizație de mediu")
                .doesNotContain("firma are autorizație de mediu");
    }

    /**
     * Alin. (3) al art. 36 cere o înscriere în plus — operatorii care repară produse — dar în alt
     * registru și <b>fără</b> să fie în lista de amenzi din antetul blocului. Proba ține cele două
     * lucruri despărțite: dosarul o numește, și nu o strecoară sub cifra de 40.000–60.000 lei.
     */
    @Test
    void theRepairRegistryIsNamedAsASeparateParagraphOutsideTheFineList() throws Exception {
        assertThat(flat(readmeOfDemo2026()))
                .contains("operatorii care REPARĂ produse")
                .contains("fără")
                .contains("în lista de amenzi de mai sus");
    }

    // --- QA-TRACE G3: persoana desemnată, OUG 92/2021 art. 23 alin. (4)–(5) ---

    /**
     * The block the inspector asks for first. Until now {@code AuditFileIT} only named the pattern in
     * a comment: the block could have vanished and the dossier would have gone quiet about it.
     */
    @Test
    void theDesignatedPersonIsPrintedWithEverythingTheProfileHolds() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Cu gestionar SRL").cui("ROG" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now())
                .wasteManagerName("Ioana Pop").wasteManagerRole("inginer de mediu")
                .wasteManagerExternal(true).wasteManagerTraining("program recunoscut ANC, 2025")
                .build());

        assertThat(flat(readmeOf(company, suffix)))
                .contains("Persoana desemnată cu gestiunea deșeurilor (OUG 92/2021, art. 23 alin. (4)):")
                .contains("- Nume : Ioana Pop")
                .contains("- Calitate: inginer de mediu")
                .contains("- Delegată unei terțe persoane (art. 23 alin. (4), a doua variantă).")
                .contains("- Instruire: program recunoscut ANC, 2025")
                .doesNotContain("Persoana desemnată cu gestiunea deșeurilor: NECOMPLETATĂ")
                .doesNotContain("Instruire: NECOMPLETATĂ");
    }

    /** The gap is the finding: an empty profile speaks, with the article, instead of staying quiet. */
    @Test
    void aMissingDesignatedPersonIsWrittenAsMissingWithItsArticle() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Fără gestionar SRL").cui("ROF" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now())
                .wasteManagerName("   ")
                .build());

        assertThat(flat(readmeOf(company, suffix)))
                .contains("Persoana desemnată cu gestiunea deșeurilor: NECOMPLETATĂ.")
                .contains("OUG 92/2021, art. 23 alin. (4)")
                .contains("orice firmă a cărei activitate generează deșeuri")
                .doesNotContain("titularul unei autorizații de mediu")
                .doesNotContain("- Nume :");
    }

    /**
     * Art. 23 alin. (5), forma din Legea 17/2023: instruirea se cere numai „pentru activitățile care
     * necesită autorizație de mediu”. Fără autorizație, lipsa certificatului nu e o lipsă.
     */
    @Test
    void theTrainingIsMissingOnlyWhereTheCompanyHasAnEnvironmentalAuthorization() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company withoutAuth = companyRepository.save(Company.builder()
                .name("Birou fără autorizație SRL").cui("ROW" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now())
                .wasteManagerName("Ioana Pop")
                .build());
        assertThat(flat(readmeOf(withoutAuth, suffix)))
                .contains("- Nume : Ioana Pop")
                .doesNotContain("Instruire: NECOMPLETATĂ");

        String suffix2 = UUID.randomUUID().toString().substring(0, 8);
        Company withAuth = companyRepository.save(Company.builder()
                .name("Fabrică cu autorizație SRL").cui("ROX" + suffix2).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now())
                .environmentalAuthNumber("12/01.02.2024")
                .wasteManagerName("Ioana Pop")
                .build());
        assertThat(flat(readmeOf(withAuth, suffix2)))
                .contains("Instruire: NECOMPLETATĂ");
    }

    /**
     * Anexa 1 Ambalaje lipsea din dosar (specialista, 15.09.2026). Intră la firma care pune ambalaje
     * pe piaţă, în amândouă formele cerute de art. 6; comerciantul n-o depune, deci nu o primeşte.
     */
    @Test
    void thePackagingDeclarationIsInTheDossierOfWhoeverPutsPackagingOnTheMarket() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company producer = companyRepository.save(Company.builder()
                .name("Producător SRL").cui("ROP" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now())
                .marketRoles(new java.util.LinkedHashSet<>(List.of(ro.ecoregistru.enums.MarketRole.PRODUCER)))
                .build());
        Company trader = companyRepository.save(Company.builder()
                .name("Comerciant SRL").cui("ROT" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now())
                .marketRoles(new java.util.LinkedHashSet<>(List.of(ro.ecoregistru.enums.MarketRole.TRADER)))
                .build());

        byte[] producerZip = dossierOf(producer, "p" + suffix);
        assertThat(zipEntryNames(producerZip))
                .contains("rapoarte/anexa1-ambalaje-2026.xls", "rapoarte/anexa1-ambalaje-2026.pdf",
                        "rapoarte/evidenta-centralizata-2026.pdf", "autorizatii-parteneri.pdf");
        assertThat(new String(readEntryBytes(producerZip, "rapoarte/anexa1-ambalaje-2026.pdf"), 0, 5))
                .isEqualTo("%PDF-");
        assertThat(new String(readEntryBytes(producerZip, "00-cuprins.txt"), StandardCharsets.UTF_8))
                .contains("Anexa 1 Ambalaje (Ordinul 794/2012)");

        assertThat(zipEntryNames(dossierOf(trader, "t" + suffix)))
                .noneMatch(name -> name.contains("anexa1-ambalaje"));

        // Și ecranul spune de ce: producătorul o primește, comerciantul nu, iar un profil fără răspuns nu.
        assertThat(contentsOf(producer, "cp" + suffix)).contains("\"packagingDeclaration\":\"INCLUDED\"");
        assertThat(contentsOf(trader, "ct" + suffix)).contains("\"packagingDeclaration\":\"TRADER_ONLY\"");
        Company unanswered = companyRepository.save(Company.builder()
                .name("Fără profil SRL").cui("ROU" + suffix).type(CompanyType.GENERATOR)
                .active(true).afmObligation(false).createdAt(Instant.now()).build());
        assertThat(contentsOf(unanswered, "cu" + suffix)).contains("\"packagingDeclaration\":\"NOT_ANSWERED\"");
        assertThat(zipEntryNames(dossierOf(unanswered, "u" + suffix)))
                .noneMatch(name -> name.contains("anexa1-ambalaje"));
    }

    // --- 17.09.2026: rapoarte/, atasamente/ și lista autorizațiilor refăcută ---

    /**
     * Garda cea mai importantă a schimbării: documentele oficiale din dosar sunt exact cele de pe
     * butoanele lor. Arhiva le-a schimbat doar numele; dacă o refacere a ambalajului ar atinge foaia,
     * textul ar ieși diferit aici.
     */
    @Test
    void theOfficialSheetsInTheDossierAreTheSameAsTheirOwnDownloads() throws Exception {
        byte[] zip = demoZip();
        byte[] sheet = mockMvc.perform(get("/api/v1/evidences/anexa1").param("year", "2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        byte[] centralized = mockMvc.perform(get("/api/v1/evidences/declaratie-anuala").param("year", "2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        assertThat(Golden.pdfText(readEntryBytes(zip, "rapoarte/evidenta-gestiunii-deseurilor-2026.pdf")))
                .isNotBlank().isEqualTo(Golden.pdfText(sheet));
        assertThat(Golden.pdfText(readEntryBytes(zip, "rapoarte/evidenta-centralizata-2026.pdf")))
                .isNotBlank().isEqualTo(Golden.pdfText(centralized));
    }

    /** Cuprinsul și arhiva citesc aceleași nume: fiecare fișier și folder numit în cuprins e în arhivă. */
    @Test
    void everyNumberedFileTheContentsNamesIsInTheArchive() throws Exception {
        byte[] zip = demoZip();
        List<String> entries = zipEntryNames(zip);
        assertThat(entries.get(0)).isEqualTo("00-cuprins.txt");

        List<String> named = new ArrayList<>();
        for (String line : new String(readEntryBytes(zip, "00-cuprins.txt"), StandardCharsets.UTF_8).split("\n")) {
            String name = line.strip();
            if (line.startsWith("  ") && !line.startsWith("   ") && !name.startsWith("-") && !name.contains(" ")
                    && (name.contains(".") || name.endsWith("/"))) {
                named.add(name);
            }
        }
        assertThat(named).isNotEmpty();
        for (String name : named) {
            if (name.endsWith(".xls / .pdf")) {
                String base = name.substring(0, name.length() - ".xls / .pdf".length());
                assertThat(entries).contains(base + ".xls", base + ".pdf");
            } else if (name.endsWith(".xlsx / .pdf")) {
                String base = name.substring(0, name.length() - ".xlsx / .pdf".length());
                assertThat(entries).contains(base + ".xlsx", base + ".pdf");
            } else if (name.endsWith("/")) {
                assertThat(entries).anyMatch(e -> e.startsWith(name));
            } else {
                assertThat(entries).contains(name);
            }
        }
    }

    /**
     * Lista autorizațiilor scria cu Helvetica standard, fără ă, ș, ț: „Autorizaii”, „Expir în”. Acum le
     * are, și numește codurile fiecărui partener — demo-ul predă ulei uzat pe 13 02 08* în aprilie 2026.
     */
    @Test
    void thePartnerListKeepsItsDiacriticsAndNamesTheCodes() throws Exception {
        byte[] zip = demoZip();
        String text = Golden.flat(Golden.pdfText(readEntryBytes(zip, "autorizatii-parteneri.pdf")));

        assertThat(text)
                .contains(Golden.flat("Autorizațiile partenerilor"))
                .contains(Golden.flat("Valabilă până la"))
                .contains(Golden.flat("Coduri de deșeu 2026"))
                .contains("130208*")
                .contains("pagina1din1")
                .doesNotContain("Autorizaii");
    }

    private byte[] demoZip() throws Exception {
        return mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private String contentsOf(Company company, String suffix) throws Exception {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("continut+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        return mockMvc.perform(get("/api/v1/audit-file/contents")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * Ecranul și arhiva, citite în același moment pe firma demo, spun același lucru: Anexa 1 Ambalaje
     * e anunțată exact când arhiva o are, Anexa 3 pe exact punctele pe care le are arhiva, iar „lipsește”
     * exact când cuprinsul scrie LIPSEȘTE. Nu presupune profilul demo-ului — alte teste din suită îl
     * schimbă —, ci compară cele două citiri. Pe trei ani, anii vin în ordine și 2026 are mișcări.
     */
    @Test
    void theContentsTellWhatTheArchiveHolds() throws Exception {
        byte[] zip = demoZip();
        List<String> entries = zipEntryNames(zip);
        String readme = new String(readEntryBytes(zip, "00-cuprins.txt"), StandardCharsets.UTF_8);
        String json = mockMvc.perform(get("/api/v1/audit-file/contents").param("year", "2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        com.jayway.jsonpath.DocumentContext c = com.jayway.jsonpath.JsonPath.parse(json);

        assertThat(c.read("$.packagingDeclaration", String.class).equals("INCLUDED"))
                .isEqualTo(entries.contains("rapoarte/anexa1-ambalaje-2026.pdf"));
        List<String> points = c.read("$.years[0].anexa3WorkPoints");
        assertThat(entries.stream().filter(n -> n.startsWith("rapoarte/anexa3-ambalaje-2026-") && n.endsWith(".pdf")).count())
                .isEqualTo(points.size());
        assertThat(c.read("$.years[0].anexa3RoleMissing", Boolean.class))
                .isEqualTo(readme.contains("LIPSEȘTE: anul are ambalaje"));
        assertThat(c.read("$.years[0].movements", Integer.class)).isPositive();
        assertThat(c.read("$.partners", Integer.class)).isEqualTo(
                partnerRepository.findAllByCompany_Id(
                        appUserRepository.findByEmail("admin@demo.ro").orElseThrow().getCompany().getId()).size());

        mockMvc.perform(get("/api/v1/audit-file/contents").param("year", "2026").param("years", "3")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.years.length()").value(3))
                .andExpect(jsonPath("$.years[0].year").value(2024))
                .andExpect(jsonPath("$.years[2].year").value(2026));

        mockMvc.perform(get("/api/v1/audit-file/contents").param("year", "2026").param("years", "6")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    private byte[] dossierOf(Company company, String suffix) throws Exception {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("dosar+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        return mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private String readmeOf(Company company, String suffix) throws Exception {
        AppUser user = appUserRepository.save(AppUser.builder()
                .email("gestionar+" + suffix + "@demo.ro").password("x")
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return new String(readEntryBytes(zip, "00-cuprins.txt"), StandardCharsets.UTF_8);
    }

    private String readmeOfDemo2026() throws Exception {
        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return new String(readEntryBytes(zip, "00-cuprins.txt"), StandardCharsets.UTF_8);
    }

    /**
     * README.txt is wrapped to fit paper, so a sentence of it may sit on two lines. What these
     * tests are about is what the dossier says, not where it breaks — assert on the flattened
     * text and the wrap stays free to change.
     */
    private static String flat(String readme) {
        return readme.replaceAll("\\s+", " ");
    }

    /**
     * R1 — reconstrucția evidenței comite înainte să înceapă scrisul în răspuns.
     *
     * <p>{@code lockForRebuild} ia un {@code pg_advisory_xact_lock} pe firmă, iar un lacăt de
     * tranzacție ține până la commit. Cât timp dosarul chema reconstrucția din propria lui
     * tranzacție — cea care ține toată descărcarea, inclusiv aducerea atașamentelor de la
     * Cloudinary — firma rămânea blocată tot atâta: orice salvare de mișcare începe cu același
     * lacăt, la fel importul și anularea lui.
     *
     * <p><b>Ce dovedește proba și ce nu:</b> că metoda pe care o cheamă dosarul își deschide
     * tranzacția ei ({@code REQUIRES_NEW}), deci lacătul pleacă la întoarcerea din ea. Nu măsoară
     * un client blocat — asta ar cere două fire și o descărcare încetinită dinadins, adică o probă
     * care cade din când în când degeaba. Regresia pe care o prinde e cea probabilă: cineva scoate
     * adnotarea sau întoarce apelul la {@code regenerateYear}.
     */
    @Test
    void theDossierRebuildsTheEvidenceInATransactionOfItsOwn() throws Exception {
        var method = ro.ecoregistru.service.EvidenceCalculator.class
                .getMethod("regenerateYearBeforeStreaming", int.class);
        var tx = method.getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        assertThat(tx).as("metoda chemată de dosar își deschide tranzacția ei").isNotNull();
        assertThat(tx.propagation())
                .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);

        // Controlul: metoda obișnuită NU are REQUIRES_NEW, altfel egalitatea de mai sus ar fi
        // trecut şi dacă toate metodele ar fi fost la fel, iar proba n-ar fi spus nimic.
        var plain = ro.ecoregistru.service.EvidenceCalculator.class.getMethod("regenerateYear", int.class);
        assertThat(plain.getAnnotation(org.springframework.transaction.annotation.Transactional.class).propagation())
                .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRED);
    }

    /**
     * R2 — arhiva spune singură dacă e întreagă.
     *
     * <p>Odată scris primul octet, răspunsul e comis cu 200: o excepţie la jumătate lăsa clientul
     * cu un ZIP care se deschide şi pare în regulă, dar căruia îi lipsesc ani — iar dosarul ăsta se
     * duce la control. Foaia se scrie <b>ultima</b>, deci prezenţa ei cu „DOSAR COMPLET" e chiar
     * dovada că tot ce e înaintea ei a trecut.
     *
     * <p>De aceea proba cere şi că e <b>ultima intrare</b>, nu doar că există: o scriere adăugată
     * după ea ar face-o să mintă, şi atunci cade aici, nu la client.
     */
    @Test
    void theDossierEndsWithAVerificationSheetThatSaysItIsComplete() throws Exception {
        byte[] zip = mockMvc.perform(get("/api/v1/audit-file")
                        .param("year", "2026")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        List<String> entries = zipEntryNames(zip);
        assertThat(entries).contains("99-verificare.txt");
        assertThat(entries.get(entries.size() - 1))
                .as("foaia de verificare se scrie ultima, altfel nu dovedeşte nimic")
                .isEqualTo("99-verificare.txt");

        String check = new String(readEntryBytes(zip, "99-verificare.txt"), StandardCharsets.UTF_8);
        assertThat(check).startsWith("DOSAR COMPLET");
        assertThat(check).doesNotContain("INCOMPLET");
        // Numără ce a intrat cu adevărat, nu ce s-a plănuit: fiecare intrare, cu octeţii ei.
        assertThat(check).contains("Fișiere scrise: " + (entries.size() - 1));
        for (String name : entries) {
            if (!name.equals("99-verificare.txt")) {
                assertThat(check).as("fişierul %s trebuie numit în foaia de verificare", name)
                        .contains(name + "\t");
            }
        }
    }

    private List<String> zipEntryNames(byte[] zipBytes) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                names.add(e.getName());
            }
        }
        assertThat(zipBytes).isNotEmpty();
        return names;
    }

    /** Extracts a single entry's (decompressed) bytes from the outer ZIP. */
    private byte[] readEntryBytes(byte[] zipBytes, String entryName) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().equals(entryName)) {
                    return zis.readAllBytes();
                }
            }
        }
        throw new AssertionError("Entry not found: " + entryName);
    }
}
