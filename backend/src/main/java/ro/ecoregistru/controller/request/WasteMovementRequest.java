package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.PhysicalState;
import ro.ecoregistru.enums.StorageType;
import ro.ecoregistru.enums.TransportDestination;
import ro.ecoregistru.enums.TransportMeans;
import ro.ecoregistru.enums.WasteDestination;
import ro.ecoregistru.enums.TreatmentMethod;
import ro.ecoregistru.enums.PackagingCategory;
import ro.ecoregistru.enums.PackagingMaterial;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WasteRegister;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Create/update payload for a waste movement.
 * clientGeneratedId is optional; when present, create is idempotent on it (offline sync safe).
 * register is optional too: the operation implies it in every case a generator can produce, and
 * the service derives it. operationCode is required for every movement that takes waste off the
 * site and rejected on the ones that do not. partnerId is optional everywhere: it names who
 * performed the operation when it was not this company.
 */
public record WasteMovementRequest(
        UUID clientGeneratedId,
        @NotNull UUID workPointId,
        @NotNull LocalDate date,
        @NotNull UUID wasteCodeId,
        /** Required unless weighedAtUnloading is set: the recipient weighs the load. */
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
        boolean weighedAtUnloading,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal volumeM3,
        @NotNull Unit unit,
        @NotNull WasteOperation operation,
        WasteRegister register,
        PhysicalState physicalState,
        StorageType storageType,
        TreatmentMethod treatmentMethod,
        TransportMeans transportMeans,
        WasteDestination wasteDestination,
        WasteOperationCode operationCode,
        UUID partnerId,
        UUID internalGeneratorId,
        String documentReference,
        String notes,

        // --- Anexa 3 la HG 1061/2008: filled in when the transport form is going to be printed ---
        LocalDate unloadDate,
        /** Which of the recipient's work points received the load; null = the only one. */
        UUID partnerWorkPointId,
        UUID transportPartnerId,
        String driverName,
        String driverIdentification,
        String vehicleRegistration,
        Set<TransportDestination> transportDestinations,
        /**
         * The unit this one form prints in. Null falls back to the company setting, and then
         * to {@link #unit()} — see {@code Anexa3FormGenerator.printedUnit}.
         */
        Unit anexa3Unit,

        // --- Anexa 1 Ambalaje (Ordinul 794/2012), tabelul 1 ---

        /**
         * Whether this packaging was put on the national market by this company — the tick that
         * decides whether the movement reaches Anexa 1 Ambalaje at all. Null keeps the pre-question
         * behaviour for rows that predate it.
         */
        Boolean packagingOnMarket,
        /**
         * Which material row of tabelul 1 this load counts in. Null lets the waste code propose
         * one, which it can do for 15 01 01, 02, 03 and 07 and not for the rest.
         */
        PackagingMaterial packagingMaterial,
        /**
         * Which column group of tabelul 1 this load counts in. Asked only when the waste code is
         * {@code 15 01 xx}; null everywhere else, and null on a packaging code means the question
         * has not been answered — the quantity then stays out of the printed table.
         */
        PackagingCategory packagingCategory,
        /** Col. 4 / col. 6 of tabelul 1, "din care: ambalaj reutilizabil". */
        Boolean packagingReusable,
        /** Col. 7 of tabelul 1, "ambalaje cu conţinut periculos" — also part of col. 3. */
        Boolean packagingHazardousContent
) {}
