package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

/**
 * P2.14 — what a consultancy prints on the unofficial reports of its companies: a logo and one header
 * line. Its own table (see {@code V48}) so the logo never loads with the consultancy itself.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "consultancy_branding")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConsultancyBranding {

    /** The consultancy's id; one row per consultancy at most. */
    @Id
    @Column(name = "consultancy_id")
    UUID consultancyId;

    @Column(length = 200)
    String headerLine;

    /** PNG or JPEG, at most {@code ReportBrandingService.MAX_LOGO_BYTES}. */
    byte[] logo;

    String logoContentType;

    @Column(nullable = false)
    Instant updatedAt;
}
