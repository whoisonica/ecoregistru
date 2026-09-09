package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0.2 — who is allowed to call this API from a browser.
 *
 * <p>Until 09.09.2026 the answer was „anyone": {@code Access-Control-Allow-Origin: *} on an API
 * that authenticates with a bearer header. The comment in {@code CorsConfig} had said since the
 * first day to tighten it „before opening the product publicly"; these tests are what keeps it
 * tight afterwards, because nothing else would notice a {@code *} coming back.
 *
 * <p>The allow-list under the {@code dev} profile is the two spellings of the Vite server, so
 * {@code http://localhost:5173} stands in here for „the frontend" and everything else for „a page
 * on the internet".
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class CorsIT {

    private static final String FRONTEND = "http://localhost:5173";
    private static final String STRANGER = "https://exemplu-strain.ro";

    @Autowired MockMvc mockMvc;

    @Test
    void theFrontendOriginIsEchoedBack() throws Exception {
        mockMvc.perform(get("/actuator/health").header("Origin", FRONTEND))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND))
                .andExpect(header().string("Vary", containsString("Origin")));
    }

    /** The whole point: no header at all, so the browser drops the response it already fetched. */
    @Test
    void aStrangeOriginGetsNoAllowHeader() throws Exception {
        mockMvc.perform(get("/actuator/health").header("Origin", STRANGER))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    /** The regression that matters: a wildcard would let every origin above through again. */
    @Test
    void noOriginIsEverAnsweredWithAWildcard() throws Exception {
        mockMvc.perform(get("/actuator/health").header("Origin", STRANGER))
                .andExpect(header().string("Access-Control-Allow-Origin", (String) null));
        mockMvc.perform(get("/actuator/health").header("Origin", FRONTEND))
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND));
    }

    @Test
    void preflightPassesForTheFrontendAndIsRefusedForTheRest() throws Exception {
        mockMvc.perform(options("/api/v1/work-points")
                        .header("Origin", FRONTEND)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("X-Tenant-Id")));

        mockMvc.perform(options("/api/v1/work-points")
                        .header("Origin", STRANGER)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    /**
     * curl, the health probe, a server-to-server call: not a CORS request, and it must not become
     * one. Answering these with CORS headers is how a „fix" quietly re-opens the door.
     */
    @Test
    void aRequestWithoutAnOriginIsUntouched() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
