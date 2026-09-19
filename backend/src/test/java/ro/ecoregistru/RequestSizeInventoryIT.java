package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ro.ecoregistru.controller.request.*;

import java.lang.reflect.RecordComponent;
import java.util.*;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA de lansare, generator — Faza 3: inventarul prin reflecție.
 *
 * <p>Pentru fiecare înregistrare din {@code controller/request} folosită de scope, fiecare câmp
 * {@code String} care ajunge într-o coloană trebuie să aibă {@code @Size(max ≤ lungimea coloanei)},
 * citită din {@code information_schema} pe baza migrată. Coloana se găsește după numele câmpului în
 * snake_case, sau din {@link #RENAMED} unde entitatea îl scrie sub alt nume. Câmpurile care nu ajung
 * într-o coloană (parole, coduri, căutări) sunt numite în {@link #NOT_STORED}: un câmp nou care nu e
 * nici coloană, nici pe listă, pică testul, ca inventarul să nu rămână în urmă.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class RequestSizeInventoryIT {

    /** DTO-urile din scope și tabelul în care scriu. */
    private static final Map<Class<? extends Record>, String> SCOPE = new LinkedHashMap<>();
    static {
        SCOPE.put(WasteMovementRequest.class, "waste_movements");
        SCOPE.put(PartnerRequest.class, "partners");
        SCOPE.put(PartnerWorkPointRequest.class, "partner_work_points");
        SCOPE.put(DriverRequest.class, "drivers");
        SCOPE.put(WorkPointRequest.class, "work_points");
        SCOPE.put(InternalGeneratorRequest.class, "internal_generators");
        SCOPE.put(InviteUserRequest.class, "app_users");
        SCOPE.put(CompanyRequest.class, "companies");
        SCOPE.put(AccountRequestSubmission.class, "account_requests");
        SCOPE.put(CompleteDeadlineRequest.class, "reporting_deadlines");
        SCOPE.put(RejectAccountRequest.class, "account_requests");
        // BUG-054 (19.09.2026): restul cererilor care scriu text, nu doar primele unsprezece.
        SCOPE.put(InviteConsultantRequest.class, "app_users");
        SCOPE.put(OnboardClientRequest.Admin.class, "app_users");
        SCOPE.put(ConsultancyRequest.class, "consultancies");
        SCOPE.put(ConsultancyBrandingRequest.class, "consultancy_branding");
        SCOPE.put(WeighingCancelRequest.class, "weighing_operations");
        SCOPE.put(WeighingOperationRequest.class, "weighing_operations");
        SCOPE.put(WeighingLinesRequest.Line.class, "waste_movements");
        SCOPE.put(NaturalPersonRequest.class, "natural_persons");
        SCOPE.put(VehicleRequest.class, "vehicles");
        SCOPE.put(WasteArticleRequest.class, "waste_articles");
        SCOPE.put(BillingDetailsRequest.class, "subscriptions");
        SCOPE.put(PushTokenRequest.class, "device_sessions");
    }

    /** Câmp → coloană, unde numele diferă. Completat după prima rulare, din entități. */
    private static final Map<String, String> RENAMED = Map.of(
            "CompleteDeadlineRequest.note", "completion_note",       // DeadlineService.java:337
            "RejectAccountRequest.reason", "notes",                  // AccountRequestService.reject: „Respins: …” adăugat la notes
            "WeighingCancelRequest.reason", "cancel_reason",
            "PushTokenRequest.token", "push_token");

    /** Câmpuri care nu se scriu într-o coloană. */
    private static final Set<String> NOT_STORED = Set.of(
            "AccountRequestSubmission.website");                     // capcana pentru roboți, nu se salvează

    @Autowired JdbcTemplate jdbc;

    @Test
    void everyStoredStringHasASizeWithinItsColumn() {
        assertThat(violations(false)).as("câmpuri fără @Size sau cu @Size peste coloană").isEmpty();
    }

    @Test
    void noDeclaredSizeIsLargerThanItsColumn() {
        // Controlul: unde @Size există, nu promite mai mult decât încape.
        assertThat(violations(true)).as("@Size peste lungimea coloanei").isEmpty();
    }

    @Test
    void everyStringFieldIsEitherAColumnOrKnownNotToBeStored() {
        List<String> unknown = new ArrayList<>();
        SCOPE.forEach((type, table) -> {
            for (RecordComponent c : strings(type)) {
                String key = type.getSimpleName() + "." + c.getName();
                if (!NOT_STORED.contains(key) && length(table, column(key, c)) == null
                        && !isText(table, column(key, c))) {
                    unknown.add(key + " → " + table + "." + column(key, c) + " nu există");
                }
            }
        });
        assertThat(unknown).as("câmpuri fără coloană, nici pe lista NOT_STORED").isEmpty();
    }

    // --- mecanica ---

    private List<String> violations(boolean onlyDeclared) {
        List<String> out = new ArrayList<>();
        SCOPE.forEach((type, table) -> {
            for (RecordComponent c : strings(type)) {
                String key = type.getSimpleName() + "." + c.getName();
                if (NOT_STORED.contains(key)) {
                    continue;
                }
                Integer max = length(table, column(key, c));
                if (max == null) {
                    continue; // TEXT sau coloană lipsă; a doua o prinde testul de inventar
                }
                Size size = size(type, c);
                if (size == null && boundedElsewhere(type, c)) {
                    continue;
                }
                if (size == null) {
                    if (!onlyDeclared) {
                        out.add(key + ": fără @Size (coloana " + max + ")");
                    }
                } else if (size.max() > max) {
                    out.add(key + ": @Size(max = " + size.max() + ") > coloana " + max);
                }
            }
        });
        return out;
    }

    /** {@code @ValidCnp} primește doar 13 cifre (după trim, iar serviciile salvează tot tăiat). */
    private static boolean boundedElsewhere(Class<?> type, RecordComponent c) {
        try {
            return type.getDeclaredField(c.getName()).isAnnotationPresent(ro.ecoregistru.util.ValidCnp.class)
                    || c.getAccessor().isAnnotationPresent(ro.ecoregistru.util.ValidCnp.class);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<RecordComponent> strings(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents()).filter(c -> c.getType() == String.class).toList();
    }

    private static Size size(Class<?> type, RecordComponent c) {
        try {
            Size onField = type.getDeclaredField(c.getName()).getAnnotation(Size.class);
            return onField != null ? onField : c.getAccessor().getAnnotation(Size.class);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String column(String key, RecordComponent c) {
        return RENAMED.getOrDefault(key, c.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase());
    }

    private Integer length(String table, String column) {
        List<Integer> r = jdbc.queryForList("""
                SELECT character_maximum_length FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?
                  AND character_maximum_length IS NOT NULL""", Integer.class, table, column);
        return r.isEmpty() ? null : r.get(0);
    }

    private boolean isText(String table, String column) {
        return !jdbc.queryForList("""
                SELECT 1 FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?
                  AND data_type = 'text'""", Integer.class, table, column).isEmpty();
    }
}
