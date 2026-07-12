package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.response.EvidenceRegenerationResponse;
import ro.ecoregistru.controller.response.MonthlyEvidenceResponse;
import ro.ecoregistru.service.EvidenceCalculator;

import java.util.List;
import java.util.UUID;

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
}
