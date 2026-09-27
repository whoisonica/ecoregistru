package ro.ecoregistru.controller.request;

import java.time.LocalDate;

/** D3.5 — elementele procesului-verbal (pct. 42) care nu se calculează din linii, plus obiecțiile gestionarului (pct. 33). */
public record InventoryPvRequest(LocalDate pvDate, String causes, String measures, String slowStock,
                                 String storageFindings, String other, String keeperObjections,
                                 String commissionConclusions) {
}
