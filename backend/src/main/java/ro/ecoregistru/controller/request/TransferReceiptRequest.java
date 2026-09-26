package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * D2.5 — recepția unui transfer la depozitul de destinație: B își cântărește marfa, cu cântarul lui.
 *
 * @param lines            câte una pentru fiecare linie plecată ({@code lineId} = linia de la plecare), cu greutatea
 *                         de la B; ca la cântar, neto e brut − tara sau scris direct, iar finalul lipsă = neto
 * @param grossKg          cântărirea camionului la B, dacă s-a cântărit întreg (brut/tara)
 * @param nirNumber        peste toleranță: NIR-ul 14-3-1A
 * @param differenceReason peste toleranță: decizia comisiei (de ce diferă)
 * @param scaleReason      ca la finalizare (D2.3): de ce se recepționează cu un cântar care nu era legal
 */
public record TransferReceiptRequest(
        LocalDate receivedOn,
        UUID scaleId,
        BigDecimal grossKg,
        BigDecimal tareKg,
        List<Line> lines,
        @Size(max = 40) String nirNumber,
        @Size(max = 1000) String differenceReason,
        @Size(max = 500) String scaleReason) {

    public record Line(UUID lineId, BigDecimal grossKg, BigDecimal tareKg, BigDecimal netKg, BigDecimal finalKg) {
    }
}
