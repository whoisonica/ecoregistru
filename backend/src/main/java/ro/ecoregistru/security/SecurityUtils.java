package ro.ecoregistru.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.exception.BusinessException;

import java.util.Set;

import static ro.ecoregistru.exception.ErrorMessageEnum.ACCESS_DENIED;

/**
 * Convenience access to the authenticated principal.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AppUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser user) {
            return user;
        }
        throw new BusinessException(ACCESS_DENIED);
    }

    /** The roles behind the controllers' {@code CAN_WRITE}. */
    private static final Set<String> WRITERS = Set.of("PLATFORM_ADMIN", "CONSULTANT", "ADMIN", "OPERATOR");

    /**
     * BUG-043 — a CNP travels whole only to who can put it on a document (the forms copy it onto
     * the movement, the aviz prints it). A read-only account gets it masked, "190********57":
     * it has nothing to do with the number, and minimisation is the rule for personal data.
     * Outside a request (a job, an export) the value is returned as it is.
     */
    public static String cnpForCurrentUser(String cnp) {
        if (cnp == null || cnp.length() < 6) {
            return cnp;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch(WRITERS::contains)) {
            return cnp;
        }
        return cnp.substring(0, 3) + "*".repeat(cnp.length() - 5) + cnp.substring(cnp.length() - 2);
    }
}
