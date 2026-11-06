package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.security.SecurityUtils;

import java.time.Instant;

/**
 * The caller's own account, as opposed to {@link UserController}, which manages a firm's colleagues and
 * is closed to everyone below an admin. Its own class so that gate stays true for every method there.
 *
 * <p>{@code isAuthenticated()} rather than a role, like {@code /auth/sign-out}: it is about the person
 * asking, not about a firm's data, so a {@code CLIENT_VIEWER} or a {@code SCALE_OPERATOR} needs it too,
 * and it needs no tenant.
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CurrentUserController {

    AppUserRepository appUserRepository;

    /** The web app's tour was finished or skipped. Idempotent: a second call keeps the first time. */
    @PostMapping("/tour-seen")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> tourSeen() {
        appUserRepository.markTourSeen(SecurityUtils.currentUser().getId(), Instant.now());
        return ResponseEntity.noContent().build();
    }
}
