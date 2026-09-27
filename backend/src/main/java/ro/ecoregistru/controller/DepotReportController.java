package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.enums.DepotReportKind;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.service.DepotReportService;

import java.time.LocalDate;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.EXPORT_FORMAT_UNSUPPORTED;

/**
 * D4.7 — rapoartele fixe ale depozitului, ca fișier. Fără {@code @PreAuthorize}, ca lista Cântarului: fiecare raport își
 * are pragul în serviciu (banii — cine aprobă și vede prețurile; CNP-urile — cine aprobă), iar {@code DepotAccess} taie
 * depozitele.
 */
@RestController
@RequestMapping("/api/v1/depot-reports")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DepotReportController {

    DepotReportService service;

    @GetMapping("/{slug}")
    public ResponseEntity<byte[]> render(@PathVariable String slug,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) UUID workPointId,
                                         @RequestParam(required = false) UUID articleId,
                                         @RequestParam(required = false) UUID partnerId,
                                         @RequestParam(defaultValue = "xlsx") String format) {
        DepotReportKind kind = DepotReportKind.ofSlug(slug)
                .orElseThrow(() -> new NotFoundException(EXPORT_FORMAT_UNSUPPORTED));
        DepotReportService.Rendered file = service.render(kind, from, to, workPointId, articleId, partnerId, format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.body());
    }
}
