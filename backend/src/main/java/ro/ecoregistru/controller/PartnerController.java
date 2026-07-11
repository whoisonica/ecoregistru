package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.PartnerRequest;
import ro.ecoregistru.controller.response.PartnerResponse;
import ro.ecoregistru.service.PartnerService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PartnerController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','ADMIN','OPERATOR')";

    PartnerService partnerService;

    @GetMapping
    public List<PartnerResponse> list() {
        return partnerService.list();
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<PartnerResponse> create(@RequestBody @Valid PartnerRequest request) {
        return ResponseEntity.ok(partnerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public PartnerResponse update(@PathVariable UUID id, @RequestBody @Valid PartnerRequest request) {
        return partnerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        partnerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
