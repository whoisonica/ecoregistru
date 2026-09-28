package ro.ecoregistru.controller.response;

import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.util.MetalWasteCodes;

import java.time.LocalDate;
import java.util.UUID;

public record WasteCodeResponse(
        UUID id,
        String code,
        String name,
        boolean hazardous,
        /** Propunerea pentru bifa „metal” a unui sortiment nou (D1.6), din {@code MetalWasteCodes}. */
        boolean metalSuggested,
        // V80: first and last day the code exists in the List of Waste; null = open-ended.
        LocalDate validFrom,
        LocalDate validTo
) {
    public static WasteCodeResponse of(WasteCode w) {
        return new WasteCodeResponse(w.getId(), w.getCode(), w.getName(), w.isHazardous(),
                MetalWasteCodes.suggests(w.getCode()), w.getValidFrom(), w.getValidTo());
    }
}
