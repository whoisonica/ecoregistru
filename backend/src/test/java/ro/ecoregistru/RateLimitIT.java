package ro.ecoregistru;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0.3 — the brake on the three doors that answer without a token.
 *
 * <p>Before this, all three were open with nothing counting: no lockout, no attempt counter, and a
 * minimum password of eight characters with no other rule. Brute force on {@code /auth/login} was
 * free, {@code /auth/request-reset-password} was a spam cannon pointed through our own Gmail
 * credentials, and {@code /account-requests} — the only public write in the application — could be
 * filled by a script.
 *
 * <p>Each test uses a fresh IP and a fresh address, because the buckets are per key and live for
 * the whole application context: two tests sharing a key would count each other's requests, and
 * whichever ran second would fail for a reason that has nothing to do with what it checks.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
// Its own property gives this class its own application context — which is exactly what it needs:
// the buckets live for the life of the context, so a shared one would carry another test's counts
// in here. It also pins the demo password, which is otherwise generated per boot and only logged.
@TestPropertySource(properties = "app.demo-password=ProbaLimitare1")
class RateLimitIT {

    private static final String DEMO_PASSWORD = "ProbaLimitare1";

    @Autowired MockMvc mockMvc;

    /** A distinct caller per test. The filter reads the last X-Forwarded-For hop (Heroku appends). */
    private static String freshIp() {
        return "203.0.113." + (int) (Math.random() * 250 + 1) + ":" + UUID.randomUUID();
    }

    private static String freshEmail() {
        return "proba-" + UUID.randomUUID() + "@example.ro";
    }

    private MockHttpServletRequestBuilder login(String ip, String email, String password) {
        return post("/api/v1/auth/login")
                .header("X-Forwarded-For", ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    /**
     * The per-IP quota is 60 in five minutes, and it counts every attempt. Sixty go through
     * (as 400s — wrong password), the sixty-first is refused before the controller sees it.
     */
    @Test
    void theSixtyFirstLoginFromOneAddressIsRefused() throws Exception {
        String ip = freshIp();
        for (int i = 0; i < 60; i++) {
            // A different address each time, so only the per-IP bucket is doing the counting.
            mockMvc.perform(login(ip, freshEmail(), "GresitaCuTotul1"))
                    .andExpect(status().isBadRequest());
        }

        MvcResult refused = mockMvc.perform(login(ip, freshEmail(), "GresitaCuTotul1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$['error-code']", is("too.many.requests")))
                .andReturn();

        // Retry-After has to be a real number of seconds, not a placeholder: the frontend shows it.
        long retryAfter = Long.parseLong(refused.getResponse().getHeader("Retry-After"));
        assertThat(retryAfter).isBetween(1L, 300L);
    }

    /**
     * The per-email quota is ten <em>failed</em> attempts in a quarter of an hour. It is the one
     * that survives an attacker rotating IPs — every request below comes from a different address.
     */
    @Test
    void tenWrongPasswordsForOneAccountAreEnough() throws Exception {
        String email = freshEmail();
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(login(freshIp(), email, "GresitaCuTotul1"))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(login(freshIp(), email, "GresitaCuTotul1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    /**
     * And the half that keeps it usable. The e2e suite signs in as the same user about ten times a
     * run, and an office does the same on a Monday morning; if success cost a token, the eleventh
     * honest sign-in would be refused. Twenty in a row, all correct, all 200.
     */
    @Test
    void signingInCorrectlyCostsNothing() throws Exception {
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(login(freshIp(), "admin@demo.ro", DEMO_PASSWORD))
                    .andExpect(status().isOk());
        }
    }

    /** Ten reset mails an hour from one address, then the cannon is unplugged. */
    @Test
    void thePasswordResetCannonRunsOutOfShots() throws Exception {
        String ip = freshIp();
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/request-reset-password")
                            .header("X-Forwarded-For", ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"%s\"}".formatted(freshEmail())))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/auth/request-reset-password")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(freshEmail())))
                .andExpect(status().isTooManyRequests());
    }

    /** Three an hour per address, whether or not the account exists — the endpoint answers 200 either way. */
    @Test
    void oneInboxGetsThreeResetMailsAnHour() throws Exception {
        String email = freshEmail();
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/request-reset-password")
                            .header("X-Forwarded-For", freshIp())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"%s\"}".formatted(email)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/auth/request-reset-password")
                        .header("X-Forwarded-For", freshIp())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isTooManyRequests());
    }

    /** The only public write in the application: ten an hour, then the table stops growing. */
    @Test
    void theIntakeFormStopsAcceptingAfterTen() throws Exception {
        String ip = freshIp();
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(intake(ip)).andExpect(status().is2xxSuccessful());
        }
        mockMvc.perform(intake(ip)).andExpect(status().isTooManyRequests());
    }

    private MockHttpServletRequestBuilder intake(String ip) {
        String suffix = String.valueOf(System.nanoTime()).substring(9);
        return post("/api/v1/account-requests")
                .header("X-Forwarded-For", ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"companyName":"Proba Limitare SRL","cui":"RO%s",\
                        "companyType":"GENERATOR","contactEmail":"%s"}"""
                        .formatted(suffix, freshEmail()));
    }

    /** Reading is not attempting: only POST is counted, so nothing here throttles a page load. */
    @Test
    void aGetOnTheSamePathIsNotCounted() throws Exception {
        String ip = freshIp();
        for (int i = 0; i < 80; i++) {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .get("/api/v1/auth/ping")
                            .header("X-Forwarded-For", ip))
                    .andExpect(status().isOk());
        }
    }
}
