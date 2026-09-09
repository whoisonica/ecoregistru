package ro.ecoregistru.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

import static ro.ecoregistru.security.RateLimiter.INTAKE_PER_IP;
import static ro.ecoregistru.security.RateLimiter.LOGIN_PER_IP;
import static ro.ecoregistru.security.RateLimiter.RESET_PER_IP;

/**
 * The per-IP half of P0.3, on the three endpoints that answer without a token.
 *
 * <p>It runs before authentication, which is the point: a request that is going to be refused for
 * a wrong password must still cost the caller a token. The per-email half lives in
 * {@code AuthenticationService}, where the email has already been parsed — reading the body twice
 * here would have meant caching every request body in the application to count two of them.
 *
 * <p>Sits after {@code CorsConfig} so a 429 still carries the CORS headers; without them the
 * browser reports „network error" and the frontend cannot show the message.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    /** POST-only: reading the login page is not an attempt at anything. */
    private static final Map<String, RateLimiter.Rule> LIMITED_POSTS = Map.of(
            "/api/v1/auth/login", LOGIN_PER_IP,
            "/api/v1/auth/request-reset-password", RESET_PER_IP,
            "/api/v1/account-requests", INTAKE_PER_IP);

    private final RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        RateLimiter.Rule rule = "POST".equalsIgnoreCase(request.getMethod())
                ? LIMITED_POSTS.get(request.getRequestURI())
                : null;

        if (rule != null) {
            String ip = clientIp(request);
            long retryAfter = rateLimiter.tryConsume(rule, ip);
            if (retryAfter > 0) {
                log.warn("Rate limit {} hit by {} on {}", rule.name(), ip, request.getRequestURI());
                TooManyRequests.write(response, retryAfter);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * The dyno never sees the caller directly — every request arrives from the Heroku router, so
     * {@code getRemoteAddr()} is the same handful of addresses for the whole internet and would
     * make one shared bucket out of all of them.
     *
     * <p>Heroku <em>appends</em> the connecting address to {@code X-Forwarded-For}, so the value we
     * want is the <strong>last</strong> entry, not the first. Trusting the first — the usual
     * reflex — would let a caller send their own header and get a fresh bucket per request.
     * Off Heroku (dev, tests) there is no header and {@code getRemoteAddr()} is the truth.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
