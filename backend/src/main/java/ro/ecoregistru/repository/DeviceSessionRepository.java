package ro.ecoregistru.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.entity.DeviceSession;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceSessionRepository extends JpaRepository<DeviceSession, UUID> {

    Optional<DeviceSession> findByTokenHash(String tokenHash);

    /** Lista din Setări: numai sesiunile vii, cea folosită ultima dată sus. */
    List<DeviceSession> findByUserAndRevokedAtIsNullOrderByLastUsedAtDesc(AppUser user);

    /**
     * „Ieși de peste tot”: parola schimbată și contul dezactivat iau cu ele fiecare telefon.
     *
     * <p>Un UPDATE, nu o buclă prin entități: se cheamă din locuri care nu vor să încarce
     * sesiunile ca să le stingă una câte una. {@code clearAutomatically} fiindcă altfel un
     * {@code findByTokenHash} de după, în aceeași tranzacție, ar răspunde din contextul de persistență
     * cu rândul de dinainte — adică vechi și nerevocat.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DeviceSession d SET d.revokedAt = :now WHERE d.user = :user AND d.revokedAt IS NULL")
    int revokeAllOf(@Param("user") AppUser user, @Param("now") Instant now);
}
