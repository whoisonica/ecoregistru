package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.PhysicalState;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Create/update payload for a waste movement.
 * clientGeneratedId is optional; when present, create is idempotent on it (offline sync safe).
 */
public record WasteMovementRequest(
        UUID clientGeneratedId,
        @NotNull UUID workPointId,
        @NotNull LocalDate date,
        @NotNull UUID wasteCodeId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
        @NotNull Unit unit,
        @NotNull WasteOperation operation,
        PhysicalState physicalState,
        WasteOperationCode operationCode,
        UUID partnerId,
        String documentReference,
        String notes
) {}
