package ro.ecoregistru.controller.request;

import ro.ecoregistru.enums.StockOpeningSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** D3.5 — nota de preluare a soldurilor, cu toate rândurile odată (salvarea le înlocuiește cât e ciornă). */
public record StockOpeningRequest(UUID workPointId, LocalDate cutOffDate, StockOpeningSource source, String keeperName,
                                  String accountantName, String notes, List<Line> lines) {

    public record Line(UUID articleId, UUID wasteCodeId, BigDecimal kg) {
    }
}
