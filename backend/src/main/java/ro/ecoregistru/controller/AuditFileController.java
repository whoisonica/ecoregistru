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

    /**
     * @param year  the last reporting year in the dossier
     * @param years how many consecutive years back to include, 1..5. Three is the retention
     *              period an inspection may ask for (OUG 92/2021, art. 48 alin. (5)); five is the
     *              margin the specialist asked for on 24.08.2026. The default stays 1, because
     *              most downloads are for the year being filed.
     */
    @GetMapping
    public ResponseEntity<byte[]> download(@RequestParam int year,
                                           @RequestParam(defaultValue = "1") int years) {
        byte[] body = auditFileService.build(year, years);
        String name = years == 1
                ? "dosar-control-" + year + ".zip"
                : "dosar-control-" + (year - years + 1) + "-" + year + ".zip";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(name)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }
}
