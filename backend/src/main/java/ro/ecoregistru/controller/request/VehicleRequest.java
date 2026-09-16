package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un vehicul, cum îl trimite formularul din Setări.
 *
 * @param heavy               peste 3,5 t; fără bifă, licența se ignoră și se golește
 * @param homeWorkPointId     depozitul unde stă de obicei, opțional
 * @param partnerId           transportatorul, dacă vehiculul nu e al firmei
 */
public record VehicleRequest(
        @Size(max = 20) String registration,
        @Size(max = 100) String kind,
        BigDecimal standardTareKg,
        boolean heavy,
        LocalDate itpExpiry,
        @Size(max = 100) String transportLicenseNumber,
        LocalDate transportLicenseExpiry,
        UUID homeWorkPointId,
        UUID partnerId) {
}
