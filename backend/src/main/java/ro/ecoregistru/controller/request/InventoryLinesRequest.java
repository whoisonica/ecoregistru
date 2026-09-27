package ro.ecoregistru.controller.request;

import ro.ecoregistru.enums.CountMethod;
import ro.ecoregistru.enums.ShortageNature;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * D3.5 — tot tabelul de numărare odată. Un rând cu {@code id} actualizează rândul fotografiat; unul fără {@code id}
 * e ceva găsit fizic și lipsă din evidență (scriptic 0). Un rând adăugat de mână care lipsește din cerere se scoate;
 * rândurile fotografiate rămân.
 */
public record InventoryLinesRequest(List<Line> lines) {

    public record Line(UUID id, UUID articleId, UUID wasteCodeId, BigDecimal countedKg, CountMethod countMethod,
                       String technicalData, String explanation, ShortageNature shortageNature,
                       String responsiblePerson, boolean slowMoving) {
    }
}
