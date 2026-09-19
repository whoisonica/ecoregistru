package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;
import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PaymentMethod;
import ro.ecoregistru.enums.WeighingOperationType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Capul unei operațiuni de depozit, la creare și la editare cât e în lucru. Liniile vin separat
 * (D1.4), iar reținerile se calculează la finalizare (D1.9, D1.10) — nu se trimit niciodată.
 *
 * @param type          se citește doar la creare: numerotarea e separată pe tip, deci după aceea nu
 *                      se mai schimbă
 * @param origin        doar pentru un partener, și doar când nu vrei ce spune fișa lui; la o persoană
 *                      fizică e ignorat (mereu POPULATIE)
 * @param driverId      șoferul ales din listă; numele și mașina se iau de la el dacă nu sunt scrise
 * @param vehicleId     vehiculul ales din flotă (D2.1); numărul lui are întâietate față de cel scris
 * @param paymentMethod cum se plătește marfa; la o persoană fizică, numerarul are plafon zilnic
 *                      (Legea 70/2015 art. 4), iar viramentul se face în 3 zile lucrătoare
 * @param receiptNumber chitanța, când s-a plătit numerar (OUG 31/2011 art. 1 alin. (1^2) lit. a))
 * @param ownHousehold  declarația persoanei fizice că deșeul provine din gospodăria proprie;
 *                      obligatorie la metal (art. 1 alin. (1^1))
 */
public record WeighingOperationRequest(
        WeighingOperationType type,
        UUID workPointId,
        LocalDate date,
        UUID partnerId,
        UUID naturalPersonId,
        PackagingOrigin origin,
        UUID driverId,
        UUID vehicleId,
        @Size(max = 255) String driverName,
        @Size(max = 50) String vehicleRegistration,
        @Size(max = 60) String orderNumber,
        PaymentMethod paymentMethod,
        @Size(max = 60) String receiptNumber,
        Boolean ownHousehold,
        @Size(max = 1000) String notes) {
}
