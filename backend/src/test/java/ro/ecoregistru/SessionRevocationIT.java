package ro.ecoregistru;

import io.jsonwebtoken.Claims;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.repository.AppUserRepository;

import java.time.Duration;
import java.util.Date;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0.4 — a session that can be taken back.
 *
 * <p>Until 09.09.2026 a token was a thirty-day grant with nothing able to revoke it: no refresh, no
 * blacklist, and the value living in {@code localStorage}. The consequence, in the sentence a
 * client actually asks: <em>disable a user and they keep working for a month.</em>
 *
 * <p>{@link SessionExpiryIT} covers what the client is <em>told</em> when a session ends. This
 * covers when one ends. Between them, the whole of „who is still allowed in".
 */
@SpringBootTest
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class SessionRevocationIT {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired AppUserRepository appUserRepository;

    /** The one named in the TODO: this returned 200 before, for another twenty-nine days. */
    @Test
    void disablingAUserClosesTheirOpenSession() throws Exception {
        AppUser user = appUserRepository.findByEmail("operator@demo.ro").orElseThrow();
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/v1/work-points").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        user.setEnabled(false);
        appUserRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/v1/work-points").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$['error-code']", is("session.expired")));

        user.setEnabled(true);
        appUserRepository.saveAndFlush(user);
    }

    /**
     * What {@code enabled} alone cannot do. Taking your account back by resetting the password
     * leaves it enabled, so nothing about the row would have changed — and the token issued to
     * whoever had it before would have gone on working next to your new password.
     */
    @Test
    void bumpingTheSessionCounterClosesTokensIssuedBefore() throws Exception {
        AppUser user = appUserRepository.findByEmail("viewer@demo.ro").orElseThrow();
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/v1/work-points").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        user.setTokenVersion(user.getTokenVersion() + 1);
        appUserRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/v1/work-points").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        // And the token issued after the bump works, so it is a revocation and not a lockout.
        String reissued = jwtService.generateToken(appUserRepository.findByEmail("viewer@demo.ro").orElseThrow());
        mockMvc.perform(get("/api/v1/work-points").header("Authorization", "Bearer " + reissued))
                .andExpect(status().isOk());
    }

    /**
     * The migration must not have signed everyone out at whatever hour Flyway happened to run.
     * A token from before {@code V32} carries no {@code tv} claim at all, and every existing row
     * was migrated to 0 — so „no claim" has to read as „version 0", not as „mismatch".
     */
    @Test
    void aTokenFromBeforeTheMigrationStillWorks() throws Exception {
        AppUser user = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        assertThat(user.getTokenVersion()).isZero();

        String withoutClaim = jwtService.generateToken(new java.util.HashMap<>(), new PlainUser(user));
        Object claim = jwtService.extractClaim(withoutClaim, c -> c.get(JwtService.TOKEN_VERSION_CLAIM));
        assertThat(claim).as("un token de dinainte de V32 n-are claimul deloc").isNull();

        mockMvc.perform(get("/api/v1/work-points").header("Authorization", "Bearer " + withoutClaim))
                .andExpect(status().isOk());
    }

    /**
     * Eight hours, not thirty days. Checked on the token itself rather than on the constant,
     * because the constant is exactly what a well-meaning „temporary" change would raise back.
     */
    @Test
    void aFreshTokenLastsAWorkingDayAndNotAMonth() {
        AppUser user = appUserRepository.findByEmail("admin@demo.ro").orElseThrow();
        Date expiry = jwtService.extractClaim(jwtService.generateToken(user), Claims::getExpiration);

        Duration life = Duration.between(new Date().toInstant(), expiry.toInstant());
        assertThat(life).isBetween(Duration.ofHours(7), Duration.ofHours(9));
    }

    /**
     * A {@code UserDetails} that is not an {@code AppUser}, so {@code generateToken} leaves the
     * {@code tv} claim off — the shape of every token issued before the migration.
     */
    private record PlainUser(AppUser delegate) implements org.springframework.security.core.userdetails.UserDetails {
        @Override public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
            return delegate.getAuthorities();
        }
        @Override public String getPassword() { return delegate.getPassword(); }
        @Override public String getUsername() { return delegate.getUsername(); }
        @Override public boolean isAccountNonExpired() { return true; }
        @Override public boolean isAccountNonLocked() { return true; }
        @Override public boolean isCredentialsNonExpired() { return true; }
        @Override public boolean isEnabled() { return delegate.isEnabled(); }
    }
}
