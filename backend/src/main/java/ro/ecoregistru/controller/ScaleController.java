package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.ScaleEventRequest;
import ro.ecoregistru.controller.request.ScaleRequest;
import ro.ecoregistru.controller.response.ScaleResponse;
import ro.ecoregistru.service.ScaleService;

import java.util.List;
import java.util.UUID;

/**
 * Cântarele depozitului (D2.3). Le văd și le scriu operatorul de cântar și cine aprobă; „Operator”-ul de birou și
 * vizualizatorul nu le văd (decizia proprietarului, 28.09.2026). {@code ScaleService} ține aceeași regulă.
 */
@RestController
@RequestMapping("/api/v1/scales")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScaleController {

    /**
     * 28.09.2026 — și operatorul de cântar își scrie cântarul și verificarea BRML. Riscul e știut: își poate marca
     * singur cântarul legal și ocolește motivul de la finalizare (D2.3); rămâne în jurnal (cântarele sunt auditate).
     */
    static final String CAN_MANAGE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN','SCALE_OPERATOR')";

    ScaleService service;

    @GetMapping
    @PreAuthorize(CAN_MANAGE)
    public List<ScaleResponse> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    public ScaleResponse create(@RequestBody @Valid ScaleRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ScaleResponse update(@PathVariable UUID id, @RequestBody @Valid ScaleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/events")
    @PreAuthorize(CAN_MANAGE)
    public ScaleResponse addEvent(@PathVariable UUID id, @RequestBody @Valid ScaleEventRequest request) {
        return service.addEvent(id, request);
    }

    @PutMapping("/{id}/events/{eventId}")
    @PreAuthorize(CAN_MANAGE)
    public ScaleResponse updateEvent(@PathVariable UUID id, @PathVariable UUID eventId,
                                     @RequestBody @Valid ScaleEventRequest request) {
        return service.updateEvent(id, eventId, request);
    }

    @DeleteMapping("/{id}/events/{eventId}")
    @PreAuthorize(CAN_MANAGE)
    public ScaleResponse deleteEvent(@PathVariable UUID id, @PathVariable UUID eventId) {
        return service.deleteEvent(id, eventId);
    }

    // --- V70: buletinul verificării și dovada BRML, ca fișiere ---

    @PostMapping(value = "/{id}/brml-proof", consumes = "multipart/form-data")
    @PreAuthorize(CAN_MANAGE)
    public ScaleResponse attachBrmlProof(@PathVariable UUID id,
                                         @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return service.attach(id, null, file);
    }

    @PostMapping(value = "/{id}/events/{eventId}/bulletin", consumes = "multipart/form-data")
    @PreAuthorize(CAN_MANAGE)
    public ScaleResponse attachBulletin(@PathVariable UUID id, @PathVariable UUID eventId,
                                        @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return service.attach(id, eventId, file);
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @PreAuthorize(CAN_MANAGE)
    public ScaleResponse detach(@PathVariable UUID id, @PathVariable UUID documentId) {
        return service.detach(id, documentId);
    }

    /** Ca la atașamentele mișcărilor: se citește prin API, iar ce nu e PDF sau imagine se descarcă. */
    @GetMapping("/{id}/documents/{documentId}")
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<byte[]> content(@PathVariable UUID id, @PathVariable UUID documentId) {
        var content = service.content(id, documentId);
        org.springframework.http.MediaType type = WasteMovementController.safeInlineType(content.contentType());
        var disposition = (type == org.springframework.http.MediaType.APPLICATION_OCTET_STREAM
                        ? org.springframework.http.ContentDisposition.attachment()
                        : org.springframework.http.ContentDisposition.inline())
                .filename(content.fileName() == null ? "document" : content.fileName(),
                        java.nio.charset.StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(type)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content.bytes());
    }
}
