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
import ro.ecoregistru.controller.request.RegisterRequest;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ro.ecoregistru.enums.VerificationRecordType.RESET_PASSWORD;
import static ro.ecoregistru.enums.VerificationRecordType.VERIFY_ACCOUNT;
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

    /** Invite-only for now: self-registration is disabled unless explicitly enabled.
     *  Not final: @Value field, must stay out of the generated constructor. */
    @Value("${app.registration-enabled:false}")
    boolean registrationEnabled;

    private static final int CODE_TTL_MINUTES = 30;

    @Transactional(noRollbackFor = EmailException.class)
    public AuthenticationResponse register(RegisterRequest request) {
        if (!registrationEnabled) {
            throw new BusinessException(REGISTRATION_DISABLED);
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException(PASSWORDS_NOT_MATCH);
        }
        validatePasswordStrength(request.password());

        String email = request.email().toLowerCase();
        if (appUserRepository.existsByEmail(email)) {
            throw new UnprocessableEntityException(ACCOUNT_ALREADY_EXISTS);
        }

        AppUser user = AppUser.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.OPERATOR)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .enabled(false)
                .createdAt(Instant.now())
                .build();
        appUserRepository.save(user);

        sendVerificationEmail(user);
        return AuthenticationResponse.builder().build();
    }

    @Transactional
    public AuthenticationResponse verifyEmail(String code) {
        VerificationRecord record = verificationRecordRepository
                .findByCodeAndVerificationRecordType(code, VERIFY_ACCOUNT)
                .orElseThrow(() -> new NotFoundException(VERIFICATION_RECORD_NOT_FOUND));

        if (!record.isValid()) {
            throw new UnprocessableEntityException(INVALID_VERIFICATION_CODE);
        }

        AppUser user = record.getUser();
        user.setEnabled(true);
        record.setConfirmed(true);
        appUserRepository.save(user);
        verificationRecordRepository.save(record);

        return buildAuthResponse(user);
    }

    @Transactional(noRollbackFor = EmailException.class)
    public void resendVerificationEmail(String email) {
        AppUser user = appUserRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
        if (user.isEnabled()) {
            throw new UnprocessableEntityException(ACCOUNT_ALREADY_VERIFIED);
        }
        verificationRecordRepository
                .deleteByUserAndVerificationRecordTypeAndConfirmedFalse(user, VERIFY_ACCOUNT);
        sendVerificationEmail(user);
    }

    public AuthenticationResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmail(request.email().toLowerCase())
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

        return buildAuthResponse(user);
    }

    @Transactional(noRollbackFor = EmailException.class)
    public void requestPasswordReset(String email) {
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

    private void sendVerificationEmail(AppUser user) {
        String code = newCode();
        saveRecord(user, code, VERIFY_ACCOUNT);
        try {
            emailService.sendVerificationEmail(user, code);
        } catch (EmailException e) {
            log.error("Failed to send verification email to {}", user.getEmail(), e);
        }
    }

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
