package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.PhysicalState;
import ro.ecoregistru.enums.StorageType;
import ro.ecoregistru.enums.TreatmentMethod;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WasteRegister;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Create/update payload for a waste movement.
 * clientGeneratedId is optional; when present, create is idempotent on it (offline sync safe).
 * register is optional too: the operation implies it in every case a generator can produce, and
 * the service derives it. operationCode is required for every movement that takes waste off the
 * site and rejected on the ones that do not. partnerId is optional everywhere: it names who
 * performed the operation when it was not this company.
 */
public record WasteMovementRequest(
        UUID clientGeneratedId,
        @NotNull UUID workPointId,
        @NotNull LocalDate date,
        @NotNull UUID wasteCodeId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
        @NotNull Unit unit,
        @NotNull WasteOperation operation,
        WasteRegister register,
        PhysicalState physicalState,
        StorageType storageType,
        TreatmentMethod treatmentMethod,
        WasteOperationCode operationCode,
        UUID partnerId,
        UUID internalGeneratorId,
        String documentReference,
        String notes
) {}
