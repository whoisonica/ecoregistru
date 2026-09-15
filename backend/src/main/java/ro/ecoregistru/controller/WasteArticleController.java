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
 * Catalogul de sortimente (D1.6). Îl scriu doar cei care aprobă operațiuni: bifele „metal” și
 * „interzis de la PF” decid ce acte se cer la cântar, deci un operator nu și le poate scoate singur
 * înaintea unei intrări. Citirea e a oricui din firmă, fiindcă formularul de operațiune alege din listă.
 */
@RestController
@RequestMapping("/api/v1/waste-articles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WasteArticleController {

    static final String CAN_MANAGE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN')";

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
