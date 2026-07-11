package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.PhysicalState;
import ro.ecoregistru.enums.Unit;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteOperationCode;

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
