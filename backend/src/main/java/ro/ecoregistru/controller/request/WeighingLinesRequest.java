package ro.ecoregistru.controller.request;

import ro.ecoregistru.enums.WasteOperationCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Cântarul unei operațiuni în lucru (D1.4): toate liniile, trimise odată. Serverul le înlocuiește
 * pe cele salvate (decizia proprietarului, 15.09.2026: „tot formularul odată”).
 *
 * @param grossKg mașina plină, pe toată operațiunea (opțional)
 * @param tareKg  mașina goală, pe toată operațiunea (opțional)
 * @param lines   în ordinea cântăririi
 */
public record WeighingLinesRequest(BigDecimal grossKg, BigDecimal tareKg, @jakarta.validation.Valid List<Line> lines) {

    /**
     * @param grossKg       și {@code tareKg}: cântărirea liniei; dacă lipsesc, se trece direct {@code netKg}
     * @param netKg         dacă sunt și brut și tara, trebuie să fie egal cu brut − tara (sau lipsă)
     * @param finalKg       cantitatea acceptată (neto minus impurități); lipsă înseamnă neto
     * @param unitPrice     lei/kg fără TVA (taxare inversă)
     * @param operationCode R/D, doar la ieșire, pe fiecare linie (decizia proprietarului, 15.09.2026)
     */
    public record Line(UUID articleId,
                       BigDecimal grossKg,
                       BigDecimal tareKg,
                       BigDecimal netKg,
                       BigDecimal finalKg,
                       BigDecimal unitPrice,
                       WasteOperationCode operationCode,
                       @jakarta.validation.constraints.Size(max = 1000) String notes) {
    }
}
