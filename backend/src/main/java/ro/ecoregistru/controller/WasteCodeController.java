package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Limit;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.controller.response.WasteCodeResponse;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.service.DeadlineService;
import ro.ecoregistru.util.Diacritics;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only access to the global waste code nomenclator. Used by the movement form
 * (searchable select). Not tenant-scoped.
 */
@RestController
@RequestMapping("/api/v1/waste-codes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WasteCodeController {

    private static final int MAX_RESULTS = 50;

    WasteCodeRepository wasteCodeRepository;

    /**
     * {@code on} is the date of the movement being written (default: today). The list follows the
     * List of Waste in force that day — from 9.11.2026 the battery codes of Decision (EU) 2025/934
     * appear and the four it removes disappear (V80).
     */
    @GetMapping
    public List<WasteCodeResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate on) {
        LocalDate day = on != null ? on : DeadlineService.today();
        List<WasteCode> codes = (q == null || q.isBlank())
                ? wasteCodeRepository.findValidOn(day, Limit.of(MAX_RESULTS))
                // Folded here, folded in the column: "deseuri", "deșeuri" and "DEŞEURI" are the
                // same search.
                : wasteCodeRepository.search(Diacritics.fold(q.trim()), day, Limit.of(MAX_RESULTS));
        return codes.stream()
                .map(WasteCodeResponse::of)
                .toList();
    }
}
