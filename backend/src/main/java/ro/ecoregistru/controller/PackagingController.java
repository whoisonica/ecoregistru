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
import ro.ecoregistru.service.PackagingService;
import ro.ecoregistru.service.export.PackagingDeclaration;

import java.util.List;

/**
 * The packaging module — Anexa 1 Ambalaje (Ordinul 794/2012).
 *
 * <p>Reading is open to every tenant member, viewer included, like the other documents. Writing
 * the market figures is not: they are declared quantities, and they end up on a form filed with an
 * authority.
 */
@RestController
@RequestMapping("/api/v1/packaging")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PackagingController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','ADMIN','OPERATOR')";

    PackagingService packagingService;

    /** Tabelul 1: what the company put on the market. One row per material, empty where unanswered. */
    @GetMapping("/market")
    public List<PackagingMarketResponse> market(@RequestParam int year) {
        return packagingService.marketEntries(year);
    }

    @PutMapping("/market")
    @PreAuthorize(CAN_WRITE)
    public PackagingMarketResponse saveMarket(@RequestBody @Valid PackagingMarketRequest request) {
        return packagingService.saveMarketEntry(request);
    }

    /** Tabelul 2, computed: the packaging waste handed over, one line per material and operator. */
    @GetMapping("/handovers")
    public List<PackagingDeclaration.HandoverRow> handovers(@RequestParam int year) {
        return packagingService.handovers(year);
    }

    @GetMapping("/anexa1")
    public ResponseEntity<byte[]> anexa1(@RequestParam int year) {
        byte[] body = packagingService.render(year);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("anexa1-ambalaje-" + year + ".pdf")
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(body);
    }
}
