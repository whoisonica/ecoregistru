package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ro.ecoregistru.enums.VerificationRecordType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A one-time code for email verification or password reset.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "verification_records")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    AppUser user;

    @Column(nullable = false)
    String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    VerificationRecordType verificationRecordType;

    @Column(nullable = false)
    boolean confirmed;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @Column(nullable = false)
    LocalDateTime expiresAt;

    public boolean isValid() {
        return !confirmed && expiresAt.isAfter(LocalDateTime.now());
    }
}
