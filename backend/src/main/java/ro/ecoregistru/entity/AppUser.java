package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ro.ecoregistru.enums.Role;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Application user. Belongs to one Company (tenant), except PLATFORM_ADMIN whose
 * company is null (global staff operating the done-for-you service).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "app_users")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(unique = true, nullable = false)
    String email;

    @Column(nullable = false)
    String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Role role;

    /** Tenant. Null only for PLATFORM_ADMIN. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    Company company;

    String firstName;
    String lastName;

    /** True once the email has been verified. */
    @Column(nullable = false)
    boolean enabled;

    /**
     * P0.4 — the session counter. Every JWT carries the value it was issued with, in the
     * {@code tv} claim; a request whose claim no longer matches is 401. Bumped when the password
     * is reset, and the one thing to write when a „sign out everywhere" is ever needed.
     *
     * <p>Defaults to 0 so tokens issued before {@code V32} — which carry no claim at all — are
     * read as version 0 and keep working until they expire on their own. See the migration for
     * why this is a counter and not a timestamp.
     */
    @Builder.Default
    @Column(name = "token_version", nullable = false)
    int tokenVersion = 0;

    /**
     * P1.12 — when the account was switched off, or null if it never was.
     *
     * <p>Exists to split the two meanings {@code enabled = false} carried on its own: an invited
     * user who has not set a password yet (null here) and one an admin switched off (set here).
     * Without the split the users list cannot tell "În aşteptare" from "Dezactivat", and
     * "Reactivează" on an invited user would enable an account whose password is the random,
     * unusable one from {@code inviteUser} — active-looking forever, and unable to sign in ever.
     *
     * <p>Cleared on reactivation, so the pair (enabled, deactivatedAt) has exactly three
     * reachable states. See {@code V34}.
     */
    @Column(name = "deactivated_at")
    Instant deactivatedAt;

    @Column(nullable = false)
    Instant createdAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
