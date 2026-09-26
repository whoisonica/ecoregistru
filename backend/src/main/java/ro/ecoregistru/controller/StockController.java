package ro.ecoregistru.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.ecoregistru.controller.response.StockResponse;
import ro.ecoregistru.service.StockService;

import java.time.LocalDate;
import java.util.UUID;

/** F3 — stocul depozitelor. Îl citește oricine vede depozitul (D2.4); nu scrie nimic. */
@RestController
@RequestMapping("/api/v1/stock")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StockController {

    static final String CAN_MANAGE = "hasAnyAuthority('PLATFORM_ADMIN','CONSULTANT','ADMIN')";

    StockService service;
    ro.ecoregistru.service.StockSettingsService settings;

    @GetMapping
    public StockResponse stock(@RequestParam(required = false) UUID workPointId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.stock(workPointId, date);
    }

    // --- D3.3 pragurile și D3.4 limitele din autorizație: le scrie cine administrează firma ---

    @GetMapping("/thresholds")
    public java.util.List<ro.ecoregistru.service.StockSettingsService.ThresholdView> thresholds(@RequestParam UUID workPointId) {
        return settings.thresholds(workPointId);
    }

    @org.springframework.web.bind.annotation.PutMapping("/thresholds")
    @org.springframework.security.access.prepost.PreAuthorize(CAN_MANAGE)
    public ro.ecoregistru.service.StockSettingsService.ThresholdView saveThreshold(
            @org.springframework.web.bind.annotation.RequestBody ro.ecoregistru.controller.request.StockThresholdRequest request) {
        return settings.saveThreshold(request);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/thresholds/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(CAN_MANAGE)
    public org.springframework.http.ResponseEntity<Void> deleteThreshold(@org.springframework.web.bind.annotation.PathVariable UUID id) {
        settings.deleteThreshold(id);
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    @GetMapping("/limits")
    public java.util.List<ro.ecoregistru.service.StockSettingsService.LimitView> limits(@RequestParam UUID workPointId) {
        return settings.limits(workPointId);
    }

    @org.springframework.web.bind.annotation.PostMapping("/limits")
    @org.springframework.security.access.prepost.PreAuthorize(CAN_MANAGE)
    public ro.ecoregistru.service.StockSettingsService.LimitView addLimit(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody ro.ecoregistru.controller.request.AuthorizedLimitRequest request) {
        return settings.addLimit(request);
    }

    @org.springframework.web.bind.annotation.PutMapping("/limits/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(CAN_MANAGE)
    public ro.ecoregistru.service.StockSettingsService.LimitView updateLimit(@org.springframework.web.bind.annotation.PathVariable UUID id,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody ro.ecoregistru.controller.request.AuthorizedLimitRequest request) {
        return settings.updateLimit(id, request);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/limits/{id}")
    @org.springframework.security.access.prepost.PreAuthorize(CAN_MANAGE)
    public org.springframework.http.ResponseEntity<Void> deleteLimit(@org.springframework.web.bind.annotation.PathVariable UUID id) {
        settings.deleteLimit(id);
        return org.springframework.http.ResponseEntity.noContent().build();
    }
}
