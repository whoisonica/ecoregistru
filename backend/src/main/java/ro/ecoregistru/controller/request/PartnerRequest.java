package ro.ecoregistru.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ro.ecoregistru.enums.PartnerType;
import ro.ecoregistru.enums.PackagingOrigin;

import java.time.LocalDate;
import java.util.List;

/**
 * Create/update payload for a partner. {@code client} and {@code supplier} are the commercial
 * role and at least one has to be set — the service rejects a partner with neither, because a
 * screen that colours rows by role cannot show a row that has none.
 *
 * <p>{@code type} is nullable since V28 and {@code carrier} is the tick that makes that legal: a
 * pure haulage firm does nothing with the waste. The service rejects a partner with neither.
 */
public record PartnerRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 32) String cui,
        @Size(max = 128) String authorizationNumber,
        LocalDate authorizationExpiry,
        /** Ziua emiterii autorizaţiei iniţiale — ancora anului de viză (V41). */
        LocalDate authorizationIssueDate,
        @Size(max = 100) String visaDecisionNumber,
        LocalDate visaDecisionDate,
        /** Ultima zi a perioadei scrise pe decizia de viză. */
        LocalDate visaValidUntil,
        PartnerType type,
        boolean client,
        boolean supplier,
        /** They can haul the waste. Independent of {@code type}: most carriers are also collectors. */
        boolean carrier,
        /**
         * What this partner is relative to the packaging waste they bring — nota 2 of anexa 3
         * la Ordinul 794/2012. Answered once here, because the note describes the source and
         * not the transport. Null means unanswered.
         */
        PackagingOrigin packagingOrigin,

        // --- What Anexa 3 prints about them, as recipient or as carrier ---
        @Size(max = 500) String address,
        /**
         * The partner's work points, replacing the list wholesale on save. Empty clears it — this
         * is a small, fully visible list on one screen, so "what you see is what is stored".
         */
        @Valid List<PartnerWorkPointRequest> workPoints,
        @Size(max = 50) String tradeRegisterNumber,
        /** Vehicule peste 3,5 t; fără bifă (sau fără {@code carrier}) licenţa se ignoră. */
        boolean heavyVehicles,
        @Size(max = 255) String transportLicenseNumber,
        LocalDate transportLicenseExpiry,
        /**
         * This carrier's drivers, replaced wholesale on save like the work points. Null leaves them
         * alone, so a client of this API that does not know about the list cannot wipe it by
         * omission.
         */
        @Valid List<DriverRequest> drivers
) {}
