package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.VehicleRequest;
import ro.ecoregistru.controller.response.VehicleResponse;
import ro.ecoregistru.service.VehicleService;

import java.util.List;
import java.util.UUID;

/** Flota (D2.1). Aceleași drepturi ca la șoferi: scrie oricine scrie în firmă, vizualizatorul citește. */
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VehicleController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN','OPERATOR')";

    VehicleService service;

    @GetMapping
    public List<VehicleResponse> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public VehicleResponse create(@RequestBody @Valid VehicleRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public VehicleResponse update(@PathVariable UUID id, @RequestBody @Valid VehicleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> reactivate(@PathVariable UUID id) {
        service.reactivate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/definitiv")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
