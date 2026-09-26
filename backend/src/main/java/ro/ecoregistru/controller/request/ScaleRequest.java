package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import ro.ecoregistru.enums.ScaleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fișa unui cântar, cum o trimite formularul din Setări.
 *
 * @param accuracyClass  clasa de precizie de pe plăcuță: I, II, III sau IIII
 * @param divisionKg     diviziunea de verificare „e”, în kg
 * @param brmlDeclaredOn data declarării la BRML; lipsă = nedeclarat
 * @param brmlReference  dovada declarării (numărul de înregistrare)
 * @param status         lipsă = în uz
 */
public record ScaleRequest(
        UUID workPointId,
        @Size(max = 100) String name,
        @Size(max = 100) String serialNumber,
        @Size(max = 100) String kind,
        @Size(max = 4) String accuracyClass,
        @Digits(integer = 7, fraction = 3) BigDecimal divisionKg,
        LocalDate commissionedOn,
        LocalDate brmlDeclaredOn,
        @Size(max = 100) String brmlReference,
        ScaleStatus status) {
}
