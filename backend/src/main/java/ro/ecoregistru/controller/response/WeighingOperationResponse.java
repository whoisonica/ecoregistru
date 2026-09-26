package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.PackagingOrigin;
import ro.ecoregistru.enums.PaymentMethod;
import ro.ecoregistru.enums.WasteOperationCode;
import ro.ecoregistru.enums.WeighingOperationStatus;
import ro.ecoregistru.enums.WeighingOperationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Capul operațiunii, cu liniile. Fără CNP: persoana fizică apare doar cu numele.
 *
 * <p>Reținerile (D1.9, D1.10) sunt bani calculați din prețuri, deci pleacă doar către cine vede
 * prețurile (D1.8); celorlalți le vin goale, ca {@code unitPrice} pe linie.
 */
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
        UUID vehicleId,
        String vehicleRegistration,
        String orderNumber,
        WeighingOperationStatus status,
        String notes,
        BigDecimal grossKg,
        BigDecimal tareKg,
        PaymentMethod paymentMethod,
        String receiptNumber,
        Boolean ownHousehold,
        BigDecimal afmBase,
        BigDecimal afmContribution,
        BigDecimal incomeTaxBase,
        BigDecimal incomeTax,
        /** Cotele în vigoare, ca ecranul să arate cât se reține din plată fără să le știe el. */
        BigDecimal afmRate,
        BigDecimal incomeTaxRate,
        String cancelReason,
        /** D2.3 — cântarul, starea lui la data cântăririi (fixată la finalizare) și motivul confirmării. */
        UUID scaleId,
        String scaleName,
        ro.ecoregistru.service.ScaleLegality.State scaleState,
        String scaleOverrideReason,
        /** Cât se plătește la o intrare, cu reținerile; null pentru cine nu-l vede ({@code seesPayment}). */
        Payment payment,
        List<Line> lines) {

    /**
     * Totalul de plată al unei intrări: valoarea, reținerile și ce primește omul. Îl vede și operatorul
     * la „Doar administratorul” (decizia proprietarului, 26.09.2026: el plătește la cântar), fără prețul
     * pe kg. În lucru e previzualizarea; după finalizare, sumele fixate atunci.
     */
    public record Payment(BigDecimal value, BigDecimal afm, BigDecimal incomeTax, BigDecimal net) {
    }

    /** O linie de cântar. {@code finalKg} e ce intră în stoc și în registre. */
    public record Line(UUID id,
                       int lineNo,
                       UUID articleId,
                       String articleName,
                       String wasteCode,
                       BigDecimal grossKg,
                       BigDecimal tareKg,
                       BigDecimal netKg,
                       BigDecimal finalKg,
                       BigDecimal unitPrice,
                       BigDecimal totalValue,
                       WasteOperationCode operationCode) {
    }
}
