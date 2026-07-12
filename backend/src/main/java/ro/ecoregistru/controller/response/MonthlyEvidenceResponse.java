package ro.ecoregistru.controller.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One aggregated evidence line for a (work point, month, waste code).
 * All quantities are in KG (movements in TONS are normalised on aggregation).
 */
public record MonthlyEvidenceResponse(
        UUID id,
        UUID workPointId,
        String workPointName,
        int year,
        int month,
        UUID wasteCodeId,
        String wasteCode,
        String wasteCodeName,
        boolean hazardous,
        BigDecimal totalGenerated,
        BigDecimal totalCollected,
        BigDecimal totalHandedOver,
        BigDecimal totalRecovered,
        BigDecimal totalDisposed,
        BigDecimal closingStock,
        Instant generatedAt
) {}
