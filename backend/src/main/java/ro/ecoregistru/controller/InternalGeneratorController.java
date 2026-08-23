package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.InternalGeneratorRequest;
import ro.ecoregistru.controller.response.InternalGeneratorResponse;
import ro.ecoregistru.service.InternalGeneratorService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal-generators")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalGeneratorController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','ADMIN','OPERATOR')";

    InternalGeneratorService internalGeneratorService;

    @GetMapping
    public List<InternalGeneratorResponse> list(@RequestParam(required = false) UUID workPointId) {
        return internalGeneratorService.list(workPointId);
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<InternalGeneratorResponse> create(
            @RequestBody @Valid InternalGeneratorRequest request) {
        return ResponseEntity.ok(internalGeneratorService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public InternalGeneratorResponse update(@PathVariable UUID id,
                                            @RequestBody @Valid InternalGeneratorRequest request) {
        return internalGeneratorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        internalGeneratorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
