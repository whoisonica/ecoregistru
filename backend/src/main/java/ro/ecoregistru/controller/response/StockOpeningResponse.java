package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.StockOpeningSource;
import ro.ecoregistru.enums.StockOpeningStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * D3.5 — nota de preluare. {@code firstMovementOn} = prima mișcare a depozitului, ca ecranul să arate până când poate
 * fi data de tăiere.
 */
public record StockOpeningResponse(UUID id, UUID workPointId, String workPointName, Integer number, LocalDate cutOffDate,
                                   StockOpeningSource source, String keeperName, String accountantName, String notes,
                                   StockOpeningStatus status, LocalDate confirmedOn, LocalDate firstMovementOn,
                                   List<Line> lines) {

    public record Line(UUID id, UUID articleId, String articleName, UUID wasteCodeId, String wasteCode, String wasteName,
                       boolean hazardous, BigDecimal kg) {
    }
}
