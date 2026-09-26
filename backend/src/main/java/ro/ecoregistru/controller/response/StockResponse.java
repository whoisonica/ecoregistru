package ro.ecoregistru.controller.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * F3 — soldul unui depozit (sau al firmei) la o dată: pe sortiment și cod de deșeu, cu marfa în tranzit spre el
 * și cea angajată (ieșiri și transferuri în lucru). {@code availableKg} = sold − angajat.
 *
 * @param workPointId null = firma întreagă (depozitele văzute de utilizator + ce e în tranzit între ele)
 * @param limits      D3.4 — doar pe un depozit: limitele din autorizația lui, cu cât s-a folosit din cele comparabile
 */
public record StockResponse(LocalDate date, UUID workPointId, List<Row> rows, int negativeRows, List<Limit> limits) {

    /**
     * Un rând de stoc. D3.2: un sold negativ = s-a scos mai mult decât s-a înregistrat că a intrat. D3.3: pragurile
     * firmei pe sortiment. D3.4: vechimea celui mai vechi lot rămas (FIFO pe cod), doar pe un depozit, și steagul ei.
     *
     * @param ageFlag null, ONE_YEAR (peste plafonul de 1 an înainte de eliminare), THREE_YEARS (peste 3 ani, plafonul
     *                înainte de valorificare — OG 2/2021 art. 3 alin. (2) lit. b)) sau LIMIT (peste durata maximă de
     *                stocare din autorizație)
     */
    public record Row(UUID articleId, String articleName, UUID wasteCodeId, String wasteCode, String wasteName,
                      boolean hazardous, BigDecimal stockKg, BigDecimal inTransitKg, BigDecimal committedKg,
                      BigDecimal availableKg, boolean negative, BigDecimal minKg, BigDecimal maxKg,
                      boolean belowMin, boolean aboveMax, Integer oldestDays, String ageFlag) {
    }

    /**
     * O limită din autorizație și starea ei. {@code comparable}: stocul „la un moment dat” în t/kg și ieșirile „pe an”
     * se compară cu ce a cântărit aplicația; volumul (m³) și cantitățile tratate nu (aplicația nu le măsoară).
     */
    public record Limit(UUID id, String kind, UUID wasteCodeId, String wasteCode, BigDecimal quantity, String unit,
                        String period, Integer maxStorageDays, boolean approximate, String note, boolean comparable,
                        BigDecimal usedKg, boolean exceeded) {
    }
}
