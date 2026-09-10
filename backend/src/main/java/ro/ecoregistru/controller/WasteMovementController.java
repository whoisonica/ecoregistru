package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.ecoregistru.controller.request.RecordWeightRequest;
import ro.ecoregistru.controller.request.WasteMovementRequest;
import ro.ecoregistru.controller.response.AttachmentResponse;
import ro.ecoregistru.controller.response.Anexa2ThresholdResponse;
import ro.ecoregistru.controller.response.WasteMovementResponse;
import ro.ecoregistru.service.WasteMovementService;

import java.nio.charset.StandardCharsets;
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

    /**
     * The weight, once the recipient has weighed the load. A small act of its own rather than a
     * full edit: the client opens the row, types the figure the collector sent back, and the
     * monthly line stops being provisional. Asked for on 24.08.2026 — the missing quantity "ne
     * încurcă la rapoarte și la anexe".
     */
    @PostMapping("/{id}/weight")
    @PreAuthorize(CAN_WRITE)
    public WasteMovementResponse recordWeight(@PathVariable UUID id,
                                              @RequestBody @Valid RecordWeightRequest request) {
        return movementService.recordWeight(id, request);
    }

    /**
     * Anexa 3 la HG 1061/2008, the transport form, as a PDF. Gated to writers because generating
     * it allocates the form's number — a reprint then returns the same document.
     */
    @GetMapping("/{id}/anexa3")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<byte[]> anexa3(@PathVariable UUID id) {
        byte[] body = movementService.renderAnexa3(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("anexa3-" + id + ".pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }

    /**
     * Anexa 2 la HG 1061/2008, the hazardous-waste consignment form, as a PDF.
     *
     * <p><b>Not gated to {@code CAN_WRITE}</b>, and the difference from {@code anexa3} above is not
     * an oversight: that endpoint allocates the form's number on first use, which is a write.
     * This one allocates nothing — the number of this form is written by the county agency, not by
     * us — so printing it is a read, like fetching an attachment.
     */
    @GetMapping("/{id}/anexa2")
    public ResponseEntity<byte[]> anexa2(@PathVariable UUID id) {
        byte[] body = movementService.renderAnexa2(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("anexa2-" + id + ".pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }

    /**
     * The yearly total the "&lt; 1t/an" tick is proposed from, so the screen can show the figure
     * next to the tick instead of ticking it silently. See {@code Anexa2ThresholdCalculator} for
     * why the proposal never becomes a decision.
     */
    @GetMapping("/{id}/anexa2/prag")
    public Anexa2ThresholdResponse anexa2Threshold(@PathVariable UUID id) {
        return movementService.anexa2Threshold(id);
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

    /**
     * The attachment's bytes, proxied through us — the whole of 11-bis.
     *
     * <p>The file used to be fetched straight from Cloudinary by the browser, at a public URL the
     * API handed out. Reading it now costs a session and membership of the movement's tenant, like
     * every other read: no session is a 401, another company's attachment a 404. Open to readers
     * (not gated to {@code CAN_WRITE}) because seeing a document is a read — a CLIENT_VIEWER who
     * can see the movement can see what is attached to it.
     *
     * <p>{@code inline}, not {@code attachment}: the common case is glancing at a handover note,
     * and a browser can show a PDF or a photo without a trip through the downloads folder.
     */
    @GetMapping("/{id}/attachments/{attachmentId}/continut")
    public ResponseEntity<byte[]> attachmentContent(@PathVariable UUID id,
                                                    @PathVariable UUID attachmentId) {
        var content = movementService.attachmentContent(id, attachmentId);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(content.fileName() == null ? "atasament" : content.fileName(),
                        StandardCharsets.UTF_8)
                .build();
        MediaType type = content.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(content.contentType());
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content.bytes());
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> deleteAttachment(@PathVariable UUID id, @PathVariable UUID attachmentId) {
        movementService.deleteAttachment(id, attachmentId);
        return ResponseEntity.noContent().build();
    }
}
