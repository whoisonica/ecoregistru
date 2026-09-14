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
import ro.ecoregistru.repository.CompanyRepository;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the effective tenant for the request and stores it in {@link TenantContext}.
 *
 * <ul>
 *   <li>{@code PLATFORM_ADMIN} — whatever company the {@code X-Tenant-Id} header names.</li>
 *   <li>{@code CONSULTANT} — the company the header names, <b>only if their consultancy manages
 *       it</b>. Anything else leaves the request without a tenant, which every scoped endpoint
 *       answers with {@code tenant.required}. A company of another consultancy and an id that
 *       exists nowhere get that same answer, so the header cannot be used to learn which ids are
 *       real.</li>
 *   <li>everyone else — their own company; the header is ignored.</li>
 * </ul>
 *
 * Runs AFTER {@link ro.ecoregistru.config.JwtAuthenticationFilter} (wired in SecurityConfiguration).
 * Not a @Component on purpose, to avoid double registration as a global servlet filter.
 */
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    private final CompanyRepository companyRepository;

    public TenantFilter(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AppUser user) {
                if (user.getRole() == Role.PLATFORM_ADMIN) {
                    headerTenant(request).ifPresent(TenantContext::set);
                } else if (user.getRole() == Role.CONSULTANT) {
                    resolveConsultantTenant(request, user);
                } else if (user.getCompany() != null) {
                    TenantContext.set(user.getCompany().getId());
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * P2.13 — the one check that keeps a consultancy out of another consultancy's clients. One
     * indexed query per request; without it the header would be exactly the platform admin's.
     */
    private void resolveConsultantTenant(HttpServletRequest request, AppUser user) {
        if (user.getConsultancy() == null) {
            return;
        }
        UUID consultancyId = user.getConsultancy().getId();
        headerTenant(request)
                .filter(id -> companyRepository.existsByIdAndConsultancy_Id(id, consultancyId))
                .ifPresent(TenantContext::set);
    }

    private Optional<UUID> headerTenant(HttpServletRequest request) {
        String header = request.getHeader(TENANT_HEADER);
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(header.trim()));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid {} header: {}", TENANT_HEADER, header);
            return Optional.empty();
        }
    }
}
