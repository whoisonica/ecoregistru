package ro.ecoregistru.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * An analysis bulletin characterising a hazardous waste — OUG 92/2021 art. 8 alin. (4) and
 * art. 48 alin. (2). Stored on Cloudinary like a movement attachment, but attached to a
 * <b>waste code</b>, not to a movement.
 *
 * <p><b>Why per code.</b> Art. 8 alin. (4) requires "o caracterizare a deşeurilor periculoase
 * generate din propria activitate", and the purposes it lists — mixing, preliminary preparation,
 * recycling, recovery, disposal — are properties of the <em>kind</em> of waste, not of one lorry
 * run. So the key is (company, waste code). That reading is what closed half of question AL on
 * 10.09.2026, in the act rather than with the specialist.
 *
 * <p><b>Why there is no expiry column.</b> Art. 48 alin. (2) says the producer must "deţină" the
 * bulletins — a continuing obligation with no term — and art. 48 alin. (5) puts the floor for the
 * evidence itself at three years. The one thing the act genuinely does not say is how often a
 * bulletin must be redone for the same code; that is inspector practice and it is all that is left
 * of AL. Writing a "valid until" date today would be guessing a term the act does not give and
 * then printing it in a dossier an inspector reads — regula de lucru 1. When the answer comes, the
 * column is added additively and nothing here is rewritten.
 *
 * <p><b>Several bulletins on one code are its history, not duplicates.</b> A re-analysis does not
 * replace the old row: a sheet filed in 2025 rested on the bulletin of 2025, and art. 48 alin. (5)
 * requires the evidence to be kept. The most recent one answers "do you have the
 * characterisation?"; the rest stay, and the dossier counts them.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "analysis_bulletins")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnalysisBulletin {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    Company company;

    /** The waste code this bulletin characterises. The whole point of the table. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "waste_code_id", nullable = false)
    WasteCode wasteCode;

    /**
     * The date printed on the laboratory's paper, not the day it was uploaded here: at an
     * inspection what counts is when the analysis was actually made.
     */
    @Column(name = "issue_date", nullable = false)
    LocalDate issueDate;

    /** The issuing laboratory. Free text — the act imposes no nomenclator and none is published. */
    @Column(nullable = false, length = 200)
    String laboratory;

    /**
     * The Cloudinary URL as returned at upload time. Kept for the same reason as on
     * {@link Attachment}: it is the fallback for rows whose three coordinates are null. Everything
     * written by this table has them, so in practice it is never read — the delivery URL is signed
     * on demand and never leaves the backend.
     */
    @Column(nullable = false)
    String url;

    /** Cloudinary public_id, needed to delete the asset and to build its delivery URL. */
    @Column(nullable = false)
    String publicId;

    String resourceType;

    /** {@code authenticated}: a bulletin names the firm, its site and the chemistry of its waste. */
    String deliveryType;

    String format;

    String fileName;
    String contentType;

    @Column(nullable = false)
    Instant createdAt;

    UUID createdBy;
}
