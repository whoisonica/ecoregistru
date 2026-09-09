package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.LoginRequest;
import ro.ecoregistru.controller.request.ResetPasswordRequest;
import ro.ecoregistru.controller.response.AuthenticationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.VerificationRecord;
import ro.ecoregistru.enums.Role;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.EmailException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.exception.UnprocessableEntityException;
import ro.ecoregistru.repository.AppUserRepository;
import ro.ecoregistru.repository.VerificationRecordRepository;
import ro.ecoregistru.security.RateLimiter;
import ro.ecoregistru.security.TooManyRequestsException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ro.ecoregistru.enums.VerificationRecordType.RESET_PASSWORD;
import static ro.ecoregistru.exception.ErrorMessageEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationService {

    final JwtService jwtService;
    final EmailService emailService;
    final PasswordEncoder passwordEncoder;
    final AuthenticationManager authenticationManager;
    final AppUserRepository appUserRepository;
    final VerificationRecordRepository verificationRecordRepository;
    final RateLimiter rateLimiter;

    private static final int CODE_TTL_MINUTES = 30;

    /**
     * P0.3, the per-email half. {@code RateLimitFilter} already counted this request against the
     * caller's IP; here the account itself is counted, so a list of passwords tried against one
     * address from a hundred addresses is still stopped.
     *
     * <p>Only <strong>failed</strong> attempts are counted, and the check comes first: a locked
     * bucket must not be openable by guessing right on the eleventh try. Counting successes too
     * would have locked out the one person who knows the password — an office signing in on a
     * Monday, or the e2e suite, which authenticates as the same user about ten times a run.
     */
    public AuthenticationResponse login(LoginRequest request) {
        String email = request.email().toLowerCase();
        long retryAfter = rateLimiter.tryConsume(RateLimiter.LOGIN_PER_EMAIL, email);
        if (retryAfter > 0) {
            log.warn("Rate limit login/email hit for {}", email);
            throw new TooManyRequestsException(retryAfter);
        }

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(INVALID_CREDENTIALS));

        if (!user.isEnabled()) {
            throw new BusinessException(EMAIL_NOT_VERIFIED);
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), request.password()));
        } catch (BadCredentialsException e) {
            throw new BusinessException(INVALID_CREDENTIALS);
        }

        // The password was right, so this attempt costs nothing: give the token back.
        rateLimiter.refund(RateLimiter.LOGIN_PER_EMAIL, email);
        return buildAuthResponse(user);
    }

    @Transactional(noRollbackFor = EmailException.class)
    public void requestPasswordReset(String email) {
        // Counted per address as well as per IP (P0.3): this endpoint answers 200 either way, so
        // a flood aimed at one inbox looks identical to a person who forgot their password twice.
        // Three an hour is the second, not the first.
        long retryAfter = rateLimiter.tryConsume(RateLimiter.RESET_PER_EMAIL, email.toLowerCase());
        if (retryAfter > 0) {
            log.warn("Rate limit reset/email hit for {}", email.toLowerCase());
            throw new TooManyRequestsException(retryAfter);
        }
        // Silent no-op if the account does not exist (avoid leaking which emails are registered).
        appUserRepository.findByEmail(email.toLowerCase()).ifPresent(user -> {
            verificationRecordRepository
                    .deleteByUserAndVerificationRecordTypeAndConfirmedFalse(user, RESET_PASSWORD);
            String code = newCode();
            saveRecord(user, code, RESET_PASSWORD);
            try {
                emailService.sendPasswordResetEmail(user, code);
            } catch (EmailException e) {
                log.error("Failed to send reset email to {}", user.getEmail(), e);
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException(PASSWORDS_NOT_MATCH);
        }
        validatePasswordStrength(request.password());

        VerificationRecord record = verificationRecordRepository
                .findByCodeAndVerificationRecordType(request.code(), RESET_PASSWORD)
                .orElseThrow(() -> new NotFoundException(VERIFICATION_RECORD_NOT_FOUND));
        if (!record.isValid()) {
            throw new UnprocessableEntityException(VERIFICATION_CODE_EXPIRED);
        }

        AppUser user = record.getUser();
        user.setPassword(passwordEncoder.encode(request.password()));
        // P0.4 — the new password takes the old sessions with it. Without this, someone who took
        // the account back by resetting the password would be sharing it with whoever still had a
        // token: the account stays enabled, so nothing else about it would have changed.
        user.setTokenVersion(user.getTokenVersion() + 1);
        // Setting a password via the reset link also activates the account. This is what makes
        // the platform-admin invite flow work: invited users start disabled and become usable
        // once they pick their own password. (Harmless for already-enabled users.)
        user.setEnabled(true);
        record.setConfirmed(true);
        appUserRepository.save(user);
        verificationRecordRepository.save(record);
    }

    /**
     * Platform-admin invite: create a tenant user (disabled, with an unusable random password)
     * and email them a reset link to set their own password. Reuses the RESET_PASSWORD flow —
     * no separate invite mechanism. Email failures degrade gracefully (blocked on SMTP for now).
     */
    @Transactional(noRollbackFor = EmailException.class)
    public AppUser inviteUser(Company company, String rawEmail, Role role, String firstName, String lastName) {
        if (role == Role.PLATFORM_ADMIN) {
            throw new BusinessException(INVALID_INVITE_ROLE);
        }
        String email = rawEmail.toLowerCase();
        if (appUserRepository.existsByEmail(email)) {
            throw new UnprocessableEntityException(ACCOUNT_ALREADY_EXISTS);
        }

        AppUser user = AppUser.builder()
                .email(email)
                .password(passwordEncoder.encode(newCode())) // random & unusable until the reset link is used
                .role(role)
                .company(company)
                .firstName(firstName)
                .lastName(lastName)
                .enabled(false)
                .createdAt(Instant.now())
                .build();
        appUserRepository.save(user);

        String code = newCode();
        saveRecord(user, code, RESET_PASSWORD);
        try {
            emailService.sendPasswordResetEmail(user, code);
        } catch (EmailException e) {
            log.error("Failed to send invite email to {}", user.getEmail(), e);
        }
        return user;
    }

    // --- helpers ---

    private void saveRecord(AppUser user, String code, ro.ecoregistru.enums.VerificationRecordType type) {
        verificationRecordRepository.save(VerificationRecord.builder()
                .user(user)
                .code(code)
                .verificationRecordType(type)
                .confirmed(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES))
                .build());
    }

    private AuthenticationResponse buildAuthResponse(AppUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        if (user.getCompany() != null) {
            claims.put("tenantId", user.getCompany().getId().toString());
        }
        String token = jwtService.generateToken(claims, user);
        return AuthenticationResponse.builder()
                .token(token)
                .role(user.getRole())
                .tenantId(user.getCompany() != null ? user.getCompany().getId() : null)
                .tenantName(user.getCompany() != null ? user.getCompany().getName() : null)
                .email(user.getEmail())
                .build();
    }

    private String newCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void validatePasswordStrength(String password) {
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (password.length() < 8 || !hasUpper || !hasLower || !hasDigit) {
            throw new BusinessException(WEAK_PASSWORD);
        }
    }
}
