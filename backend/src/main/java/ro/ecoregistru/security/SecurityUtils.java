package ro.ecoregistru.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.exception.BusinessException;

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
}
