package ro.ecoregistru.controller.response;

import java.util.UUID;

/** Un sortiment din catalogul firmei, cu codul lui LER. */
public record WasteArticleResponse(
        UUID id,
        String name,
        UUID wasteCodeId,
        String wasteCode,
        String wasteCodeName,
        boolean hazardous,
        boolean metal,
        boolean forbiddenFromIndividuals,
        boolean active) {
}
