package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.WasteArticleRequest;
import ro.ecoregistru.controller.response.WasteArticleResponse;
import ro.ecoregistru.service.WasteArticleService;

import java.util.List;
import java.util.UUID;

/**
 * Catalogul de sortimente (D1.6). Îl personalizează oricine scrie în firmă, inclusiv operatorul de la
 * cântar (decizia proprietarului, 15.09.2026: „lasă userii să își customizeze sortimentele”). Fiecare
 * schimbare, inclusiv a bifelor „metal” și „interzis de la PF”, rămâne în jurnalul de audit, iar
 * finalizarea operațiunii tot adminul sau consultantul o face. Vizualizatorul doar citește.
 */
@RestController
@RequestMapping("/api/v1/waste-articles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WasteArticleController {

    static final String CAN_MANAGE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN','OPERATOR')";

    WasteArticleService service;

    @GetMapping
    public List<WasteArticleResponse> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    public WasteArticleResponse create(@RequestBody @Valid WasteArticleRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public WasteArticleResponse update(@PathVariable UUID id, @RequestBody @Valid WasteArticleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /** POST, ca la șoferi: e o faptă, nu o resursă. */
    @PostMapping("/{id}/reactivate")
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<Void> reactivate(@PathVariable UUID id) {
        service.reactivate(id);
        return ResponseEntity.noContent().build();
    }
}
