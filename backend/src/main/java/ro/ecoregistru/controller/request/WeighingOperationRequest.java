package ro.ecoregistru.controller.request;

import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.WeighingOperationType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Capul unei operațiuni de depozit. Liniile, cântarul și plata vin în punctele următoare (D1.4+).
 *
 * @param origin    doar pentru un partener, și doar când nu vrei ce spune fișa lui; la o persoană
 *                  fizică e ignorat (mereu POPULATIE)
 * @param driverId  șoferul ales din listă; numele și mașina se iau de la el dacă nu sunt scrise
 */
public record WeighingOperationRequest(
        WeighingOperationType type,
        UUID workPointId,
        LocalDate date,
        UUID partnerId,
        UUID naturalPersonId,
        PackagingOrigin origin,
        UUID driverId,
        String driverName,
        String vehicleRegistration,
        String orderNumber,
        String notes) {
}
