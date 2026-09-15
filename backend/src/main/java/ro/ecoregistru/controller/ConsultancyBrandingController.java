package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.ecoregistru.controller.request.ConsultancyBrandingRequest;
import ro.ecoregistru.controller.response.ConsultancyBrandingResponse;
import ro.ecoregistru.service.ReportBrandingService;

/**
 * P2.14 — the consultant's own consultancy header: a logo and one line, printed on the unofficial reports
 * of the consultancy's companies. No consultancy id in the path, as {@link ConsultancyTeamController}.
 */
@RestController
@RequestMapping("/api/v1/consultancy/branding")
@PreAuthorize(ConsultancyTeamController.CONSULTANT_ONLY)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsultancyBrandingController {

    ReportBrandingService brandingService;

    @GetMapping
    public ConsultancyBrandingResponse get() {
        return brandingService.mine();
    }

    @PutMapping
    public ConsultancyBrandingResponse updateHeaderLine(@RequestBody ConsultancyBrandingRequest request) {
        return brandingService.updateHeaderLine(request.headerLine());
    }

    @PostMapping(path = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ConsultancyBrandingResponse uploadLogo(@RequestParam("file") MultipartFile file) {
        return brandingService.uploadLogo(file);
    }

    @DeleteMapping("/logo")
    public ConsultancyBrandingResponse deleteLogo() {
        return brandingService.deleteLogo();
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        ReportBrandingService.Logo logo = brandingService.logo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(logo.bytes());
    }
}
