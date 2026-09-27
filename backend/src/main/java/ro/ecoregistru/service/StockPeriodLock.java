package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import ro.ecoregistru.enums.StockOpeningStatus;
import ro.ecoregistru.enums.WasteOperation;
import ro.ecoregistru.enums.WasteRegister;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.repository.InventoryRepository;
import ro.ecoregistru.repository.StockOpeningRepository;

import java.time.LocalDate;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.STOCK_PERIOD_CLOSED;

/**
 * D3.5 — stocul unui depozit e stabilit la data unui inventar aprobat sau la data de tăiere a notei de preluare.
 * O linie de stoc datată în ziua D e stocul dimineții lui D, deci ce s-a întâmplat înaintea lui D e închis: o
 * mișcare nouă, mutată, finalizată sau anulată acolo ar schimba retroactiv un stoc semnat de comisie. Din ziua D
 * încolo se lucrează normal (pct. 9 din Normele OMFP 2861/2009 — inventarul nu oprește operațiunile).
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockPeriodLock {

    InventoryRepository inventoryRepository;
    StockOpeningRepository openingRepository;

    /** Prima zi în care se mai poate scrie pe depozit, sau {@code null} când nimic nu e închis. */
    public LocalDate firstOpenDay(UUID workPointId) {
        LocalDate inventory = inventoryRepository.latestApprovedStart(workPointId);
        LocalDate opening = openingRepository.findFirstByWorkPoint_IdAndStatus(workPointId, StockOpeningStatus.CONFIRMED)
                .map(o -> o.getCutOffDate()).orElse(null);
        if (inventory == null) {
            return opening;
        }
        return opening == null || inventory.isAfter(opening) ? inventory : opening;
    }

    public void require(UUID workPointId, LocalDate date) {
        LocalDate open = firstOpenDay(workPointId);
        if (open != null && date.isBefore(open)) {
            throw new BusinessException(STOCK_PERIOD_CLOSED);
        }
    }

    /** O mișcare scrisă de mână intră în stoc doar pe registrul art. 48 și doar dacă nu e generare proprie. */
    public static boolean countsInStock(WasteRegister register, WasteOperation operation) {
        return register == WasteRegister.ART_48 && operation != WasteOperation.GENERATED;
    }
}
