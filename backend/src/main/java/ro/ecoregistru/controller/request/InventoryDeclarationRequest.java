package ro.ecoregistru.controller.request;

import ro.ecoregistru.entity.KeeperDeclaration;

import java.time.LocalDate;
import java.util.List;

/** D3.5 — declarația gestionarului (pct. 8 lit. a)): cele șapte răspunsuri în ordine și ultimele documente. */
public record InventoryDeclarationRequest(List<KeeperDeclaration.Answer> answers, String lastEntryDoc, String lastExitDoc,
                                          LocalDate declarationDate) {
}
