package ro.ecoregistru.controller.request;

import java.math.BigDecimal;
import java.util.UUID;

/** D3.3 — pragul unui sortiment într-un depozit; cel puțin unul dintre minim și maxim. Salvarea înlocuiește pragul vechi. */
public record StockThresholdRequest(UUID workPointId, UUID articleId, BigDecimal minKg, BigDecimal maxKg) {
}
