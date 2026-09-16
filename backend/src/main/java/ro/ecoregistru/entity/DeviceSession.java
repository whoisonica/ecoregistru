package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.DevicePlatform;

import java.time.Instant;
import java.util.UUID;

/**
 * G1 — un telefon care are voie să-și ceară un token de acces nou, fără parolă.
 *
 * <p>Tokenul de acces (JWT) rămâne exact ce era: opt ore, nicio bază întrebată la fiecare cerere.
 * Rândul de aici e ce se întâmplă după cele opt ore — și, spre deosebire de JWT, se poate lua înapoi.
 *
 * <p>Ce ține un rând <b>nu</b> e tokenul, ci SHA-256 peste el ({@link #tokenHash}); vezi {@code V50}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "device_sessions")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeviceSession {

    /** Dat de serviciu, nu de bază: rândul se scrie odată cu hash-ul tokenului pe care îl acoperă. */
    @Id
    UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    AppUser user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    String tokenHash;

    @Column(name = "device_name", nullable = false, length = 80)
    String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    DevicePlatform platform;

    @Column(nullable = false)
    Instant createdAt;

    @Column(nullable = false)
    Instant lastUsedAt;

    @Column(nullable = false)
    Instant expiresAt;

    @Column(name = "revoked_at")
    Instant revokedAt;

    /** G2 — tokenul Expo Push al telefonului; null până îl declară aplicația (V54). */
    @Column(name = "push_token", length = 255)
    String pushToken;

    /** Vie = neluată înapoi și neieșită din termen. Singura poartă prin care trece o reîmprospătare. */
    public boolean isLive() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
