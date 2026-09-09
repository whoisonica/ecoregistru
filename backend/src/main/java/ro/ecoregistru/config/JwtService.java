package ro.ecoregistru.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ro.ecoregistru.entity.AppUser;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT issue/verify. The same token shape works for web and (future) mobile clients.
 *
 * <p>P0.4 (09.09.2026) changed two things here, and the reason for both is the same question a
 * client asks before signing: <em>what happens when an employee leaves?</em> Until then the answer
 * was „they keep working for a month".
 */
@Service
public class JwtService {

    /**
     * A working day, not a month. It used to be <b>30 days</b>, with no refresh, no revocation
     * list, and the token sitting in {@code localStorage} — so a laptop left on a train stayed
     * signed in until October. Eight hours means a session outlives a day of work and nothing more;
     * the frontend already sends an expired session back to the login page with „sesiunea a
     * expirat" and the address to return to, so the cost of it running out is one sign-in.
     */
    private static final long TOKEN_VALIDITY_MS = 1000L * 60 * 60 * 8; // 8 hours

    /** The session counter the token was issued with. See {@code AppUser#tokenVersion}. */
    public static final String TOKEN_VERSION_CLAIM = "tv";

    @Value("${app.jwt.secret}")
    private String secretKey;

    public String extractEmail(String jwt) {
        return extractClaim(jwt, Claims::getSubject);
    }

    public <T> T extractClaim(String jwt, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(jwt);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        if (userDetails instanceof AppUser user) {
            claims.put(TOKEN_VERSION_CLAIM, user.getTokenVersion());
        }
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY_MS))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * A token is valid while it names this user, has not expired, the account is still enabled,
     * and it carries the session counter the account is on now.
     *
     * <p>The last two are P0.4. Before them a token was a month-long grant that nothing could take
     * back: disabling an account left every open session working, and resetting a password left
     * the old one working next to the new. Both are checked on every request — the user row is
     * already loaded by {@code JwtAuthenticationFilter}, so this costs nothing extra.
     */
    public boolean isTokenValid(String jwt, UserDetails userDetails) {
        final String email = extractEmail(jwt);
        if (!email.equals(userDetails.getUsername()) || isTokenExpired(jwt)) {
            return false;
        }
        if (!userDetails.isEnabled()) {
            return false;
        }
        if (userDetails instanceof AppUser user) {
            return tokenVersionOf(jwt) == user.getTokenVersion();
        }
        return true;
    }

    /**
     * Tokens issued before {@code V32} carry no {@code tv} claim at all. They are read as version
     * 0 — the value every existing row was migrated to — so the migration did not sign everybody
     * out at whatever hour it happened to run.
     */
    private int tokenVersionOf(String jwt) {
        Integer version = extractClaim(jwt, c -> c.get(TOKEN_VERSION_CLAIM, Integer.class));
        return version == null ? 0 : version;
    }

    private boolean isTokenExpired(String jwt) {
        return extractClaim(jwt, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String jwt) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
