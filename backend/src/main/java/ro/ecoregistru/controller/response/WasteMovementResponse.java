package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.PhysicalState;
import ro.ecoregistru.enums.TreatmentPurpose;
import ro.ecoregistru.enums.Unit;

import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WasteRegister;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WasteMovementResponse(
        UUID id,
        UUID workPointId,
        String workPointName,
        LocalDate date,
        UUID wasteCodeId,
        String wasteCode,
        String wasteCodeName,
        boolean hazardous,
        BigDecimal quantity,
        Unit unit,
        WasteOperation operation,
        WasteRegister register,
        /** Derived from operationCode, never stored: which Anexa 1 cap. 1 column the quantity feeds. */
        TreatmentPurpose treatmentPurpose,
        PhysicalState physicalState,
        WasteOperationCode operationCode,
        UUID partnerId,
        String partnerName,
        String documentReference,
        String notes,
        List<AttachmentResponse> attachments,
        UUID clientGeneratedId,
        Instant createdAt,
        Instant updatedAt
) {}
