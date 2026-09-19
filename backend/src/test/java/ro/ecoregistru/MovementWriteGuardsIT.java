package ro.ecoregistru;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.MovementDocumentService;
import ro.ecoregistru.service.export.Anexa3FormGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA de lansare, generator — G10, G13, G14, G24, G26 (`ecoregistru-docs/qa/LAUNCH-GAP-MAP.md`).
 *
 * <p>Ce apără scrierea unei mișcări: numărul Anexei 3 (unic, fără goluri, nears de o tipărire
 * căzută), cantitatea din cântărirea ulterioară, lungimea textelor libere și referințele din
 * corpul cererii care arată spre altă firmă.
 *
 * <p>Două firme GENERATOR, A și B, fiecare cu punct de lucru, partener autorizat și un punct de
 * lucru al partenerului. Toate atacurile pleacă din A spre rândurile lui B.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class MovementWriteGuardsIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MovementDocumentService documentService;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired PartnerWorkPointRepository partnerWorkPointRepository;
    @Autowired WasteMovementRepository movementRepository;
    @MockitoSpyBean Anexa3FormGenerator anexa3FormGenerator;

    private record Tenant(UUID companyId, String token, UUID workPoint, UUID partner, UUID partnerWorkPoint) {}

    private Tenant a;
    private Tenant b;
    private UUID paper;

    @BeforeEach
    void setUp() {
        paper = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
        a = tenant("Alfa Hârtie SRL");
        b = tenant("Beta Carton SRL");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Tenant tenant(String name) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Company company = companyRepository.save(Company.builder()
                .name(name).cui("RO" + suffix).type(CompanyType.GENERATOR)
                .active(true).createdAt(Instant.now()).build());
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("scriere+" + suffix + "@demo.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build());
        WorkPoint workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL " + name).active(true).createdAt(Instant.now()).build());
        Partner partner = partnerRepository.save(Partner.builder()
                .company(company).name("Colector " + name).cui("RO9" + suffix)
                .authorizationNumber("AM 1/2025").type(PartnerType.COLLECTOR).supplier(true).carrier(true)
                .active(true).createdAt(Instant.now()).build());
        PartnerWorkPoint depot = partnerWorkPointRepository.save(PartnerWorkPoint.builder()
                .partner(partner).name("Depozit " + name).address("Str. Depozitului 1")
                .active(true).createdAt(Instant.now()).build());
        return new Tenant(company.getId(), jwtService.generateToken(admin), workPoint.getId(),
                partner.getId(), depot.getId());
    }

    // ---------- G10: numerotarea Anexei 3 ----------

    /**
     * Opt tipăriri în același timp, pe opt predări diferite ale unei firme noi: numerele sunt
     * 1..8, fiecare o dată. Un număr dublat pe două formulare e un carnet pe care nu-l poate apăra
     * nimeni la control; un gol arată ca un formular rupt.
     */
    @Test
    void parallelPrintsOfDifferentMovementsGetNumbersOneToEightOnce() throws Exception {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            ids.add(UUID.fromString(handover(a, "5")));
        }

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> done = new ArrayList<>();
        for (UUID id : ids) {
            done.add(pool.submit(() -> {
                start.await();
                TenantContext.set(a.companyId());
                try {
                    return documentService.renderAnexa3(id);
                } finally {
                    TenantContext.clear();
                }
            }));
        }
        start.countDown();
        for (Future<?> f : done) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        Set<Integer> numbers = ids.stream()
                .map(id -> movementRepository.findById(id).orElseThrow().getAnexa3Number())
                .collect(Collectors.toSet());
        assertThat(numbers).containsExactlyInAnyOrderElementsOf(
                IntStream.rangeClosed(1, 8).boxed().toList());
    }

    /**
     * Tipărirea cade după ce numărul a fost ales. Tranzacția se anulează, deci numărul nu rămâne
     * scris pe mișcare și nu se pierde: următoarea tipărire reușită primește tot 1.
     */
    @Test
    void aPrintThatFailsAfterTheNumberWasPickedDoesNotBurnIt() throws Exception {
        UUID id = UUID.fromString(handover(a, "5"));
        doThrow(new IllegalStateException("fontul lipsește"))
                .doCallRealMethod()
                .when(anexa3FormGenerator).render(any(WasteMovement.class), any(Company.class));

        TenantContext.set(a.companyId());
        assertThatThrownBy(() -> documentService.renderAnexa3(id)).isInstanceOf(IllegalStateException.class);
        assertThat(movementRepository.findById(id).orElseThrow().getAnexa3Number()).isNull();

        documentService.renderAnexa3(id);
        assertThat(movementRepository.findById(id).orElseThrow().getAnexa3Number()).isEqualTo(1);
    }

    // ---------- G14: cântărirea venită ulterior ----------

    /**
     * 0 kg înapoi de la cântar nu e o cântărire: încărcătura a plecat, deci a cântărit ceva.
     * Refuz, iar mișcarea rămâne „de cântărit”, nu închisă pe zero.
     */
    @Test
    void aZeroWeightIsRefusedAndTheLoadStaysUnweighed() throws Exception {
        String id = unweighedHandover(a);

        for (String quantity : new String[]{"0", "0.000", "-0.001"}) {
            mockMvc.perform(post("/api/v1/movements/" + id + "/weight")
                            .header("Authorization", "Bearer " + a.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\": " + quantity + "}"))
                    .andExpect(status().isUnprocessableEntity());
        }

        assertThat(movementRepository.findById(UUID.fromString(id)).orElseThrow().getQuantity()).isNull();
    }

    // ---------- G26: texte libere prea lungi ----------

    /**
     * {@code notes} e {@code VARCHAR(1000)}, {@code document_reference} e {@code VARCHAR(255)}.
     * O valoare mai lungă e o greșeală a clientului: așteptat 422, cum răspunde deja ruta de
     * șoferi ({@code ApiErrorContractIT#aTooLongFieldIsUnprocessableOnTheDirectRoute}), și nimic
     * scris. Azi cererea trece de controller, se oprește în coloană și iese 500 cu alertă Sentry.
     */
    @Test
    void aTooLongNoteOrDocumentReferenceIsUnprocessableAndNothingIsWritten() throws Exception {
        long before = movementRepository.findAllByCompany_IdAndDeletedFalse(a.companyId()).size();

        postMovement(a, handoverJson(a, "5", ", \"notes\": \"" + "n".repeat(1001) + "\""))
                .andExpect(status().isUnprocessableEntity());
        postMovement(a, handoverJson(a, "5", ", \"documentReference\": \"" + "D".repeat(256) + "\""))
                .andExpect(status().isUnprocessableEntity());

        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(a.companyId())).hasSize((int) before);
    }

    /** Controlul: exact la limită, 1000 și 255 de semne, se scrie. */
    @Test
    void aNoteAndAReferenceExactlyAtTheColumnLengthAreAccepted() throws Exception {
        postMovement(a, handoverJson(a, "5", ", \"notes\": \"" + "n".repeat(1000)
                + "\", \"documentReference\": \"" + "D".repeat(255) + "\""))
                .andExpect(status().isOk());
    }

    // ---------- G13 + G24: referințe din alt tenant în corpul cererii ----------

    /** Transportatorul tipărit pe Anexa 3, luat din partenerii firmei B. */
    @Test
    void aCarrierFromAnotherCompanyIsRefusedAndNothingIsWritten() throws Exception {
        long before = count(a);

        ResultActions result = postMovement(a, handoverJson(a, "5",
                ", \"transportPartnerId\": \"" + b.partner() + "\""));

        assertRefused(result);
        assertThat(count(a)).isEqualTo(before);
    }

    /** Punctul de lucru al destinatarului — adresa de descărcare de pe Anexa 3 — al unui partener din B. */
    @Test
    void aRecipientWorkPointFromAnotherCompanyIsRefusedAndNothingIsWritten() throws Exception {
        long before = count(a);

        ResultActions result = postMovement(a, handoverJson(a, "5",
                ", \"partnerWorkPointId\": \"" + b.partnerWorkPoint() + "\""));

        assertRefused(result);
        assertThat(count(a)).isEqualTo(before);
    }

    /** Un cod de deșeu care nu există în nomenclator. */
    @Test
    void anUnknownWasteCodeIsRefused() throws Exception {
        String body = handoverJson(a, "5", "").replace(paper.toString(), UUID.randomUUID().toString());
        assertRefused(postMovement(a, body));
    }

    /** Aceleași două atacuri pe {@code PUT}: mișcarea existentă rămâne cum era. */
    @Test
    void anEditCannotPointAnExistingMovementAtAnotherCompanysCarrierOrDepot() throws Exception {
        String id = handover(a, "5");

        for (String extra : new String[]{
                ", \"transportPartnerId\": \"" + b.partner() + "\"",
                ", \"partnerWorkPointId\": \"" + b.partnerWorkPoint() + "\""}) {
            assertRefused(mockMvc.perform(put("/api/v1/movements/" + id)
                    .header("Authorization", "Bearer " + a.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(handoverJson(a, "7", extra))));
        }

        WasteMovement stored = movementRepository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(stored.getQuantity()).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("5"));
    }

    /** Controlul pozitiv: transportatorul și depozitul firmei proprii trec. */
    @Test
    void theOwnCompanysCarrierAndDepotAreAccepted() throws Exception {
        postMovement(a, handoverJson(a, "5", ", \"transportPartnerId\": \"" + a.partner()
                + "\", \"partnerWorkPointId\": \"" + a.partnerWorkPoint() + "\""))
                .andExpect(status().isOk());
    }

    // --- helpers ---

    private static void assertRefused(ResultActions result) {
        int code = result.andReturn().getResponse().getStatus();
        assertThat(code).as("un refuz de client, nu 2xx și nu 500").isBetween(400, 499);
    }

    private long count(Tenant t) {
        return movementRepository.findAllByCompany_IdAndDeletedFalse(t.companyId()).size();
    }

    private String handover(Tenant t, String kg) throws Exception {
        String json = postMovement(t, handoverJson(t, kg, ""))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String unweighedHandover(Tenant t) throws Exception {
        String body = """
                {"workPointId": "%s", "date": "2026-07-05", "wasteCodeId": "%s",
                 "unit": "KG", "physicalState": "SOLID", "weighedAtUnloading": true,
                 "operation": "RECOVERED", "register": "ANEXA_1", "wasteDestination": "Vr",
                 "operationCode": "R13", "partnerId": "%s"}
                """.formatted(t.workPoint(), paper, t.partner());
        String json = postMovement(t, body).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private ResultActions postMovement(Tenant t, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/movements")
                .header("Authorization", "Bearer " + t.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String handoverJson(Tenant t, String kg, String extra) {
        return """
                {"workPointId": "%s", "date": "2026-07-05", "wasteCodeId": "%s", "quantity": %s,
                 "unit": "KG", "physicalState": "SOLID", "operation": "RECOVERED", "register": "ANEXA_1",
                 "wasteDestination": "Vr", "operationCode": "R13", "partnerId": "%s"%s}
                """.formatted(t.workPoint(), paper, kg, t.partner(), extra);
    }
}
