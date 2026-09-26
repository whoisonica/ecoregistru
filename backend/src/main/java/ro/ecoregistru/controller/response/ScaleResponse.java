package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.ScaleEventKind;
import ro.ecoregistru.enums.ScaleStatus;
import ro.ecoregistru.service.ScaleLegality;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Un cântar cu starea lui de azi și istoricul, cel mai nou primul.
 *
 * @param state      e legal azi? ({@link ScaleLegality})
 * @param validUntil până când ține ultima valabilitate cunoscută, sau null
 */
public record ScaleResponse(
        UUID id,
        UUID workPointId,
        String workPointName,
        String name,
        String serialNumber,
        String kind,
        String accuracyClass,
        BigDecimal divisionKg,
        LocalDate commissionedOn,
        LocalDate brmlDeclaredOn,
        String brmlReference,
        ScaleStatus status,
        ScaleLegality.State state,
        LocalDate validUntil,
        /** V70 — dovada declarării la BRML, ca fișier; null dacă n-a fost urcată. */
        Document brmlProof,
        List<Event> events) {

    /** Un fișier al cântarului; conținutul se citește prin {@code /scales/{id}/documents/{documentId}}. */
    public record Document(UUID id, String fileName, String contentType) {
    }

    public record Event(UUID id,
                        ScaleEventKind kind,
                        LocalDate date,
                        Boolean admitted,
                        String bulletinNumber,
                        LocalDate validUntil,
                        String laboratory,
                        String verifier,
                        String notes,
                        /** V70 — buletinul ca fișier, doar la verificare; null dacă n-a fost urcat. */
                        Document bulletinFile) {
    }
}
