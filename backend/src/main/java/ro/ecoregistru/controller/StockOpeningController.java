package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.controller.request.StockOpeningRequest;
import ro.ecoregistru.controller.response.StockOpeningResponse;
import ro.ecoregistru.service.StockOpeningService;

import java.util.List;
import java.util.UUID;

/** D3.5 — nota de preluare a soldurilor. O citește oricine vede depozitul; o scrie cine administrează firma. */
@RestController
@RequestMapping("/api/v1/stock-openings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockOpeningController {

    StockOpeningService service;

    @GetMapping
    public List<StockOpeningResponse> list(@RequestParam(required = false) UUID workPointId) {
        return service.list(workPointId);
    }

    @GetMapping("/{id}")
    public StockOpeningResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize(StockController.CAN_MANAGE)
    public StockOpeningResponse create(@RequestBody StockOpeningRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(StockController.CAN_MANAGE)
    public StockOpeningResponse update(@PathVariable UUID id, @RequestBody StockOpeningRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(StockController.CAN_MANAGE)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize(StockController.CAN_MANAGE)
    public StockOpeningResponse confirm(@PathVariable UUID id) {
        return service.confirm(id);
    }
}
