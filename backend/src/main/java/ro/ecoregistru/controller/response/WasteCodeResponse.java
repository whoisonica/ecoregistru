package ro.ecoregistru.controller.response;

import java.util.UUID;

public record WasteCodeResponse(
        UUID id,
        String code,
        String name,
        boolean hazardous,
        /** Propunerea pentru bifa „metal” a unui sortiment nou (D1.6), din {@code MetalWasteCodes}. */
        boolean metalSuggested
) {}
