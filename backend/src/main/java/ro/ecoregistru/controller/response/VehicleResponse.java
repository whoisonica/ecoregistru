package ro.ecoregistru.controller.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Un vehicul din flotă, cu depozitul și transportatorul lui după nume. */
public record VehicleResponse(
        UUID id,
        String registration,
        String kind,
        BigDecimal standardTareKg,
        boolean heavy,
        LocalDate itpExpiry,
        String transportLicenseNumber,
        LocalDate transportLicenseExpiry,
        UUID homeWorkPointId,
        String homeWorkPointName,
        UUID partnerId,
        String partnerName,
        boolean active) {
}
