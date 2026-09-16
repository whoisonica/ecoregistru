package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.repository.WeighingOperationRepository;

import java.util.UUID;

/**
 * Numărul următor al Anexei 3 pe o firmă. O serie, două locuri care o folosesc: mișcarea (V10) și, din
 * D1.13, operațiunea de depozit (V52). Carnetul e unul singur, deci numărul e maximul din amândouă + 1.
 *
 * <p>Sub lacăt consultativ ținut până la commit: două tipăriri simultane ar citi același maxim, iar
 * indexurile unice sunt câte unul pe tabel și n-ar vedea ciocnirea dintre o mișcare și o operațiune.
 * Se cheamă numai dintr-o tranzacție care scrie numărul primit.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Anexa3Numbering {

    WasteMovementRepository movementRepository;
    WeighingOperationRepository operationRepository;

    public int next(UUID tenantId) {
        operationRepository.lockNumbering(tenantId + ":anexa3");
        Integer movements = movementRepository.findMaxAnexa3Number(tenantId);
        Integer operations = operationRepository.findMaxAnexa3Number(tenantId);
        return Math.max(movements == null ? 0 : movements, operations == null ? 0 : operations) + 1;
    }
}
