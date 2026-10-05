package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.EnergyCarriersRequest;
import ro.ecoregistru.controller.request.EnergyConsumptionRequest;
import ro.ecoregistru.controller.request.EnergyContactRequest;
import ro.ecoregistru.controller.request.EnergyDeclarationRequest;
import ro.ecoregistru.controller.response.EnergySheetResponse;
import ro.ecoregistru.controller.response.EnergyYearSummary;
import ro.ecoregistru.service.energy.EnergyService;
import ro.ecoregistru.exception.BadRequestException;
import ro.ecoregistru.service.export.ExportFormat;

import static ro.ecoregistru.exception.ErrorMessageEnum.EXPORT_FORMAT_UNSUPPORTED;

/**
 * The annual energy declaration (Anexa 1, consum sub 1000 tep). Reading is open to every tenant member;
 * writing is not, because the figures end up on a form filed with an authority.
 */
@RestController
@RequestMapping("/api/v1/energy")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EnergyController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN','OPERATOR')";

    EnergyService energyService;

    @GetMapping
    public EnergySheetResponse sheet(@RequestParam int year) {
        return energyService.sheet(year);
    }

    /**
     * Anexa 1 (consum sub 1000 tep) of the year, as {@code .xlsx}. 422 at 1000 tep and above.
     *
     * <p>Din 05.10.2026, {@code format=pdf} dă aceeași anexă ca PDF, {@code inline}, de deschis în tab (Chrome nu arată
     * un .xlsx). Fără {@code format} sau cu {@code xlsx} răspunsul e exact cel de dinainte; orice alt format e 400.
     */
    @GetMapping("/anexa1")
    public ResponseEntity<byte[]> annex1(@RequestParam int year, @RequestParam(required = false) String format) {
        if (wantsPdf(format, ExportFormat.XLSX.getExtension())) {
            return inlinePdf(energyService.annex1Pdf(year), "Anexa 1 energie " + year + ".pdf");
        }
        byte[] body = energyService.annex1(year);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("anexa1-energie-" + year + "." + ExportFormat.XLSX.getExtension())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ExportFormat.XLSX.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }

    /**
     * The Declarație accompanying Anexa 1, as a Word file — or, din 05.10.2026, cu {@code format=pdf}, ca PDF
     * {@code inline}, de deschis în tab. Fără {@code format} sau cu {@code docx}: exact răspunsul de dinainte.
     */
    @GetMapping("/declaratie")
    public ResponseEntity<byte[]> declarationDocx(@RequestParam int year,
                                                  @RequestParam(required = false) String format) {
        if (wantsPdf(format, "docx")) {
            return inlinePdf(energyService.declarationPdf(year), "Declaratie energie " + year + ".pdf");
        }
        byte[] body = energyService.declarationDocx(year);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("declaratie-energie-" + year + ".docx")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }

    /**
     * {@code true} pentru {@code pdf}; {@code false} fără format sau cu formatul editabil al documentului. Nu trece
     * prin {@link ExportFormat#fromParam}: acolo {@code xls} e valid, iar anexa de energie nu are .xls — un format
     * pe care documentul nu-l are e 400, nu fișierul celălalt pus sub alt nume.
     */
    private static boolean wantsPdf(String format, String editable) {
        if (format == null || format.equalsIgnoreCase(editable)) {
            return false;
        }
        if (format.equalsIgnoreCase(ExportFormat.PDF.getExtension())) {
            return true;
        }
        throw new BadRequestException(EXPORT_FORMAT_UNSUPPORTED);
    }

    /** Un PDF de deschis în tab: {@code inline}, cu numele în UTF-8, ca la confirmarea EfEnClima. */
    private static ResponseEntity<byte[]> inlinePdf(byte[] body, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(fileName, java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(body);
    }

    /** The year's filing package (Anexa 1, Declarație, receipt) in one zip. */
    @GetMapping("/dosar")
    public ResponseEntity<byte[]> dossier(@RequestParam int year) {
        byte[] body = energyService.dossier(year);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("dosar-energie-" + year + ".zip").build().toString())
                .body(body);
    }

    /** The years with data, newest first, for the audit-file tab. */
    @GetMapping("/years")
    public java.util.List<EnergyYearSummary> years() {
        return energyService.years();
    }

    @PutMapping("/carriers")
    @PreAuthorize(CAN_WRITE)
    public EnergySheetResponse carriers(@Valid @RequestBody EnergyCarriersRequest request) {
        return energyService.saveCarriers(request);
    }

    @PutMapping("/consumption")
    @PreAuthorize(CAN_WRITE)
    public EnergySheetResponse consumption(@Valid @RequestBody EnergyConsumptionRequest request) {
        return energyService.saveCell(request);
    }

    @PutMapping("/declaration")
    @PreAuthorize(CAN_WRITE)
    public EnergySheetResponse declaration(@Valid @RequestBody EnergyDeclarationRequest request) {
        return energyService.saveDeclaration(request);
    }

    @PutMapping("/contact")
    @PreAuthorize(CAN_WRITE)
    public EnergySheetResponse contact(@Valid @RequestBody EnergyContactRequest request) {
        return energyService.saveContact(request);
    }

    @PostMapping(value = "/recipisa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(CAN_WRITE)
    public EnergySheetResponse receipt(@RequestParam int year, @RequestParam("file") MultipartFile file) {
        return energyService.attachReceipt(year, file);
    }

    @GetMapping("/recipisa/continut")
    public ResponseEntity<byte[]> receiptContent(@RequestParam int year) {
        var content = energyService.receiptContent(year);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(content.fileName() == null ? "confirmare.pdf" : content.fileName(),
                                java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(content.bytes());
    }
}
