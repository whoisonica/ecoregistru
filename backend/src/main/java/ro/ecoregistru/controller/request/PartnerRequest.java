package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotBlank;
import ro.ecoregistru.enums.PartnerType;

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
        @NotBlank String name,
        String cui,
        String authorizationNumber,
        LocalDate authorizationExpiry,
        PartnerType type,
        boolean client,
        boolean supplier,
        /** They can haul the waste. Independent of {@code type}: most carriers are also collectors. */
        boolean carrier,

        // --- What Anexa 3 prints about them, as recipient or as carrier ---
        String address,
        /**
         * The partner's work points, replacing the list wholesale on save. Empty clears it — this
         * is a small, fully visible list on one screen, so "what you see is what is stored".
         */
        List<PartnerWorkPointRequest> workPoints,
        String tradeRegisterNumber,
        String transportLicenseNumber,
        LocalDate transportLicenseExpiry,
        /**
         * This carrier's drivers, replaced wholesale on save like the work points. Null leaves them
         * alone, so a client of this API that does not know about the list cannot wipe it by
         * omission.
         */
        List<DriverRequest> drivers
) {}
