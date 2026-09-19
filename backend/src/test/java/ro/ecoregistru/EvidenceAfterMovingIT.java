package ro.ecoregistru;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
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
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.EvidenceCalculator;
import ro.ecoregistru.service.export.Anexa1Sheet;
import ro.ecoregistru.service.export.AnnualDeclaration;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA de lansare, generator — G07 și G09 (`ecoregistru-docs/qa/LAUNCH-GAP-MAP.md`).
 *
 * <p><b>G07.</b> {@code EvidenceFreshnessIT} probează că o mișcare <em>adăugată</em> sau <em>ștearsă</em>
 * după ultima reconstrucție ajunge pe fișă. Aici e cealaltă jumătate: o mișcare <b>mutată</b> —
 * altă dată, alt cod, alt punct de lucru. Mutarea e o corectură obișnuită (data greșită, codul
 * greșit) și e singura schimbare care scoate un rând din grupul în care era, nu doar îl atinge.
 *
 * <p><b>G09.</b> O ieșire care așteaptă cântarul destinatarului face linia de evidență provizorie.
 * Declarația anuală rezumă aceleași linii: spune și ea că e provizorie?
 *
 * <p>Oracolul e socotit de mână din rândurile puse în test, niciodată din motorul de evidență.
 * Toate mișcările trec prin API, ca un client: {@code PUT} e drumul pe care o corectură îl ia în
 * realitate.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class EvidenceAfterMovingIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EvidenceCalculator evidenceCalculator;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;

    private UUID tenantId;
    private String token;
    private UUID pointA;
    private UUID pointB;
    private UUID paper;
    private UUID glass;
    private UUID collector;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name("Mutare SRL").cui("ROM" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        tenantId = company.getId();
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("mutare+" + suffix + "@demo.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        token = jwtService.generateToken(admin);
        adminId = admin.getId();
        pointA = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Nord").active(true).createdAt(Instant.now()).build()).getId();
        pointB = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Sud").active(true).createdAt(Instant.now()).build()).getId();
        paper = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
        glass = wasteCodeRepository.findByCode("20 01 02").orElseThrow().getId();
        collector = partnerRepository.save(Partner.builder()
                .company(company).name("Colector Mutare SRL").authorizationNumber("AM 7/2025").cui("RO" + suffix)
                .type(PartnerType.COLLECTOR).supplier(true).active(true)
                .createdAt(Instant.now()).build()).getId();
        TenantContext.set(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---------- G07: o mișcare mutată ----------

    /**
     * Decembrie 2025 → ianuarie 2026, cu fișa pe 2025 deja citită o dată (cache cald).
     *
     * <p>De mână. Din V58 generarea se înregistrează la predare: o ieșire fără stoc înainte
     * implică generarea aceleiași cantități în aceeași lună (G03). Pe 2025: 1000 kg predate în
     * iunie, 300 kg pe 15 decembrie; cele 300 pleacă în 2026. Pe 2025 rămân 1000 generate și 1000
     * valorificate, nimic în decembrie.
     *
     * <p>Azi: prospețimea anului 2025 se judecă după ultima schimbare a mișcărilor datate
     * <em>până la</em> 31.12.2025 ({@code WasteMovementRepository.findLastChangeUpTo}), cu data
     * <em>curentă</em>. Mișcarea mutată nu mai e în filtru, restul nu s-au schimbat, deci anul pare
     * proaspăt și fișa tipărește cifrele vechi.
     */
    @Test
    void aMovementMovedFromDecemberIntoJanuaryLeavesTheOldYear() throws Exception {
        handover(pointA, paper, "2025-06-10", "1000");
        String december = handover(pointA, paper, "2025-12-15", "300");
        readEvidence(2025); // cache-ul anului 2025, cu decembrie la 300

        update(december, pointA, paper, "2026-01-10", "300");

        Anexa1Sheet sheet = onlySheet(2025, pointA);
        assertKg(sheet.rows().get(11).generated(), "0");
        assertKg(sheet.rows().get(11).recovered(), "0");
        AnnualDeclaration.Row row = onlyDeclarationRow(2025, pointA);
        assertKg(row.generated(), "1000");
        assertKg(row.recovered(), "1000");
    }

    /**
     * Controlul pozitiv al probei de mai sus: același drum, dar anul următor e citit primul.
     * {@code findEarliestStaleYear} prinde atunci anul 2025. Arată că oracolul e bun și că
     * defectul e doar în ordinea citirilor.
     */
    @Test
    void theSameMoveIsCaughtWhenTheLaterYearIsReadFirst() throws Exception {
        handover(pointA, paper, "2025-06-10", "1000");
        String december = handover(pointA, paper, "2025-12-15", "300");
        readEvidence(2025);

        update(december, pointA, paper, "2026-01-10", "300");
        readEvidence(2026);

        assertKg(onlySheet(2025, pointA).rows().get(11).generated(), "0");
        assertKg(onlyDeclarationRow(2025, pointA).generated(), "1000");
        Anexa1Sheet next = onlySheet(2026, pointA);
        assertKg(next.rows().get(0).generated(), "300");
        assertKg(next.rows().get(0).recovered(), "300");
    }

    /** Codul greșit, corectat: 400 kg trec de pe hârtie pe sticlă, în aceeași lună. */
    /**
     * Anul părăsit e primul an cu linii și duce stoc în anul următor. Prima reparație a BUG-031
     * ștergea liniile anului părăsit: citit primul, 2026 nu mai găsea nimic pe 2025, se deschidea
     * la zero și, proaspăt după aceea, păstra zero-ul și după ce 2025 se reconstruia.
     *
     * <p>De mână: 1000 kg generate în iunie 2025 (linie veche, rămasă în stoc), 300 kg predate pe
     * 15 decembrie și mutate pe 10 ianuarie. 2025 se închide cu 1000, deci 2026 se deschide cu 1000.
     */
    @Test
    void theNextYearKeepsItsOpeningStockAfterTheMove() throws Exception {
        legacyGenerated(pointA, paper, "2025-06-10", "1000");
        String december = handover(pointA, paper, "2025-12-15", "300");
        readEvidence(2025);
        readEvidence(2026);
        assertKg(onlySheet(2026, pointA).openingStock(), "700");

        update(december, pointA, paper, "2026-01-10", "300");
        readEvidence(2026);
        readEvidence(2025);

        assertKg(onlySheet(2025, pointA).rows().get(11).closingStock(), "1000");
        assertKg(onlySheet(2026, pointA).openingStock(), "1000");
    }

    /** Singura mișcare a anului pleacă în anul următor: anul rămas fără mișcări nu mai tipărește nimic. */
    @Test
    void aYearLeftWithoutMovementsDropsItsLines() throws Exception {
        String only = handover(pointA, paper, "2025-12-15", "300");
        readEvidence(2025);

        update(only, pointA, paper, "2026-01-10", "300");

        TenantContext.set(tenantId);
        assertThat(evidenceCalculator.anexa1(2025, pointA)).isEmpty();
    }

    @Test
    void aMovementMovedToAnotherCodeLeavesTheOldCodesSheet() throws Exception {
        handover(pointA, paper, "2026-02-10", "100");
        String wrong = handover(pointA, paper, "2026-03-10", "400");
        readEvidence(2026);

        update(wrong, pointA, glass, "2026-03-10", "400");

        List<Anexa1Sheet> sheets = sheets(2026, pointA);
        assertKg(sheetFor(sheets, "20 01 01").rows().get(2).generated(), "0");
        assertKg(sheetFor(sheets, "20 01 01").rows().get(2).recovered(), "0");
        assertKg(sheetFor(sheets, "20 01 02").rows().get(2).generated(), "400");
        assertKg(sheetFor(sheets, "20 01 02").rows().get(2).recovered(), "400");
    }

    /** Punctul de lucru greșit, corectat: 400 kg trec de la Nord la Sud. */
    @Test
    void aMovementMovedToAnotherWorkPointLeavesTheOldPointsSheet() throws Exception {
        handover(pointA, paper, "2026-02-10", "100");
        String wrong = handover(pointA, paper, "2026-03-10", "400");
        readEvidence(2026);

        update(wrong, pointB, paper, "2026-03-10", "400");

        assertKg(onlySheet(2026, pointA).rows().get(2).recovered(), "0");
        assertKg(onlySheet(2026, pointB).rows().get(2).recovered(), "400");
        assertKg(onlyDeclarationRow(2026, pointA).recovered(), "100");
        assertKg(onlyDeclarationRow(2026, pointB).recovered(), "400");
    }

    // ---------- G09: declarația cu o ieșire necântărită ----------

    /**
     * 1000 kg predate în martie; în aprilie o încărcătură pleacă la colector, cântărită la
     * descărcare, fără cantitate încă. Linia din aprilie e provizorie ({@code awaitingWeighing}),
     * deci și totalurile anului sunt provizorii: nu se știe cât a plecat.
     *
     * <p>Așteptat: declarația anuală tipărită spune asta, cum spune deja pentru ieșirile fără cod
     * R/D (nota „(*)”). Oracolul caută „cântăr” în textul PDF-ului, fără diacritice; cum anume o
     * spune e treaba reparației.
     */
    @Test
    void theAnnualDeclarationSaysAWeightIsStillMissing() throws Exception {
        handover(pointA, paper, "2025-03-10", "1000");
        unweighedHandover(pointA, paper, "2025-04-10");

        assertThat(fold(declarationText(2025))).contains("cantar");
    }

    /**
     * Controlul negativ: fără ieșirea necântărită, cuvântul nu apare. Fără el, proba de mai sus ar
     * putea trece pe un antet care conține „cântar” din alt motiv.
     */
    @Test
    void aFullyWeighedYearSaysNothingAboutWeighing() throws Exception {
        handover(pointA, paper, "2025-03-10", "1000");

        assertThat(fold(declarationText(2025))).doesNotContain("cantar");
    }

    /** Ecranul știe deja: linia din aprilie e marcată. Asta e premisa probei de mai sus. */
    @Test
    void theEvidenceLineItselfIsMarkedAwaitingWeighing() throws Exception {
        handover(pointA, paper, "2025-03-10", "1000");
        unweighedHandover(pointA, paper, "2025-04-10");

        String json = mockMvc.perform(get("/api/v1/evidences?year=2025&month=4")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Boolean> awaiting = JsonPath.read(json, "$[*].awaitingWeighing");
        assertThat(awaiting).containsExactly(true);
    }

    // --- helpers ---

    private String handover(UUID workPoint, UUID code, String date, String kg) throws Exception {
        String json = mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handoverJson(workPoint, code, date, kg)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private void legacyGenerated(UUID workPoint, UUID code, String date, String kg) {
        movementRepository.saveAndFlush(WasteMovement.builder()
                .company(companyRepository.getReferenceById(tenantId))
                .workPoint(workPointRepository.getReferenceById(workPoint))
                .date(java.time.LocalDate.parse(date)).wasteCode(wasteCodeRepository.getReferenceById(code))
                .quantity(new BigDecimal(kg)).unit(Unit.KG)
                .operation(WasteOperation.GENERATED).register(WasteRegister.ANEXA_1)
                .deleted(false).createdBy(adminId).build());
    }

    private void update(String id, UUID workPoint, UUID code, String date, String kg) throws Exception {
        mockMvc.perform(put("/api/v1/movements/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(handoverJson(workPoint, code, date, kg)))
                .andExpect(status().isOk());
    }

    private void unweighedHandover(UUID workPoint, UUID code, String date) throws Exception {
        mockMvc.perform(post("/api/v1/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workPointId": "%s", "date": "%s", "wasteCodeId": "%s",
                                 "unit": "KG", "physicalState": "SOLID", "weighedAtUnloading": true,
                                 "operation": "RECOVERED", "register": "ANEXA_1", "physicalState": "SOLID", "storageType": "CT", "transportMeans": "AN", "packagingCategory": "SECONDARY", "wasteDestination": "Vr",
                                 "operationCode": "R13", "partnerId": "%s"}
                                """.formatted(workPoint, date, code, collector)))
                .andExpect(status().isOk());
    }

    private String handoverJson(UUID workPoint, UUID code, String date, String kg) {
        return """
                {"workPointId": "%s", "date": "%s", "wasteCodeId": "%s", "quantity": %s,
                 "unit": "KG", "physicalState": "SOLID", "operation": "RECOVERED", "register": "ANEXA_1",
                 "physicalState": "SOLID", "storageType": "CT", "transportMeans": "AN", "packagingCategory": "SECONDARY", "wasteDestination": "Vr", "operationCode": "R13", "partnerId": "%s"}
                """.formatted(workPoint, date, code, kg, collector);
    }

    private void readEvidence(int year) throws Exception {
        mockMvc.perform(get("/api/v1/evidences?year=" + year)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String declarationText(int year) throws Exception {
        byte[] pdf = mockMvc.perform(get("/api/v1/evidences/declaratie-anuala?year=" + year)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        PdfReader reader = new PdfReader(pdf);
        try {
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(new PdfTextExtractor(reader).getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    /** Direct pe serviciu; contextul se repune, fiindcă filtrul îl golește după fiecare cerere. */
    private List<Anexa1Sheet> sheets(int year, UUID workPoint) {
        TenantContext.set(tenantId);
        return evidenceCalculator.anexa1(year, workPoint);
    }

    private Anexa1Sheet onlySheet(int year, UUID workPoint) {
        List<Anexa1Sheet> sheets = sheets(year, workPoint);
        assertThat(sheets).hasSize(1);
        return sheets.get(0);
    }

    private static Anexa1Sheet sheetFor(List<Anexa1Sheet> sheets, String code) {
        return sheets.stream().filter(s -> s.wasteCode().equals(code)).findFirst().orElseThrow(
                () -> new AssertionError("nicio fișă pentru " + code));
    }

    private AnnualDeclaration.Row onlyDeclarationRow(int year, UUID workPoint) {
        TenantContext.set(tenantId);
        List<AnnualDeclaration> declarations = evidenceCalculator.annualDeclaration(year, workPoint);
        assertThat(declarations).hasSize(1);
        assertThat(declarations.get(0).rows()).hasSize(1);
        return declarations.get(0).rows().get(0);
    }

    private static void assertKg(BigDecimal actual, String expected) {
        assertThat(actual).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal(expected));
    }

    /** „ă” iese din extractorul PDF ca „ª” (fontul cp1250), deci se pliază și el pe „a”. */
    private static String fold(String text) {
        return Normalizer.normalize(text.replace('ª', 'a'), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
