package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import ro.ecoregistru.service.CloudinaryStorageService;

import java.util.*;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * {@code OPTIONS} e lăsat liber pe toată aplicaţia ({@code 4a8bbea}, pentru preflight-ul CORS).
 * Asta e o uşă deschisă fără sesiune pe fiecare rută — deci trebuie probat că <b>nu duce nicăieri</b>:
 * un {@code OPTIONS} nu ajunge la niciun controller, nu scrie nimic şi nu întoarce date.
 *
 * <p>Cum se deosebeşte „a răspuns framework-ul" de „a rulat controllerul": Spring MVC răspunde
 * singur la {@code OPTIONS} pe o rută cunoscută, cu antetul {@code Allow} şi corp gol. Un handler
 * care ar accepta şi {@code OPTIONS} (un {@code @RequestMapping} fără verb) ar scrie un corp. Iar
 * baza de date se compară înainte şi după, pe <b>toate</b> tabelele, după număr de rânduri şi după
 * cea mai nouă versiune de rând ({@code xmin}) — o ştergere moale nu schimbă numărul, dar schimbă
 * versiunea.
 *
 * <p>Rutele se citesc din Spring, nu dintr-o listă scrisă de mână: o rută nouă intră singură sub
 * probă.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class PreflightIT {

    private static final String FRONTEND = "http://localhost:5173";
    private static final String STRANGER = "https://exemplu-strain.ro";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapping;

    @MockBean CloudinaryStorageService storageService;

    private List<String> apiPaths() {
        TreeSet<String> paths = new TreeSet<>();
        mapping.getHandlerMethods().forEach((info, handler) -> {
            if (!ClassUtils.getUserClass(handler.getBeanType()).getPackageName().startsWith("ro.ecoregistru")) return;
            info.getPatternValues().forEach(p -> paths.add(p.replaceAll("\\{[^}]+}", UUID.randomUUID().toString())));
        });
        assertThat(paths).as("rutele aplicaţiei au fost citite").hasSizeGreaterThan(40);
        return List.copyOf(paths);
    }

    /** Număr de rânduri şi cea mai nouă versiune de rând, pe fiecare tabelă. */
    private Map<String, String> snapshot() {
        Map<String, String> state = new TreeMap<>();
        for (String table : jdbc.queryForList(
                "select tablename from pg_tables where schemaname = 'public'", String.class)) {
            state.put(table, jdbc.queryForObject(
                    "select count(*) || ':' || coalesce(max(xmin::text::bigint), 0) from \"" + table + "\"",
                    String.class));
        }
        return state;
    }

    @Test
    void anOptionsWithoutATokenNeverReachesAControllerAndWritesNothing() throws Exception {
        Map<String, String> before = snapshot();
        List<String> problems = new ArrayList<>();

        for (String path : apiPaths()) {
            MockHttpServletResponse r = mockMvc.perform(options(path)).andReturn().getResponse();
            if (r.getStatus() >= 500) problems.add(path + " → " + r.getStatus());
            if (!r.getContentAsString().isEmpty()) problems.add(path + " → corp: " + r.getContentAsString());
            if (r.getStatus() == 200 && r.getHeader("Allow") == null) problems.add(path + " → 200 fără Allow");
        }

        assertThat(problems).isEmpty();
        assertThat(snapshot()).isEqualTo(before);
        verifyNoInteractions(storageService);
    }

    @Test
    void aForeignPreflightIsRefusedOnEveryRouteAndSaysNothing() throws Exception {
        List<String> problems = new ArrayList<>();
        for (String path : apiPaths()) {
            MockHttpServletResponse r = mockMvc.perform(options(path)
                            .header("Origin", STRANGER)
                            .header("Access-Control-Request-Method", "DELETE"))
                    .andReturn().getResponse();
            if (r.getStatus() != 403 || !r.getContentAsString().isEmpty()
                    || r.getHeader("Access-Control-Allow-Origin") != null) {
                problems.add(path + " → " + r.getStatus());
            }
        }
        assertThat(problems).isEmpty();
    }

    @Test
    void theFrontendPreflightGetsHeadersAndNoBody() throws Exception {
        MockHttpServletResponse r = mockMvc.perform(options("/api/v1/partners/" + UUID.randomUUID())
                        .header("Origin", FRONTEND)
                        .header("Access-Control-Request-Method", "DELETE"))
                .andReturn().getResponse();
        assertThat(r.getStatus()).isEqualTo(200);
        assertThat(r.getHeader("Access-Control-Allow-Origin")).isEqualTo(FRONTEND);
        assertThat(r.getContentAsString()).isEmpty();
    }

    /** Uşa deschisă pe {@code OPTIONS} nu se întinde peste verbul pe care îl anunţă preflight-ul. */
    @Test
    void theVerbAPreflightAsksAboutStillNeedsASession() throws Exception {
        mockMvc.perform(options("/api/v1/partners").header("Origin", FRONTEND)
                .header("Access-Control-Request-Method", "POST"));
        int status = mockMvc.perform(post("/api/v1/partners").header("Origin", FRONTEND)
                        .contentType("application/json").content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(401);
    }
}
