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
import ro.ecoregistru.controller.response.EvidenceRegenerationResponse;
import ro.ecoregistru.controller.response.MonthlyEvidenceResponse;
import ro.ecoregistru.entity.Company;
import ro.ecoregistru.exception.NotFoundException;
import ro.ecoregistru.repository.CompanyRepository;
import ro.ecoregistru.security.TenantContext;
import ro.ecoregistru.service.EvidenceCalculator;
import ro.ecoregistru.service.export.ExportFormat;
import ro.ecoregistru.service.export.GenericEvidenceExporter;

import java.util.List;
import java.util.UUID;

import static ro.ecoregistru.exception.ErrorMessageEnum.COMPANY_NOT_FOUND;

/**
 * Monthly evidence (FAZA EVID). Reads: any authenticated tenant member.
 * Regenerate (recompute from movements): not CLIENT_VIEWER.
 */
@RestController
@RequestMapping("/api/v1/evidences")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EvidenceController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','ADMIN','OPERATOR')";

    EvidenceCalculator evidenceCalculator;
    GenericEvidenceExporter evidenceExporter;
    ro.ecoregistru.service.export.Anexa1FormGenerator anexa1FormGenerator;
    CompanyRepository companyRepository;

    @GetMapping
    public List<MonthlyEvidenceResponse> list(
            @RequestParam int year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) UUID workPointId) {
        return evidenceCalculator.list(year, month, workPointId);
    }

    @PostMapping("/regenerate")
    @PreAuthorize(CAN_WRITE)
    public EvidenceRegenerationResponse regenerate(@RequestParam int year) {
        return evidenceCalculator.regenerateYear(year);
    }

    /**
     * Downloads the cached evidence as a generic (unofficial) table — xlsx or pdf.
     * Read-only: any authenticated tenant member, including CLIENT_VIEWER. Exports exactly
     * what the last regeneration cached (no recompute); an empty cache yields a header-only file.
     */
    /**
     * The Anexa 1 form itself (HG 856/2002) — one page per waste code per work point, with the
     * identification header and the four chapters. This is the document the client files; the
     * "export" below is an unofficial working summary and stays separate on purpose.
     */
    @GetMapping("/anexa1")
    public ResponseEntity<byte[]> anexa1(@RequestParam int year,
                                         @RequestParam(required = false) UUID workPointId) {
        byte[] body = anexa1FormGenerator.render(evidenceCalculator.anexa1(year, workPointId));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("anexa1-" + year + ".pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam int year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) UUID workPointId,
            @RequestParam(defaultValue = "xlsx") String format) {
        ExportFormat exportFormat = ExportFormat.fromParam(format);
        Company company = companyRepository.findById(TenantContext.require())
                .orElseThrow(() -> new NotFoundException(COMPANY_NOT_FOUND));
        List<MonthlyEvidenceResponse> rows = evidenceCalculator.list(year, month, workPointId);
        byte[] body = evidenceExporter.export(exportFormat, company.getName(), year, month, rows);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("evidenta-" + year + "." + exportFormat.getExtension())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(exportFormat.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }
}
