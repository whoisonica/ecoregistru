package ro.ecoregistru.service;

import io.sentry.Sentry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.config.JwtService;
import ro.ecoregistru.controller.request.LoginRequest;
import ro.ecoregistru.controller.request.ResetPasswordRequest;
import ro.ecoregistru.controller.response.AuthenticationResponse;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.entity.Consultancy;
import ro.ecoregistru.entity.VerificationRecord;
import ro.ecoregistru.enums.DevicePlatform;
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
    final AppUserRepository appUserRepository;
    final VerificationRecordRepository verificationRecordRepository;
    final RateLimiter rateLimiter;
    final DeviceSessionService deviceSessionService;

    private static final int CODE_TTL_MINUTES = 30;
    /** O invitație se deschide când ajunge omul la mail, nu în jumătate de oră. */
    static final int INVITE_TTL_DAYS = 7;

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

        // BUG-065: întâi parola, apoi starea contului — altfel „cont dezactivat” / „neconfirmat” spunea
        // oricui că adresa are cont. Direct cu encoderul: `authenticationManager` verifică `enabled`
        // înaintea parolei și ar fi spus același lucru.
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(INVALID_CREDENTIALS);
        }
        // Before `enabled`: a row that is both enabled and deactivated could only have come from the
        // reset hole closed below, and it must not sign in either.
        if (user.getDeactivatedAt() != null) {
            throw new BusinessException(ACCOUNT_DEACTIVATED);
        }
        if (!user.isEnabled()) {
            throw new BusinessException(EMAIL_NOT_VERIFIED);
        }

        // The password was right, so this attempt costs nothing: give the token back.
        rateLimiter.refund(RateLimiter.LOGIN_PER_EMAIL, email);

        // G1 — un dispozitiv primește pe lângă token și dreptul de a cere altul peste opt ore.
        // Webul nu trimite `deviceName`, deci nu se schimbă nimic pentru el.
        if (request.deviceName() != null && !request.deviceName().isBlank()) {
            DevicePlatform platform =
                    request.devicePlatform() == null ? DevicePlatform.ANDROID : request.devicePlatform();
            var issued = deviceSessionService.issue(user, request.deviceName(), platform);
            return buildAuthResponse(user).toBuilder()
                    .refreshToken(issued.token())
                    .deviceSessionId(issued.session().getId())
                    .build();
        }
        return buildAuthResponse(user);
    }

    /**
     * G1 — telefonul schimbă tokenul de reîmprospătare pe unul nou și pe încă opt ore de acces.
     *
     * <p>Nu e „încă un login”: nu trece prin {@code authenticationManager}, deci nu se numără la
     * limitarea pe adresă. Un token de reîmprospătare nu se poate ghici (256 de biți), iar unul greșit
     * nu spune nimic despre parolă — ce ar proteja limitarea aici e deja protejat de entropie.
     */
    // Fără `@Transactional` aici, dinadins: `rotate` își deschide singur una, cu `noRollbackFor`
    // pentru refuzuri. O tranzacție în jurul ei ar fi devenit cea care hotărăște, iar ea ar fi
    // anulat stingerea rândului pe care refuzul tocmai a scris-o.
    public AuthenticationResponse refresh(String refreshToken) {
        var issued = deviceSessionService.rotate(refreshToken);
        return buildAuthResponse(issued.session().getUser()).toBuilder()
                .refreshToken(issued.token())
                .deviceSessionId(issued.session().getId())
                .build();
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
        // A deactivated account gets no link either — same silent 200, so the answer still says nothing.
        appUserRepository.findByEmail(email.toLowerCase()).filter(u -> u.getDeactivatedAt() == null).ifPresent(user -> {
            // BUG-063: un invitat (cont încă neactivat) are în aceeași tabelă linkul de 7 zile al
            // invitației; oricine scria adresa lui aici i-l anula. Linkul nou se adaugă lângă el.
            if (user.isEnabled()) {
                verificationRecordRepository
                        .deleteByUserAndVerificationRecordTypeAndConfirmedFalse(user, RESET_PASSWORD);
            }
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
        // The link may predate the deactivation (an invite lives 7 days). Without this, the
        // `setEnabled(true)` below undid the admin's decision: a colleague switched off could pick
        // "Parolă uitată" and walk back in. Only reactivation brings an account back.
        if (user.getDeactivatedAt() != null) {
            throw new BusinessException(ACCOUNT_DEACTIVATED);
        }
        // BUG-057: BCrypt refuză peste 72 de octeți, iar o literă cu diacritice ține doi — era 500.
        if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(PASSWORD_TOO_LONG);
        }
        user.setPassword(passwordEncoder.encode(request.password()));
        // P0.4 — the new password takes the old sessions with it. Without this, someone who took
        // the account back by resetting the password would be sharing it with whoever still had a
        // token: the account stays enabled, so nothing else about it would have changed.
        user.setTokenVersion(user.getTokenVersion() + 1);
        // G1 — și telefoanele. Fără asta, `tokenVersion` scotea din cont fiecare sesiune de browser
        // și lăsa vie exact pe cea lungă: telefonul ar fi cerut un token nou și l-ar fi primit.
        deviceSessionService.revokeAllOf(user);
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
     * no separate invite mechanism.
     *
     * <p>{@code noRollbackFor} e regula, nu o amânare: invitația se salvează și atunci când mailul
     * cade, fiindcă utilizatorul creat e lucrul greu de refăcut, iar mailul se retrimite oricând din
     * Setări → Utilizatori. Un rollback ar lăsa în urmă numai un ecran cu eroare.
     */
    @Transactional(noRollbackFor = EmailException.class)
    public AppUser inviteUser(Company company, String rawEmail, Role role, String firstName, String lastName) {
        // CONSULTANT is refused as well: a consultant belongs to a consultancy, not to a firm, and
        // V40 would refuse the row anyway — as a 500 rather than as this sentence.
        if (role == Role.PLATFORM_ADMIN || role == Role.CONSULTANT) {
            throw new BusinessException(INVALID_INVITE_ROLE);
        }
        return invite(role, company, null, rawEmail, firstName, lastName);
    }

    /**
     * P2.13 — invite a consultant onto a consultancy. The same account-and-reset-link mechanism as
     * {@link #inviteUser}; only the owner of the account differs.
     */
    @Transactional(noRollbackFor = EmailException.class)
    public AppUser inviteConsultant(Consultancy consultancy, String rawEmail, String firstName, String lastName) {
        return invite(Role.CONSULTANT, null, consultancy, rawEmail, firstName, lastName);
    }

    /** Exactly one of {@code company} and {@code consultancy} is set — see V40. */
    private AppUser invite(Role role, Company company, Consultancy consultancy,
                           String rawEmail, String firstName, String lastName) {
        String email = rawEmail.toLowerCase();
        if (appUserRepository.existsByEmail(email)) {
            throw new UnprocessableEntityException(ACCOUNT_ALREADY_EXISTS);
        }

        AppUser user = AppUser.builder()
                .email(email)
                .password(passwordEncoder.encode(newCode())) // random & unusable until the reset link is used
                .role(role)
                .company(company)
                .consultancy(consultancy)
                .firstName(firstName)
                .lastName(lastName)
                .enabled(false)
                .createdAt(Instant.now())
                .build();
        appUserRepository.save(user);

        String code = newCode();
        saveInviteRecord(user, code);
        try {
            emailService.sendInviteEmail(user, code, INVITE_TTL_DAYS);
        } catch (EmailException e) {
            // BUG-038: the account stays pending and can be re-invited; the admin is told in the
            // response, and Sentry hears of it, since it only reports the 500s on its own.
            log.error("Failed to send invite email to {}", user.getEmail(), e);
            Sentry.captureException(e);
            user.setInviteEmailFailed(true);
        }
        return user;
    }

    /**
     * P1.12 — send the invite again, to a user who never used the first one.
     *
     * <p>Same three steps as {@code inviteUser} minus creating the account: drop whatever
     * unconfirmed link is outstanding (so the old one in the old mail stops working — two live
     * links to one account is one more than anybody needs), mint a new code, mail it.
     *
     * <p>Counted against {@link RateLimiter#RESET_PER_EMAIL} like a self-service reset, and for
     * the same reason: the thing being protected is the invitee's inbox, and it does not care
     * that this request came from an authenticated admin rather than from a form. Three an hour.
     *
     * <p>Whether the account is even in a state to be re-invited is decided by the caller —
     * {@code CompanyUserService}, which knows the tenant. This method only sends.
     */
    @Transactional(noRollbackFor = EmailException.class)
    public void resendInvite(AppUser user) {
        long retryAfter = rateLimiter.tryConsume(RateLimiter.RESET_PER_EMAIL, user.getEmail().toLowerCase());
        if (retryAfter > 0) {
            log.warn("Rate limit resend-invite hit for {}", user.getEmail());
            throw new TooManyRequestsException(retryAfter);
        }
        verificationRecordRepository
                .deleteByUserAndVerificationRecordTypeAndConfirmedFalse(user, RESET_PASSWORD);
        String code = newCode();
        saveInviteRecord(user, code);
        try {
            emailService.sendInviteEmail(user, code, INVITE_TTL_DAYS);
        } catch (EmailException e) {
            // BUG-038: a 204 here read as "sent". The error rolls the new link back, so the old
            // one, if the first mail did arrive, keeps working.
            log.error("Failed to resend invite email to {}", user.getEmail(), e);
            Sentry.captureException(e);
            throw new BusinessException(EMAIL_SEND_FAILED);
        }
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

    /** Tot un RESET_PASSWORD (pagina și endpointul sunt aceleași), doar cu termenul unei invitații. */
    private void saveInviteRecord(AppUser user, String code) {
        verificationRecordRepository.save(VerificationRecord.builder()
                .user(user)
                .code(code)
                .verificationRecordType(RESET_PASSWORD)
                .confirmed(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(INVITE_TTL_DAYS))
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
                .consultancyName(user.getConsultancy() != null ? user.getConsultancy().getName() : null)
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
