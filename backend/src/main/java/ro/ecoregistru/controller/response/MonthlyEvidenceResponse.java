package ro.ecoregistru.controller.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One aggregated Anexa 1 line for a (work point, month, waste code), in the order the form reads:
 * generated, of which recovered and disposed, and what remains in stock (HG 856/2002 anexa nr. 1,
 * cap. 1). All quantities are in KG (movements in TONS are normalised on aggregation).
 *
 * <p>{@code totalHandedOver} is a memo — the part of recovered + disposed that left as a handover,
 * already counted in those two. {@code totalUnclassifiedOut} left the site without an R/D code and
 * is therefore in neither column, which is what {@code incomplete} reports.
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
        BigDecimal totalRecovered,
        BigDecimal totalDisposed,
        BigDecimal totalHandedOver,
        BigDecimal totalUnclassifiedOut,
        /** Quantity left the site with no operation code: the line cannot be reported as it is. */
        boolean incomplete,
        /** The handovers here may be passing on third-party goods; for review, not for reporting. */
        boolean resaleSuspected,
        BigDecimal closingStock,
        Instant generatedAt
) {}
