package ro.ecoregistru.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.enums.Role;

import java.io.IOException;
import java.util.UUID;

/**
 * Resolves the effective tenant for the request and stores it in {@link TenantContext}.
 *
 * Runs AFTER {@link ro.ecoregistru.config.JwtAuthenticationFilter} (wired in SecurityConfiguration).
 * Not a @Component on purpose, to avoid double registration as a global servlet filter.
 */
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AppUser user) {
                if (user.getRole() == Role.PLATFORM_ADMIN) {
                    resolvePlatformAdminTenant(request);
                } else if (user.getCompany() != null) {
                    TenantContext.set(user.getCompany().getId());
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void resolvePlatformAdminTenant(HttpServletRequest request) {
        String header = request.getHeader(TENANT_HEADER);
        if (header != null && !header.isBlank()) {
            try {
                TenantContext.set(UUID.fromString(header.trim()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid {} header: {}", TENANT_HEADER, header);
            }
        }
    }
}
