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
import ro.ecoregistru.service.energy.EnergyService;
import ro.ecoregistru.service.export.ExportFormat;

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

    /** Anexa 1 (consum sub 1000 tep) of the year, as {@code .xlsx}. 422 at 1000 tep and above. */
    @GetMapping("/anexa1")
    public ResponseEntity<byte[]> annex1(@RequestParam int year) {
        byte[] body = energyService.annex1(year);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("anexa1-energie-" + year + "." + ExportFormat.XLSX.getExtension())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ExportFormat.XLSX.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }

    /** The Declarație accompanying Anexa 1, as a Word file. */
    @GetMapping("/declaratie")
    public ResponseEntity<byte[]> declarationDocx(@RequestParam int year) {
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
                        .filename(content.fileName() == null ? "recipisa.pdf" : content.fileName(),
                                java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(content.bytes());
    }
}
