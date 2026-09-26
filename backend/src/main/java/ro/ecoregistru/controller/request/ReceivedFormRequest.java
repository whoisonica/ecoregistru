package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * D2.6 — un formular de transport primit, trecut în registrul depozitului. La o corectură, depozitul e al rândului
 * îndreptat (se ignoră cel trimis), iar {@code correctionReason} e obligatoriu.
 *
 * @param formKind ANEXA_3 (implicit) sau ANEXA_2
 */
public record ReceivedFormRequest(
        UUID workPointId,
        LocalDate receivedOn,
        String formKind,
        @Size(max = 20) String formSeries,
        @Size(max = 30) String formNumber,
        LocalDate formDate,
        @Size(max = 255) String senderName,
        @Size(max = 20) String senderCui,
        @Size(max = 500) String wasteDescription,
        BigDecimal quantityKg,
        UUID weighingOperationId,
        @Size(max = 500) String correctionReason) {
}
