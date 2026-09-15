package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;

import java.time.LocalDate;
import java.util.UUID;

/** Capul operațiunii. Fără CNP: persoana fizică apare doar cu numele. */
public record WeighingOperationResponse(
        UUID id,
        WeighingOperationType type,
        int number,
        LocalDate date,
        UUID workPointId,
        String workPointName,
        UUID partnerId,
        String partnerName,
        UUID naturalPersonId,
        String naturalPersonName,
        PackagingOrigin origin,
        String driverName,
        String vehicleRegistration,
        String orderNumber,
        WeighingOperationStatus status,
        String notes) {
}
