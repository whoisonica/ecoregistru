package ro.ecoregistru.controller.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * D2.6 — un rând din registrul formularelor primite. {@code correctedBy} e numărul rândului care îl îndreaptă, dacă
 * există; rândul greșit rămâne în registru, cu numărul lui.
 */
public record ReceivedFormResponse(
        UUID id,
        UUID workPointId,
        String workPointName,
        int entryNo,
        LocalDate receivedOn,
        String formKind,
        String formSeries,
        String formNumber,
        LocalDate formDate,
        String senderName,
        String senderCui,
        String wasteDescription,
        BigDecimal quantityKg,
        UUID weighingOperationId,
        Integer correctsEntryNo,
        String correctionReason,
        Integer correctedBy,
        Instant createdAt) {
}
