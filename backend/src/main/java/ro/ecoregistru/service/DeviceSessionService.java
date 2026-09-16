package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.DeviceSession;
import ro.ecoregistru.enums.DevicePlatform;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.repository.DeviceSessionRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static ro.ecoregistru.exception.ErrorMessageEnum.*;

/**
 * G1 — sesiunile pe dispozitiv: cine are voie să-și ceară un token de acces nou fără să tasteze parola.
 *
 * <p><b>Ce nu face.</b> Nu atinge tokenul de acces: acela rămâne opt ore, semnat, necitit din bază
 * (vezi {@code JwtService}). Aici se hotărăște doar dacă un telefon mai are dreptul la încă opt ore.
 *
 * <p><b>Rotirea.</b> Fiecare reîmprospătare înlocuiește tokenul. Cel vechi nu mai deschide nimic —
 * deci un token furat ține cel mult până la următoarea pornire a aplicației adevărate. Ce
 * <em>nu</em> facem, deliberat: să stingem tot lanțul la o refolosire. O rețea care cade între
 * răspuns și scriere lasă telefonul cinstit cu tokenul vechi, iar pedeapsa ar cădea pe el, nu pe hoț.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceSessionService {

    /**
     * Șaizeci de zile de la <b>ultima folosire</b>, nu de la login: un telefon deschis în fiecare zi
     * nu mai cere niciodată parola, iar unul uitat într-un sertar iese singur din cont până la toamnă.
     */
    static final Duration IDLE_VALIDITY = Duration.ofDays(60);

    /** Cât ține lista din Setări. Mai multe telefoane pe un cont e normal (schimbă telefonul, îl repară). */
    static final int MAX_DEVICES_PER_USER = 10;

    /** Forma tokenului Expo Push: „ExponentPushToken[…]” (și vechiul „ExpoPushToken[…]”). */
    static final Pattern EXPO_PUSH_TOKEN = Pattern.compile("^Expo(nent)?PushToken\\[[A-Za-z0-9_-]{1,200}]$");

    DeviceSessionRepository deviceSessionRepository;

    /** Tokenul brut, arătat o singură dată — cui i s-a emis. În bază intră doar hash-ul lui. */
    public record IssuedToken(DeviceSession session, String token) {}

    /**
     * Un dispozitiv nou pe contul ăsta. Se cheamă din login, numai când cel care se loghează
     * <em>spune</em> că e un dispozitiv: webul nu trimite nimic și nu primește nimic.
     */
    @Transactional
    public IssuedToken issue(AppUser user, String deviceName, DevicePlatform platform) {
        List<DeviceSession> live = deviceSessionRepository
                .findByUserAndRevokedAtIsNullOrderByLastUsedAtDesc(user);
        // Peste prag iese cel mai vechi folosit — altfel un telefon care se reinstalează săptămânal
        // ar umple lista și ar ține vii sesiuni pe telefoane care nu mai există.
        if (live.size() >= MAX_DEVICES_PER_USER) {
            live.subList(MAX_DEVICES_PER_USER - 1, live.size())
                    .forEach(old -> old.setRevokedAt(Instant.now()));
        }

        String token = newToken();
        Instant now = Instant.now();
        DeviceSession session = DeviceSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(hash(token))
                .deviceName(trimName(deviceName))
                .platform(platform)
                .createdAt(now)
                .lastUsedAt(now)
                .expiresAt(now.plus(IDLE_VALIDITY))
                .build();
        deviceSessionRepository.save(session);
        return new IssuedToken(session, token);
    }

    /**
     * Schimbă un token de reîmprospătare pe altul, și spune cui aparține.
     *
     * <p>Fiecare motiv de refuz e același 401 pentru client („sesiunea a expirat, loghează-te”), dar
     * unul separat aici, ca proba negativă să poată scoate exact o regulă și să vadă testul căzând:
     * token necunoscut · revocat sau ieșit din termen · cont dezactivat · cont neactivat.
     */
    // `noRollbackFor`: stingerea rândului de mai jos se face pe drumul de refuz, iar refuzul e o
    // excepție. Fără asta, `revoke` se scria și se anula la ieșirea din metodă — un telefon respins
    // ar fi putut încerca la nesfârșit, iar „Dispozitive conectate" ar fi arătat sesiunea ca vie.
    // Prins de `aRefusedRefreshAlsoBurnsTheRow`, nu de citit.
    @Transactional(noRollbackFor = BusinessException.class)
    public IssuedToken rotate(String presentedToken) {
        DeviceSession session = deviceSessionRepository.findByTokenHash(hash(presentedToken))
                .orElseThrow(() -> new BusinessException(DEVICE_SESSION_INVALID));

        if (!session.isLive()) {
            throw new BusinessException(DEVICE_SESSION_INVALID);
        }

        AppUser user = session.getUser();
        // Aceeași ordine ca la login: dezactivat înaintea lui `enabled`, fiindcă un cont oprit de
        // administrator nu trebuie să primească nici mesajul „alege-ți parola”.
        if (user.getDeactivatedAt() != null) {
            revoke(session);
            throw new BusinessException(ACCOUNT_DEACTIVATED);
        }
        if (!user.isEnabled()) {
            revoke(session);
            throw new BusinessException(EMAIL_NOT_VERIFIED);
        }

        String token = newToken();
        Instant now = Instant.now();
        session.setTokenHash(hash(token));
        session.setLastUsedAt(now);
        session.setExpiresAt(now.plus(IDLE_VALIDITY));
        deviceSessionRepository.save(session);
        return new IssuedToken(session, token);
    }

    /** Ieșirea din cont de pe telefonul ăsta. Un token necunoscut e tot o ieșire reușită: n-a rămas nimic. */
    @Transactional
    public void revokeByToken(String presentedToken) {
        deviceSessionRepository.findByTokenHash(hash(presentedToken)).ifPresent(this::revoke);
    }

    /** „Scoate telefonul ăsta” din Setări. Numai de pe contul propriu — cine întreabă verifică. */
    @Transactional
    public void revokeById(AppUser user, UUID sessionId) {
        DeviceSession session = deviceSessionRepository.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new BusinessException(DEVICE_SESSION_INVALID));
        revoke(session);
    }

    /**
     * G2 — telefonul își declară tokenul de push, pe sesiunea lui. Numai pe o sesiune vie a contului
     * propriu: un id străin sau stins e refuzat ca la „Scoate telefonul”. {@code null} îl șterge.
     */
    @Transactional
    public void setPushToken(AppUser user, UUID sessionId, String pushToken) {
        DeviceSession session = deviceSessionRepository.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .filter(DeviceSession::isLive)
                .orElseThrow(() -> new BusinessException(DEVICE_SESSION_INVALID));
        if (pushToken != null && !EXPO_PUSH_TOKEN.matcher(pushToken).matches()) {
            throw new BusinessException(PUSH_TOKEN_INVALID);
        }
        if (pushToken != null) {
            deviceSessionRepository.clearPushTokenElsewhere(pushToken, session.getId());
            // `clearAutomatically` a golit contextul: rândul se citește din nou, altfel save-ul l-ar fi scris peste.
            session = deviceSessionRepository.findById(sessionId).orElseThrow();
        }
        session.setPushToken(pushToken);
        deviceSessionRepository.save(session);
    }

    /** Parola schimbată și contul dezactivat nu lasă niciun telefon în urmă. */
    @Transactional
    public int revokeAllOf(AppUser user) {
        return deviceSessionRepository.revokeAllOf(user, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<DeviceSession> listLive(AppUser user) {
        return deviceSessionRepository.findByUserAndRevokedAtIsNullOrderByLastUsedAtDesc(user)
                .stream().filter(DeviceSession::isLive).toList();
    }

    private void revoke(DeviceSession session) {
        session.setRevokedAt(Instant.now());
        deviceSessionRepository.save(session);
    }

    /** 256 de biți de la {@link SecureRandom}, scriși URL-safe fiindcă pleacă prin JSON. */
    private static String newToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256, nu bcrypt: tabelul se caută <em>după</em> hash, deci sarea per rând n-ar avea cum
     * să fie găsită. Ce face bcrypt lent aici nu ajută — tokenul nu e ales de om, are 256 de biți
     * și nu e într-un dicționar.
     */
    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 lipsește din JVM", e);
        }
    }

    /** Numele vine de la client, deci se taie la cât ține coloana în loc să pice cererea. */
    private static String trimName(String name) {
        String trimmed = name == null || name.isBlank() ? "Telefon" : name.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }
}
