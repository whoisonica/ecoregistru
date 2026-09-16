package ro.ecoregistru.controller;

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
import ro.ecoregistru.controller.response.ImportResultResponse;
import ro.ecoregistru.service.importer.ExcelImportService;
import ro.ecoregistru.service.importer.ImportTemplate;

/**
 * P2.15 — importul de istoric din Excel. Vezi {@link ExcelImportService}.
 *
 * <p><b>Numai platforma</b> (decizia proprietarului, 16.09.2026): importul e partea noastră din
 * implementare, nu o unealtă a clientului. Clientul și consultantul ne trimit fișierul, iar noi îl
 * aducem în firma lor, aleasă cu {@code X-Tenant-Id}. Și șablonul: n-are ce căuta la cine nu importă.
 */
@RestController
@RequestMapping("/api/v1/import")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImportController {

    static final String CAN_IMPORT = "hasAuthority('PLATFORM_ADMIN')";

    ImportTemplate template;
    ExcelImportService importService;

    @GetMapping("/sablon")
    @PreAuthorize(CAN_IMPORT)
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("sablon-import-wastehouse.xlsx").build().toString())
                .body(template.render());
    }

    /** „Verifică": importul întreg, întors înapoi. POST fiindcă duce un fişier, nu fiindcă scrie. */
    @PostMapping(value = "/verificare", consumes = "multipart/form-data")
    @PreAuthorize(CAN_IMPORT)
    public ImportResultResponse preview(@RequestParam("file") MultipartFile file) {
        return importService.run(file, false);
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize(CAN_IMPORT)
    public ImportResultResponse importFile(@RequestParam("file") MultipartFile file) {
        return importService.run(file, true);
    }
}
