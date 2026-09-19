package ro.ecoregistru;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.*;
import ro.ecoregistru.entity.*;
import ro.ecoregistru.enums.*;
import ro.ecoregistru.repository.*;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * QA de lansare, generator — Faza 3: intrările absurde pe rutele de scriere din scope.
 *
 * <p>Pentru fiecare rută: un corp valid (controlul: 2xx), apoi <b>fiecare</b> câmp text, găsit prin
 * reflecție pe înregistrarea din {@code controller/request}, primește 100.000 de semne. Nicio
 * coloană din scope nu e mai lungă de 2000, deci răspunsul corect e un refuz de client (4xx).
 * Eșecul e orice 5xx: o greșeală a clientului ieșită ca defect de server, cu alertă Sentry.
 * Testul strânge toate câmpurile care cad și le numește pe toate, nu doar pe primul.
 *
 * <p>Plus cantitățile: null, negativ, zero, 1e15, mai multe zecimale decât coloana.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class LaunchSurfaceInputIT {

    private static final String HUGE = "x".repeat(100_000);
    private static final AtomicInteger IP = new AtomicInteger(10);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CompanyRepository companyRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired WorkPointRepository workPointRepository;
    @Autowired WasteCodeRepository wasteCodeRepository;
    @Autowired PartnerRepository partnerRepository;
    @Autowired WasteMovementRepository movementRepository;
    @Autowired ReportingDeadlineRepository deadlineRepository;
    @Autowired AccountRequestRepository accountRequestRepository;

    private Company company;
    private String admin;
    private String platform;
    private UUID workPoint;
    private UUID partner;
    private UUID paper;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        company = companyRepository.save(Company.builder()
                .name("Intrări SRL").cui(TestCui.random())
                .type(CompanyType.GENERATOR).active(true).createdAt(Instant.now()).build());
        admin = jwtService.generateToken(appUserRepository.save(AppUser.builder()
                .email("intrari+" + suffix + "@demo.ro")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ADMIN).company(company).enabled(true).createdAt(Instant.now()).build()));
        platform = jwtService.generateToken(appUserRepository.findByEmail("platform@ecoregistru.ro").orElseThrow());
        workPoint = workPointRepository.save(WorkPoint.builder()
                .company(company).name("PL Intrări").active(true).createdAt(Instant.now()).build()).getId();
        partner = partnerRepository.save(Partner.builder()
                .company(company).name("Colector Intrări SRL").authorizationNumber("AM 5/2025")
                .type(PartnerType.COLLECTOR).supplier(true).active(true).createdAt(Instant.now()).build()).getId();
        paper = wasteCodeRepository.findByCode("20 01 01").orElseThrow().getId();
    }

    // ---------- 100.000 de semne în fiecare câmp text ----------

    @Test
    void movementTextFields() throws Exception {
        assertNoServerErrors(HttpMethod.POST, "/api/v1/movements", admin, this::movement,
                stringFields(WasteMovementRequest.class));
    }

    @Test
    void partnerTextFields() throws Exception {
        assertNoServerErrors(HttpMethod.POST, "/api/v1/partners", admin, this::partnerBody,
                stringFields(PartnerRequest.class));
    }

    @Test
    void partnerNestedWorkPointAndDriverFields() throws Exception {
        List<String> nested = new ArrayList<>();
        stringFields(PartnerWorkPointRequest.class).forEach(f -> nested.add("workPoints[0]." + f));
        stringFields(DriverRequest.class).forEach(f -> nested.add("drivers[0]." + f));
        assertNoServerErrors(HttpMethod.POST, "/api/v1/partners", admin, () -> {
            Map<String, Object> body = partnerBody();
            body.put("workPoints", List.of(new LinkedHashMap<>(Map.of("name", "Depozit", "address", "Str. 1"))));
            body.put("drivers", List.of(new LinkedHashMap<>(Map.of("name", "Ion Pop", "identification", "CJ 1"))));
            return body;
        }, nested);
    }

    @Test
    void workPointTextFields() throws Exception {
        assertNoServerErrors(HttpMethod.POST, "/api/v1/work-points", admin,
                () -> new LinkedHashMap<>(Map.of("name", "PL " + UUID.randomUUID(), "address", "Str. 2")),
                stringFields(WorkPointRequest.class));
    }

    @Test
    void internalGeneratorTextFields() throws Exception {
        assertNoServerErrors(HttpMethod.POST, "/api/v1/internal-generators", admin,
                () -> new LinkedHashMap<>(Map.of("workPointId", workPoint.toString(),
                        "name", "Secția " + UUID.randomUUID(), "description", "vopsitorie")),
                stringFields(InternalGeneratorRequest.class));
    }

    @Test
    void driverTextFields() throws Exception {
        assertNoServerErrors(HttpMethod.POST, "/api/v1/drivers", admin,
                () -> new LinkedHashMap<>(Map.of("name", "Șofer " + UUID.randomUUID(), "identification", "CJ 2")),
                stringFields(DriverRequest.class));
    }

    @Test
    void inviteTextFields() throws Exception {
        assertNoServerErrors(HttpMethod.POST, "/api/v1/users", admin,
                () -> new LinkedHashMap<>(Map.of("email", "inv+" + UUID.randomUUID().toString().substring(0, 8) + "@client.ro",
                        "role", "OPERATOR", "firstName", "Ana", "lastName", "Pop")),
                stringFields(InviteUserRequest.class));
    }

    @Test
    void companyProfileTextFields() throws Exception {
        assertNoServerErrors(HttpMethod.PUT, "/api/v1/companies/" + company.getId(), platform,
                () -> new LinkedHashMap<>(Map.of("name", "Intrări SRL", "cui", company.getCui(), "type", "GENERATOR")),
                stringFields(CompanyRequest.class));
    }

    /** Ruta publică: formularul „Cere cont”, fără autentificare. */
    @Test
    void accountRequestTextFields() throws Exception {
        assertNoServerErrors(HttpMethod.POST, "/api/v1/account-requests", null,
                () -> new LinkedHashMap<>(Map.of("companyName", "Cerere SRL", "cui", TestCui.random(),
                        "companyType", "GENERATOR",
                        "contactEmail", "cerere+" + UUID.randomUUID().toString().substring(0, 8) + "@client.ro")),
                stringFields(AccountRequestSubmission.class));
    }

    @Test
    void deadlineNoteTextField() throws Exception {
        assertNoServerErrors(HttpMethod.POST, null, admin, () -> new LinkedHashMap<>(Map.of("note", "depus")),
                List.of("note"));
    }

    // ---------- cantitățile ----------

    /**
     * {@code quantity} e NUMERIC(14,3): cel mult 99.999.999.999,999. 1e15 nu încape; 1.0005 are o
     * zecimală mai mult decât coloana. Fiecare trebuie refuzat cu 4xx sau, dacă e primit, păstrat
     * exact — niciodată rotunjit tăcut și niciodată 500.
     */
    @Test
    void absurdQuantitiesAreRefusedOrKeptExactly() throws Exception {
        List<String> failures = new ArrayList<>();
        for (Object quantity : new Object[]{-1, 0, 1e15, new BigDecimal("1000000000000000"), new BigDecimal("1.0005"),
                new BigDecimal("0.0001")}) {
            Map<String, Object> body = movement();
            body.put("quantity", quantity);
            int status = send(HttpMethod.POST, "/api/v1/movements", admin, body);
            if (status >= 500) {
                failures.add("quantity=" + quantity + " → " + status);
            } else if (status < 300) {
                BigDecimal stored = movementRepository.findAllByCompany_IdAndDeletedFalse(company.getId()).stream()
                        .map(WasteMovement::getQuantity).filter(Objects::nonNull)
                        .filter(q -> q.compareTo(new BigDecimal(quantity.toString())) != 0)
                        .findAny().orElse(null);
                if (stored != null) {
                    failures.add("quantity=" + quantity + " → " + status + ", păstrat " + stored.toPlainString());
                }
                movementRepository.deleteAll(movementRepository.findAllByCompany_IdAndDeletedFalse(company.getId()));
            }
        }
        Map<String, Object> volume = movement();
        volume.put("volumeM3", 1e15);
        int status = send(HttpMethod.POST, "/api/v1/movements", admin, volume);
        if (status >= 500) failures.add("volumeM3=1e15 → " + status);
        assertThat(failures).isEmpty();
    }

    @Test
    void aMissingQuantityOnAWeighedLoadIsRefused() throws Exception {
        Map<String, Object> body = movement();
        body.remove("quantity");
        assertThat(send(HttpMethod.POST, "/api/v1/movements", admin, body)).isBetween(400, 499);
        assertThat(movementRepository.findAllByCompany_IdAndDeletedFalse(company.getId())).isEmpty();
    }

    /** Cântărirea venită ulterior ({@code RecordWeightRequest}): aceleași valori absurde, niciun 5xx. */
    @Test
    void absurdLaterWeightsAreRefusedOrKeptExactly() throws Exception {
        List<String> failures = new ArrayList<>();
        for (Object quantity : new Object[]{null, -1, 0, 1e15, new BigDecimal("0.0001")}) {
            Map<String, Object> load = movement();
            load.remove("quantity");
            load.put("weighedAtUnloading", true);
            assertThat(send(HttpMethod.POST, "/api/v1/movements", admin, load)).isBetween(200, 299);
            UUID id = movementRepository.findAllByCompany_IdAndDeletedFalse(company.getId()).stream()
                    .filter(m -> m.getQuantity() == null).findFirst().orElseThrow().getId();
            Map<String, Object> weight = new HashMap<>();
            weight.put("quantity", quantity);
            int status = send(HttpMethod.POST, "/api/v1/movements/" + id + "/weight", admin, weight);
            BigDecimal stored = movementRepository.findById(id).orElseThrow().getQuantity();
            if (status >= 500) {
                failures.add("quantity=" + quantity + " → " + status);
            } else if (status < 300 && (quantity == null || stored == null
                    || stored.compareTo(new BigDecimal(quantity.toString())) != 0)) {
                failures.add("quantity=" + quantity + " → " + status + ", păstrat " + stored);
            }
            movementRepository.deleteAll(movementRepository.findAllByCompany_IdAndDeletedFalse(company.getId()));
        }
        assertThat(failures).isEmpty();
    }

    // ---------- ce a mai găsit inventarul (RequestSizeInventoryIT) ----------

    /** 64 + 1 + 192 + 3 = 260 de semne: trece de {@code @Email}, nu încape în VARCHAR(255). */
    private static String longValidEmail() {
        String label = "d".repeat(63);
        return "l".repeat(64) + "@" + label + "." + label + "." + label + ".ro" + UUID.randomUUID().toString().charAt(0);
    }

    @Test
    void aLongButValidInvitationEmailIsNotAServerError() throws Exception {
        int status = send(HttpMethod.POST, "/api/v1/users", admin, new LinkedHashMap<>(Map.of(
                "email", longValidEmail(), "role", "OPERATOR", "firstName", "Ana", "lastName", "Pop")));
        assertThat(status).as("email valid de 260 de semne la invitație").isBetween(400, 499);
    }

    @Test
    void aLongButValidContactEmailOnTheAccountRequestIsNotAServerError() throws Exception {
        int status = send(HttpMethod.POST, "/api/v1/account-requests", null, new LinkedHashMap<>(Map.of(
                "companyName", "Cerere SRL", "cui", TestCui.random(), "companyType", "GENERATOR",
                "contactEmail", longValidEmail())));
        assertThat(status).as("email valid de 260 de semne pe ruta publică").isBetween(400, 499);
    }

    /** Motivul se lipește la notele cererii („Respins: …”), deci și un motiv scurt poate depăși coloana. */
    @Test
    void aRejectionReasonOnTopOfLongNotesIsNotAServerError() throws Exception {
        String cui = TestCui.random();
        assertThat(send(HttpMethod.POST, "/api/v1/account-requests", null, new LinkedHashMap<>(Map.of(
                "companyName", "Respinsă SRL", "cui", cui, "companyType", "GENERATOR",
                "contactEmail", "resp+" + UUID.randomUUID().toString().substring(0, 8) + "@client.ro",
                "notes", "n".repeat(1990))))).isBetween(200, 299);
        UUID id = accountRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(r -> cui.equals(r.getCui())).findFirst().orElseThrow().getId();
        int status = send(HttpMethod.POST, "/api/v1/account-requests/" + id + "/reject", platform,
                new LinkedHashMap<>(Map.of("reason", "firma nu e generator")));
        assertThat(status).as("respingere cu notele aproape pline").isLessThan(500);
    }

    // --- mecanica ---

    private interface Body {
        Map<String, Object> get() throws Exception;
    }

    private void assertNoServerErrors(HttpMethod method, String url, String token, Body base, List<String> fields)
            throws Exception {
        String target = url != null ? url : "/api/v1/deadlines/" + freshDeadline() + "/complete";
        assertThat(send(method, target, token, base.get())).as("controlul: corpul valid trece").isBetween(200, 299);

        List<String> failures = new ArrayList<>();
        for (String field : fields) {
            Map<String, Object> body = base.get();
            put(body, field, HUGE);
            String t = url != null ? url : "/api/v1/deadlines/" + freshDeadline() + "/complete";
            int status = send(method, t, token, body);
            if (status >= 500) {
                failures.add(field + " → " + status);
            }
        }
        assertThat(failures).as("câmpuri de 100.000 de semne care dau 5xx").isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static void put(Map<String, Object> body, String path, Object value) {
        if (path.contains("[0].")) {
            String list = path.substring(0, path.indexOf('['));
            String field = path.substring(path.indexOf("].") + 2);
            ((Map<String, Object>) ((List<?>) body.get(list)).get(0)).put(field, value);
        } else {
            body.put(path, value);
        }
    }

    private int send(HttpMethod method, String url, String token, Map<String, Object> body) throws Exception {
        String ip = "10.9." + (IP.get() / 250) + "." + (IP.getAndIncrement() % 250 + 1);
        MockHttpServletRequestBuilder req = request(method, url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
                .with(r -> { r.setRemoteAddr(ip); return r; });
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(req).andReturn().getResponse().getStatus();
    }

    private static List<String> stringFields(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents())
                .filter(c -> c.getType() == String.class).map(RecordComponent::getName).toList();
    }

    private Map<String, Object> movement() {
        return new LinkedHashMap<>(Map.of("workPointId", workPoint.toString(), "date", "2026-07-05",
                "wasteCodeId", paper.toString(), "quantity", 5, "unit", "KG", "physicalState", "SOLID",
                "operation", "RECOVERED", "wasteDestination", "Vr", "operationCode", "R13",
                "partnerId", partner.toString()));
    }

    private Map<String, Object> partnerBody() {
        return new LinkedHashMap<>(Map.of("name", "Partener " + UUID.randomUUID(), "type", "COLLECTOR",
                "supplier", true, "authorizationNumber", "AM 9/2025"));
    }

    private UUID freshDeadline() {
        return deadlineRepository.save(ReportingDeadline.builder()
                .company(company).reportType(ReportType.SIM_ANNUAL)
                .dueDate(LocalDate.of(2027, 1, 1).plusDays(IP.incrementAndGet())).status(DeadlineStatus.UPCOMING)
                .createdAt(Instant.now()).build()).getId();
    }
}
