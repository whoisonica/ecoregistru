package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.PackagingCategory;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.enums.PhysicalState;
import ro.ecoregistru.enums.StorageType;
import ro.ecoregistru.enums.TransportDestination;
import ro.ecoregistru.enums.TransportMeans;
import ro.ecoregistru.enums.WasteDestination;
import ro.ecoregistru.enums.TreatmentMethod;
import ro.ecoregistru.enums.TreatmentPurpose;
import ro.ecoregistru.enums.Unit;

import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WasteRegister;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record WasteMovementResponse(
        UUID id,
        UUID workPointId,
        String workPointName,
        LocalDate date,
        UUID wasteCodeId,
        String wasteCode,
        String wasteCodeName,
        boolean hazardous,
        /** Null while the recipient has not weighed the load yet. */
        BigDecimal quantity,
        boolean weighedAtUnloading,
        BigDecimal volumeM3,
        Unit unit,
        WasteOperation operation,
        WasteRegister register,
        /** Derived from operationCode, never stored: which Anexa 1 cap. 1 column the quantity feeds. */
        TreatmentPurpose treatmentPurpose,
        PhysicalState physicalState,
        /** Anexa 1 cap. 2 "Stocare: Tipul". */
        StorageType storageType,
        /** Anexa 1 cap. 2 "Tratare: Modul". */
        TreatmentMethod treatmentMethod,
        /** Anexa 1 cap. 2 "Transport: Mijlocul". */
        TransportMeans transportMeans,
        /** Anexa 1 cap. 2 "Transport: Destinaţia". */
        WasteDestination wasteDestination,
        WasteOperationCode operationCode,
        UUID partnerId,
        String partnerName,
        /**
         * True when the recipient's environmental authorization had already expired on the day of
         * this movement — OUG 92/2021 art. 23 alin. (1), which requires the handover to go to an
         * <em>authorized</em> operator. Compared against {@link #date()}, not against today: the
         * question the form answers is whether the operator was authorized when the waste left,
         * and a partner whose paperwork has lapsed since does not make last year's handover wrong.
         *
         * <p>False when the partner has no expiry recorded at all — an empty field is "we do not
         * know", and regula de lucru 1 forbids turning that into an accusation.
         *
         * <p>It is a warning, never a refusal, and it is shown on screen only. See
         * {@code WasteMovementService#renderAnexa3} for why the form still prints, and why the
         * warning must not be printed on it.
         */
        boolean recipientAuthorizationExpired,
        /** The recipient's authorization expiry, so the screen can name the date it warns about. */
        LocalDate recipientAuthorizationExpiry,
        UUID internalGeneratorId,
        /** Printed as "Secţia" in Anexa 1 cap. 2; null when the movement predates the notion. */
        String internalGeneratorName,
        String documentReference,
        String notes,
        List<AttachmentResponse> attachments,
        UUID clientGeneratedId,

        // --- Anexa 3 ---
        LocalDate unloadDate,
        UUID partnerWorkPointId,
        String partnerWorkPointLabel,
        UUID transportPartnerId,
        String transportPartnerName,
        String driverName,
        String driverIdentification,
        String vehicleRegistration,
        Set<TransportDestination> transportDestinations,
        /** Set once the form has been generated; a reprint keeps the same series and number. */
        String anexa3Series,
        Integer anexa3Number,
        /** Null means "as the company chose, and failing that as the quantity was recorded". */
        Unit anexa3Unit,

        // --- Anexa 1 Ambalaje ---
        /** Null on rows that predate the question; they behave as true. */
        Boolean packagingOnMarket,
        /** What the tables actually do with it: true when the movement feeds Anexa 1 Ambalaje. */
        boolean countsForAnexa1Packaging,
        /** What the client chose; null means "as the code proposes". */
        PackagingMaterial packagingMaterial,
        /** What the table will actually use: the choice, or what the code proposes, or null. */
        PackagingMaterial effectivePackagingMaterial,
        /** Which column group of tabelul 1 the quantity counts in; null on non-packaging codes. */
        PackagingCategory packagingCategory,
        Boolean packagingReusable,
        Boolean packagingHazardousContent,
        /** True when the code is 15 01 xx, so the screen knows to ask the three above. */
        boolean packagingCode,

        Instant createdAt,
        Instant updatedAt
) {}
