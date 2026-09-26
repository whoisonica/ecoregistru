package ro.ecoregistru.controller.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * F3 (D3.1) — soldul unui depozit (sau al firmei) la o dată: pe sortiment și cod de deșeu, cu marfa în tranzit spre el
 * și cea angajată (ieșiri și transferuri în lucru). {@code availableKg} = sold − angajat.
 *
 * @param workPointId null = firma întreagă (depozitele văzute de utilizator + ce e în tranzit între ele)
 */
public record StockResponse(LocalDate date, UUID workPointId, List<Row> rows, int negativeRows) {

    /** Un sold negativ înseamnă că s-a scos mai mult decât s-a înregistrat că a intrat: de corectat (D3.2). */
    public record Row(UUID articleId, String articleName, UUID wasteCodeId, String wasteCode, String wasteName,
                      boolean hazardous, BigDecimal stockKg, BigDecimal inTransitKg, BigDecimal committedKg,
                      BigDecimal availableKg, boolean negative) {
    }
}
