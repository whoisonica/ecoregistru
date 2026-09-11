package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.response.AuditLogResponse;
import ro.ecoregistru.controller.response.PageResponse;
import ro.ecoregistru.service.AuditLogService;

import java.util.UUID;

/**
 * Jurnalul de audit al firmei curente — P1.11.
 *
 * <p>Tenant-scoped prin construcţie, ca {@code /users} şi {@code /analysis-bulletins}: nu există
 * niciun id de firmă în cale, deci un ADMIN ajunge exact la jurnalul firmei lui.
 *
 * <p><b>Numai citire, şi numai pentru administratori.</b> Nu există POST, PUT sau DELETE — un
 * jurnal care se poate rescrie nu răspunde la întrebarea pentru care există. Iar citirea e a
 * administratorului fiindcă rândurile numesc oameni: „cine a schimbat cantitatea" e o întrebare
 * despre date, dar răspunsul e despre un coleg.
 */
@RestController
@RequestMapping("/api/v1/audit-log")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogController {

    static final String CAN_READ = "hasAnyAuthority('PLATFORM_ADMIN','ADMIN')";

    AuditLogService auditLogService;

    /**
     * O pagină de jurnal, cea mai nouă întâi.
     *
     * @param entityType filtrul din capul ecranului — „WasteMovement", „Partner"…
     * @param entityId   drumul invers, de la un rând la povestea lui: exact întrebarea
     *                   „cine a modificat cantitatea asta şi când"
     */
    @GetMapping
    @PreAuthorize(CAN_READ)
    public PageResponse<AuditLogResponse> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return auditLogService.list(entityType, entityId, search, page, size);
    }
}
