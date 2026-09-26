package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.ScaleEventRequest;
import ro.ecoregistru.controller.request.ScaleRequest;
import ro.ecoregistru.controller.response.ScaleResponse;
import ro.ecoregistru.service.ScaleService;

import java.util.List;
import java.util.UUID;

/** Cântarele depozitului (D2.3). Aceleași drepturi ca la flotă: scrie oricine scrie, vizualizatorul citește. */
@RestController
@RequestMapping("/api/v1/scales")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScaleController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN','OPERATOR')";

    ScaleService service;

    @GetMapping
    public List<ScaleResponse> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ScaleResponse create(@RequestBody @Valid ScaleRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ScaleResponse update(@PathVariable UUID id, @RequestBody @Valid ScaleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/events")
    @PreAuthorize(CAN_WRITE)
    public ScaleResponse addEvent(@PathVariable UUID id, @RequestBody @Valid ScaleEventRequest request) {
        return service.addEvent(id, request);
    }

    @PutMapping("/{id}/events/{eventId}")
    @PreAuthorize(CAN_WRITE)
    public ScaleResponse updateEvent(@PathVariable UUID id, @PathVariable UUID eventId,
                                     @RequestBody @Valid ScaleEventRequest request) {
        return service.updateEvent(id, eventId, request);
    }

    @DeleteMapping("/{id}/events/{eventId}")
    @PreAuthorize(CAN_WRITE)
    public ScaleResponse deleteEvent(@PathVariable UUID id, @PathVariable UUID eventId) {
        return service.deleteEvent(id, eventId);
    }
}
