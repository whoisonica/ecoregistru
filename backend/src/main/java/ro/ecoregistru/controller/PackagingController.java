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
import ro.ecoregistru.controller.request.PackagingMarketRequest;
import ro.ecoregistru.controller.response.PackagingMarketResponse;
import ro.ecoregistru.controller.response.WasteMovementResponse;
import ro.ecoregistru.service.PackagingService;
import ro.ecoregistru.service.export.ExportFormat;
import ro.ecoregistru.service.export.PackagingDeclaration;

import java.util.List;

/**
 * The packaging module — Anexa 1 Ambalaje (Ordinul 794/2012).
 *
 * <p>Everything the tab shows comes from here, and everything here comes from the movements on
 * {@code 15 01 xx} codes: the register itself, the two tables computed from it, and the lines that
 * could not be placed on a table yet.
 *
 * <p>Reading is open to every tenant member, viewer included, like the other documents. Writing an
 * override of tabelul 1 is not: those are declared quantities, and they end up on a form filed with
 * an authority.
 */
@RestController
@RequestMapping("/api/v1/packaging")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PackagingController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','ADMIN','OPERATOR')";

    PackagingService packagingService;

    /** The register: every movement of the year on a packaging code, newest first. */
    @GetMapping("/movements")
    public List<WasteMovementResponse> movements(@RequestParam int year) {
        return packagingService.movements(year);
    }

    /** Tabelul 1, computed from those movements, with any per-material override applied. */
    @GetMapping("/table1")
    public List<PackagingDeclaration.MarketRow> table1(@RequestParam int year) {
        return packagingService.marketRows(year);
    }

    /** Tabelul 2, computed: the packaging waste handed over, one line per material and operator. */
    @GetMapping("/handovers")
    public List<PackagingDeclaration.HandoverRow> handovers(@RequestParam int year) {
        return packagingService.handovers(year);
    }

    /** The movements the tables could not use, and what each is missing. */
    @GetMapping("/unclassified")
    public List<PackagingDeclaration.UnclassifiedRow> unclassified(@RequestParam int year) {
        return packagingService.unclassified(year);
    }

    /** The stored overrides only — empty rows where the client has overridden nothing. */
    @GetMapping("/market")
    public List<PackagingMarketResponse> market(@RequestParam int year) {
        return packagingService.marketEntries(year);
    }

    /** Overrides one material row; an all-empty row removes the override. */
    @PutMapping("/market")
    @PreAuthorize(CAN_WRITE)
    public PackagingMarketResponse saveMarket(@RequestBody @Valid PackagingMarketRequest request) {
        return packagingService.saveMarketEntry(request);
    }

    /**
     * The document. {@code format=xlsx} is the default: the authority receives a spreadsheet, and
     * the model we hold is one — two sheets, {@code Tabelul nr. 1} and {@code Tabelul nr. 2}.
     * {@code format=pdf} prints the same content on one page, for the control file.
     */
    @GetMapping("/anexa1")
    public ResponseEntity<byte[]> anexa1(@RequestParam int year,
                                         @RequestParam(defaultValue = "xlsx") String format) {
        ExportFormat exportFormat = ExportFormat.fromParam(format);
        byte[] body = packagingService.render(year, exportFormat);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("anexa1-ambalaje-" + year + "." + exportFormat.getExtension())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(exportFormat.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }
}
