package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * D3.4 — o limită din autorizație, cum o scrie PDF-ul: {@code kind} STORED / TREATED / OUTPUT, {@code wasteCodeId} gol =
 * toate codurile, {@code unit} T / KG / M3, {@code period} AT_ONCE / MONTH / YEAR.
 */
public record AuthorizedLimitRequest(UUID workPointId, String kind, UUID wasteCodeId, BigDecimal quantity, String unit,
                                     String period, Integer maxStorageDays, boolean approximate,
                                     @Size(max = 500) String note) {
}
