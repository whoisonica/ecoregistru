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
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.repository.AppUserRepository;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

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

    /**
     * P0.4. Un token nu e o legitimaţie citită, e una <b>verificată</b>: acelaşi corp, semnat cu
     * altă cheie, e o hârtie scrisă de altcineva. Proba contează fiindcă eşecul ei nu s-ar vedea —
     * o bibliotecă JWT configurată prost (algoritmul citit din antetul tokenului, {@code alg: none}
     * acceptat) trece testul „expirat" şi pe cel „stricat", şi cade numai aici.
     */
    @Test
    void aTokenSignedWithAnotherKeyIsRejected() throws Exception {
        // O cheie străină, de mărimea cerută de HS256, care nu e a noastră.
        SecretKey foreign = Keys.hmacShaKeyFor(
                "o-cheie-cu-totul-strains-de-32-de-octeti".getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder()
                .subject("admin@demo.ro")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(foreign)
                .compact();

        mockMvc.perform(get("/api/v1/work-points")
                        .header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$['error-code']", is("session.expired")));
    }

    /**
     * Atacul care ar conta dacă semnătura n-ar fi verificată: iau tokenul meu bun, îi rescriu
     * <b>numai</b> încărcătura ca să numească contul de platformă, şi las semnătura veche
     * neatinsă. E o escaladare de la ADMIN într-o firmă la {@code PLATFORM_ADMIN} peste tot,
     * scrisă cu un editor de text.
     */
    @Test
    void aPayloadEditedToNameSomebodyElseIsRejected() throws Exception {
        String mine = jwtService.generateToken(
                appUserRepository.findByEmail("admin@demo.ro").orElseThrow());
        String[] parts = mine.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        String claims = new String(decoder.decode(parts[1]), StandardCharsets.UTF_8)
                .replace("admin@demo.ro", "platform@ecoregistru.ro");
        String tampered = parts[0] + "."
                + encoder.encodeToString(claims.getBytes(StandardCharsets.UTF_8)) + "."
                + parts[2];

        mockMvc.perform(get("/api/v1/work-points")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$['error-code']", is("session.expired")));
    }

    /**
     * Rândul dispare de sub sesiune. O invitaţie anulată chiar se şterge — e singurul „remove" din
     * aplicaţie care nu e soft delete — deci întrebarea nu e teoretică: ce se întâmplă cu un token
     * care numeşte un cont inexistent. Trebuie să fie 401, nu 500 din căutarea care nu găseşte.
     */
    @Test
    void aTokenForAnAccountThatNoLongerExistsIsUnauthorized() throws Exception {
        AppUser admin = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser doomed = appUserRepository.save(AppUser.builder()
                .email("sters+" + suffix + "@demo.ro").password("x")
                .role(admin.getRole()).company(admin.getCompany()).enabled(true)
                .createdAt(java.time.Instant.now()).build());
        String token = jwtService.generateToken(doomed);

        // Sesiunea e bună cât timp contul există...
        mockMvc.perform(get("/api/v1/work-points")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        appUserRepository.delete(doomed);

        // ...şi se închide în clipa în care rândul nu mai e.
        mockMvc.perform(get("/api/v1/work-points")
                        .header("Authorization", "Bearer " + token))
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
