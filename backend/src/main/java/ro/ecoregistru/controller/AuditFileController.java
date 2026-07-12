package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.service.AuditFileService;

/**
 * Control dossier (FAZA DOSAR). Downloads a ZIP bundling the year's evidence, partner
 * authorizations and movement attachments. Read-only: any authenticated tenant member,
 * including CLIENT_VIEWER — presenting the dossier is a read action.
 */
@RestController
@RequestMapping("/api/v1/audit-file")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditFileController {

    AuditFileService auditFileService;

    @GetMapping
    public ResponseEntity<byte[]> download(@RequestParam int year) {
        byte[] body = auditFileService.build(year);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("dosar-control-" + year + ".zip")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }
}
