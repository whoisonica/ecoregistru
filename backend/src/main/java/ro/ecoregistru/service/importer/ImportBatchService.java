package ro.ecoregistru.service.importer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.ecoregistru.controller.response.ImportBatchResponse;
import ro.ecoregistru.controller.response.ImportUndoResponse;
import ro.ecoregistru.entity.ImportBatch;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.exception.BusinessException;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.ImportBatchRepository;
import ro.ecoregistru.repository.WasteMovementRepository;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.security.TenantContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.IMPORT_ALREADY_UNDONE;
import static ro.ecoregistru.exception.ErrorMessageEnum.IMPORT_BATCH_NOT_FOUND;

/**
 * Istoricul importurilor din Excel și anularea lor (V62, 16.09.2026).
 *
 * <p><b>Anularea șterge numai ce n-a atins nimeni.</b> O mișcare importată și apoi corectată (cântărită,
 * completată, schimbată) are {@code version > 0}; ea rămâne, fiindcă cineva a lucrat pe ea după import, iar
 * răspunsul spune câte au rămas. Ștergerea e cea obișnuită (soft, cu jurnal de audit). Partenerii și punctele
 * de lucru create de import rămân: pot fi folosite între timp și de mișcări scrise de mână.
 *
 * <p>La ștergere se golește și {@code clientGeneratedId}: altfel același fișier, reimportat după anulare, ar
 * găsi rândurile șterse după amprentă și n-ar mai importa nimic.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImportBatchService {

    ImportBatchRepository batchRepository;
    WasteMovementRepository movementRepository;
    ro.ecoregistru.repository.MonthlyEvidenceRepository evidenceRepository;

    @Transactional(readOnly = true)
    public List<ImportBatchResponse> list() {
        UUID tenantId = TenantContext.require();
        return batchRepository.findAllByCompanyIdOrderByCreatedAtDesc(tenantId).stream().map(batch -> {
            List<WasteMovement> remaining = movementRepository.findAllByImportBatchIdAndDeletedFalse(batch.getId());
            int edited = (int) remaining.stream().filter(ImportBatchService::editedSinceImport).count();
            return new ImportBatchResponse(batch.getId(), batch.getFileName(), batch.getCreatedAt(),
                    batch.getWorkPointsNew(), batch.getPartnersNew(), batch.getMovementsNew(),
                    remaining.size(), edited, batch.getUndoneAt());
        }).toList();
    }

    @Transactional
    public ImportUndoResponse undo(UUID id) {
        UUID tenantId = TenantContext.require();
        evidenceRepository.lockForRebuild(tenantId); // BUG-048: nu scrie în mijlocul unei refaceri
        ImportBatch batch = batchRepository.findByIdAndCompanyId(id, tenantId)
                .orElseThrow(() -> new NotFoundException(IMPORT_BATCH_NOT_FOUND));
        if (batch.getUndoneAt() != null) {
            throw new BusinessException(IMPORT_ALREADY_UNDONE);
        }
        UUID user = SecurityUtils.currentUser().getId();
        Instant now = Instant.now();
        int deleted = 0;
        int kept = 0;
        for (WasteMovement movement : movementRepository.findAllByImportBatchIdAndDeletedFalse(batch.getId())) {
            if (editedSinceImport(movement)) {
                kept++;
                continue;
            }
            movement.setDeleted(true);
            movement.setDeletedAt(now);
            movement.setDeletedBy(user);
            movement.setClientGeneratedId(null);
            deleted++;
        }
        batch.setUndoneAt(now);
        batch.setUndoneBy(user);
        return new ImportUndoResponse(deleted, kept);
    }

    /** Importul creează rândul cu {@code version = 0}; orice salvare de după îl urcă. */
    private static boolean editedSinceImport(WasteMovement movement) {
        return movement.getVersion() != null && movement.getVersion() > 0;
    }
}
