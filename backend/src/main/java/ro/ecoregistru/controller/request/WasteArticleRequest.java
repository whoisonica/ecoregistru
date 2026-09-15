package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Un sortiment, cum îl trimite formularul din Setări (D1.6).
 *
 * @param metal null = „propune din cod” ({@link ro.ecoregistru.util.MetalWasteCodes}); o valoare
 *              trimisă e alegerea omului și câștigă
 */
public record WasteArticleRequest(
        @Size(max = 160) String name,
        UUID wasteCodeId,
        Boolean metal,
        boolean forbiddenFromIndividuals) {
}
