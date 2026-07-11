package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.ecoregistru.controller.request.WasteMovementRequest;
import ro.ecoregistru.controller.response.AttachmentResponse;
import ro.ecoregistru.controller.response.WasteMovementResponse;
import ro.ecoregistru.service.WasteMovementService;

import java.util.List;
import java.util.UUID;

/**
 * Waste movements CRUD. Reads: any authenticated tenant member. Writes: not CLIENT_VIEWER.
 */
@RestController
@RequestMapping("/api/v1/movements")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WasteMovementController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','ADMIN','OPERATOR')";

    WasteMovementService movementService;

    @GetMapping
    public List<WasteMovementResponse> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) UUID workPointId,
            @RequestParam(required = false) UUID wasteCodeId) {
        return movementService.list(year, month, workPointId, wasteCodeId);
    }

    @GetMapping("/{id}")
    public WasteMovementResponse get(@PathVariable UUID id) {
        return movementService.get(id);
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<WasteMovementResponse> create(@RequestBody @Valid WasteMovementRequest request) {
        return ResponseEntity.ok(movementService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public WasteMovementResponse update(@PathVariable UUID id, @RequestBody @Valid WasteMovementRequest request) {
        return movementService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        movementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/attachments", consumes = "multipart/form-data")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<AttachmentResponse> addAttachment(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(movementService.addAttachment(id, file));
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> deleteAttachment(@PathVariable UUID id, @PathVariable UUID attachmentId) {
        movementService.deleteAttachment(id, attachmentId);
        return ResponseEntity.noContent().build();
    }
}
