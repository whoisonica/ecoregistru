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
