package ro.ecoregistru.controller.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Un import salvat, în istoricul lui. {@code movementsRemaining} = mișcările lui încă neșterse;
 * {@code movementsEdited} = dintre ele, cele modificate de la import, pe care anularea le lasă în pace.
 */
public record ImportBatchResponse(
        UUID id,
        String fileName,
        Instant createdAt,
        int workPointsNew,
        int partnersNew,
        int movementsNew,
        int movementsRemaining,
        int movementsEdited,
        Instant undoneAt
) {}
