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
import ro.ecoregistru.controller.request.EnvironmentalPermitRequest;
import ro.ecoregistru.controller.request.EnvironmentalReportRequest;
import ro.ecoregistru.controller.response.EnvironmentalReportResponse;
import ro.ecoregistru.service.EnvironmentalReportService;

/**
 * The raport anual de mediu (RAM). Reading is open to every tenant member; writing is not, because the answers end up
 * on a report filed with an authority — the same rule as the energy declaration.
 */
@RestController
@RequestMapping("/api/v1/environmental-report")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnvironmentalReportController {

    static final String CAN_WRITE = EnergyController.CAN_WRITE;

    EnvironmentalReportService service;

    @GetMapping
    public EnvironmentalReportResponse get(@RequestParam int year) {
        return service.get(year);
    }

    @PutMapping
    @PreAuthorize(CAN_WRITE)
    public EnvironmentalReportResponse save(@Valid @RequestBody EnvironmentalReportRequest request) {
        return service.save(request);
    }

    /** The authorization and the due days, written on the company; answers with the RAM of {@code year}. */
    @PutMapping("/permit")
    @PreAuthorize(CAN_WRITE)
    public EnvironmentalReportResponse permit(@RequestParam int year,
                                              @Valid @RequestBody EnvironmentalPermitRequest request) {
        return service.savePermit(year, request);
    }

    @GetMapping("/document")
    public ResponseEntity<byte[]> document(@RequestParam int year) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("raport-anual-de-mediu-" + year + ".docx")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(service.docx(year));
    }
}
