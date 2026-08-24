package ro.ecoregistru;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.repository.AppUserRepository;

import javax.crypto.SecretKey;
import java.util.Date;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What the client is told when a session runs out.
 *
 * <p>This is a contract with the frontend, not an internal detail: its interceptor sends the user
 * to the login page with „sesiunea a expirat" on a 401 and leaves them where they are on a 403.
 * Until 24.08.2026 every one of these cases answered 403 — Spring Security's default entry point
 * — so an expired session ended in an unexplained bounce, with a half-filled form lost. Whoever
 * touches the security chain next: keep 401 here.
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class SessionExpiryIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;

    @Value("${app.jwt.secret}")
    String secret;

    @Test
    void noTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/work-points"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$['error-code']", is("session.expired")));
    }

    /** A token from an old build, a truncated copy-paste, a mangled localStorage value. */
    @Test
    void malformedTokenIsUnauthorizedNotServerError() throws Exception {
        mockMvc.perform(get("/api/v1/work-points")
                        .header("Authorization", "Bearer stale.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$['error-code']", is("session.expired")));
    }

    /** The real case: correctly signed, simply past its expiry. */
    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        long anHourAgo = System.currentTimeMillis() - 3_600_000L;
        String expired = Jwts.builder()
                .subject("admin@demo.ro")
                .issuedAt(new Date(anHourAgo - 1000))
                .expiration(new Date(anHourAgo))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/v1/work-points")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$['error-code']", is("session.expired")));
    }

    /** And a live token still works, so the fix did not lock everyone out. */
    @Test
    void aValidTokenStillPasses() throws Exception {
        String token = jwtService.generateToken(
                appUserRepository.findByEmail("admin@demo.ro").orElseThrow());
        mockMvc.perform(get("/api/v1/work-points")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
