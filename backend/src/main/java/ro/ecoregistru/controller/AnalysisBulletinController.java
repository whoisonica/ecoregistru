package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.ecoregistru.controller.response.AnalysisBulletinResponse;
import ro.ecoregistru.service.AnalysisBulletinService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The analysis bulletins of the current tenant — OUG 92/2021 art. 8 alin. (4), art. 48 alin. (2).
 *
 * <p>Tenant-scoped by construction, like {@code /users}: there is no company id in any path, so an
 * ADMIN reaches exactly their own firm's bulletins. The upload is multipart, the same shape as a
 * movement attachment, because it is the same kind of act — a document arriving from outside.
 */
@RestController
@RequestMapping("/api/v1/analysis-bulletins")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnalysisBulletinController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','ADMIN','OPERATOR')";

    AnalysisBulletinService bulletinService;

    @GetMapping
    public List<AnalysisBulletinResponse> list() {
        return bulletinService.list();
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<AnalysisBulletinResponse> create(
            @RequestParam("wasteCodeId") UUID wasteCodeId,
            @RequestParam("issueDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
            @RequestParam("laboratory") String laboratory,
            // required = false on purpose: a missing part would otherwise be rejected by Spring
            // with a 500 before our own check runs, and the client would get a framework message
            // instead of the one that says why the file is the point — art. 48 alin. (2) obliges
            // the holder to hold the paper, not to declare it exists.
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(bulletinService.create(wasteCodeId, issueDate, laboratory, file));
    }

    /**
     * The bulletin's bytes, proxied through us exactly as a movement attachment is since 11-bis:
     * a session and tenant membership, never a URL. Open to readers — seeing the document is a
     * read, and a CLIENT_VIEWER preparing for an inspection needs it.
     */
    @GetMapping("/{id}/continut")
    public ResponseEntity<byte[]> content(@PathVariable UUID id) {
        var content = bulletinService.content(id);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(content.fileName() == null ? "buletin-analiza" : content.fileName(),
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

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        bulletinService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
