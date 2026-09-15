package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/**
 * A Netopia notification, kept as it arrived once its signature checked out (F3, {@code V47}). Unique on
 * (ntpID, status): Netopia resends until it gets a 200, and a resent one must not settle anything twice.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_notifications")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, length = 64)
    String ntpId;

    String orderId;

    @Column(nullable = false)
    int status;

    /** The raw body, the card token masked out. */
    @Column(nullable = false, columnDefinition = "text")
    String body;

    @Column(nullable = false)
    Instant receivedAt;
}
