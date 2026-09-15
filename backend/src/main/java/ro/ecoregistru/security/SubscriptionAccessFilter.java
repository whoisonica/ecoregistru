package ro.ecoregistru.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.enums.SubscriptionStatus;
import ro.ecoregistru.repository.SubscriptionRepository;
import ro.ecoregistru.service.SubscriptionStatusRules;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * F4 of plata-abonamente.md — an account whose subscription is READ_ONLY or CANCELLED reads and downloads
 * everything, and writes nothing (decision 6, §9.3).
 *
 * <ul>
 *   <li><b>Only writes.</b> Every document — the PDFs, the xlsx, the audit-file ZIP — is a GET, checked on
 *       15.09.2026 across every controller; a client who has not paid still takes their records.</li>
 *   <li><b>Who pays decides.</b> A consultant, and a company of a cabinet, are restricted by the cabinet's
 *       subscription; a direct company by its own. A company nobody pays for is not restricted.</li>
 *   <li><b>Never</b> the platform, the login and reset endpoints, paying itself ({@code /api/v1/billing/**})
 *       or the two regenerations, which only recompute documents from records already there.</li>
 *   <li><b>Off</b> unless {@code app.billing.read-only-enabled} (§9.6): the contract must say it first.</li>
 * </ul>
 *
 * <p>Runs after {@link TenantFilter}, which it needs; wired in SecurityConfiguration, not a component.
 */
@Slf4j
public class SubscriptionAccessFilter extends OncePerRequestFilter {

    public static final String ERROR_CODE = "subscription.read_only";
    public static final String MESSAGE = "Contul e doar pentru citire: abonamentul are o factură neplătită de peste "
            + "15 zile sau e oprit. Poți vedea și descărca tot. Plata, din Abonament, redeschide contul imediat.";

    private static final Set<String> READS = Set.of("GET", "HEAD", "OPTIONS");
    private static final List<String> EXEMPT_PREFIXES = List.of("/api/v1/billing/", "/api/v1/auth/");
    private static final Set<String> EXEMPT_PATHS = Set.of(
            "/api/v1/billing", "/api/v1/evidences/regenerate", "/api/v1/deadlines/regenerate");

    private final SubscriptionRepository subscriptionRepository;
    private final boolean enabled;

    public SubscriptionAccessFilter(SubscriptionRepository subscriptionRepository, boolean enabled) {
        this.subscriptionRepository = subscriptionRepository;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {
        if (enabled && !READS.contains(request.getMethod().toUpperCase()) && !exempt(request.getRequestURI())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AppUser user && user.getRole() != Role.PLATFORM_ADMIN
                    && statusesFor(user).stream().anyMatch(SubscriptionStatusRules::restricts)) {
                log.info("Scriere refuzată, abonament în doar-citire: {} {} ({})",
                        request.getMethod(), request.getRequestURI(), user.getId());
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("""
                        {"error-type":"access-denied","error-code":"%s","error-message":"%s"}"""
                        .formatted(ERROR_CODE, MESSAGE));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private List<SubscriptionStatus> statusesFor(AppUser user) {
        if (user.getRole() == Role.CONSULTANT) {
            return user.getConsultancy() == null ? List.of()
                    : subscriptionRepository.findStatusOfConsultancy(user.getConsultancy().getId());
        }
        UUID companyId = TenantContext.get() != null ? TenantContext.get()
                : user.getCompany() != null ? user.getCompany().getId() : null;
        return companyId == null ? List.of() : subscriptionRepository.findStatusPayingFor(companyId);
    }

    private static boolean exempt(String uri) {
        return EXEMPT_PATHS.contains(uri) || EXEMPT_PREFIXES.stream().anyMatch(uri::startsWith);
    }
}
