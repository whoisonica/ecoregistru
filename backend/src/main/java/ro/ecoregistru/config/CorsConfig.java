package ro.ecoregistru.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * CORS for the SPA (and future mobile client), on an allow-list rather than {@code *}.
 *
 * <p>Until 09.09.2026 this answered {@code Access-Control-Allow-Origin: *} — an API that takes an
 * {@code Authorization: Bearer} header, reachable from any page on the internet. The list comes
 * from {@code app.cors.allowed-origins}, which defaults to the frontend's own base URL, so
 * production has exactly one entry and no new config var to remember; the dev profile adds the
 * two spellings of the Vite server.
 *
 * <p>An origin is echoed back only if it matches an entry exactly. Anything else gets no
 * {@code Access-Control-Allow-Origin} at all — the browser then refuses the response, which is the
 * point. A request with no {@code Origin} header (curl, the health probe, a server-to-server call)
 * is not a CORS request and passes through untouched.
 *
 * <p>{@code Vary: Origin} is set on every response, including the refusals: without it a shared
 * cache could hand the allowed origin's response — headers and all — to a page from somewhere else.
 */
@Component
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsConfig implements Filter {

    private static final String ALLOWED_METHODS = "POST, GET, DELETE, PATCH, PUT, OPTIONS";
    private static final String ALLOWED_HEADERS =
            "X-Requested-With, Content-Type, Accept, Authorization, Cache-Control, X-Tenant-Id";

    private final Set<String> allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:}") String configured) {
        this.allowedOrigins = parse(configured);
    }

    /** Trailing slashes are the usual way a base URL and an {@code Origin} header stop matching. */
    private static Set<String> parse(String configured) {
        Set<String> origins = new LinkedHashSet<>();
        Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .map(o -> o.endsWith("/") ? o.substring(0, o.length() - 1) : o)
                .forEach(origins::add);
        return origins;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        response.addHeader("Vary", "Origin");

        String origin = request.getHeader("Origin");
        boolean allowed = origin != null && allowedOrigins.contains(origin);

        if (allowed) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        }

        // A preflight never reaches a controller. Answering 200 for an origin we refuse would be a
        // lie the browser cannot read anyway, so it gets 403 — and it shows up as one in the logs.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) && origin != null) {
            response.setStatus(allowed ? HttpServletResponse.SC_OK : HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(req, res);
    }
}
