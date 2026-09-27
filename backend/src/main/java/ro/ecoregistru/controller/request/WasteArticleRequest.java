package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Un sortiment, cum îl trimite formularul din Setări (D1.6).
 *
 * @param metal           null = „propune din cod” ({@link ro.ecoregistru.util.MetalWasteCodes}); o valoare
 *                        trimisă e alegerea omului și câștigă
 * @param sourceArticleId F5 — sortimentul vrac din care se face acesta, prin balotare; null = sortiment obișnuit
 * @param baleWeightKg    F5 — cât cântărește un balot; doar împreună cu sursa
 */
public record WasteArticleRequest(
        @Size(max = 160) String name,
        UUID wasteCodeId,
        Boolean metal,
        boolean forbiddenFromIndividuals,
        UUID sourceArticleId,
        java.math.BigDecimal baleWeightKg) {

    /** Forma de dinainte de balotare (F5). */
    public WasteArticleRequest(String name, UUID wasteCodeId, Boolean metal, boolean forbiddenFromIndividuals) {
        this(name, wasteCodeId, metal, forbiddenFromIndividuals, null, null);
    }
}
