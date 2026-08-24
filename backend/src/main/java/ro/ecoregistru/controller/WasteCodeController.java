package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Limit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.controller.response.WasteCodeResponse;
import ro.ecoregistru.entity.WasteCode;
import ro.ecoregistru.repository.WasteCodeRepository;
import ro.ecoregistru.util.Diacritics;

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

    @GetMapping
    public List<WasteCodeResponse> list(@RequestParam(required = false) String q) {
        List<WasteCode> codes = (q == null || q.isBlank())
                ? wasteCodeRepository.findAllByOrderByCodeAsc(Limit.of(MAX_RESULTS))
                // Folded here, folded in the column: "deseuri", "deșeuri" and "DEŞEURI" are the
                // same search.
                : wasteCodeRepository.search(Diacritics.fold(q.trim()), Limit.of(MAX_RESULTS));
        return codes.stream()
                .map(w -> new WasteCodeResponse(w.getId(), w.getCode(), w.getName(), w.isHazardous()))
                .toList();
    }
}
