package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.DriverRequest;
import ro.ecoregistru.controller.response.DriverResponse;
import ro.ecoregistru.service.DriverService;

import java.util.List;
import java.util.UUID;

/**
 * The drivers of this tenant. {@code GET} returns all of them — ours and the carriers' — because
 * the movement form needs both in one call; the write endpoints only touch ours, since a carrier's
 * drivers are edited in its partner form. See {@link DriverService}.
 */
@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DriverController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','ADMIN','OPERATOR')";

    DriverService driverService;

    @GetMapping
    public List<DriverResponse> list() {
        return driverService.list();
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<DriverResponse> create(@RequestBody @Valid DriverRequest request) {
        return ResponseEntity.ok(driverService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public DriverResponse update(@PathVariable UUID id, @RequestBody @Valid DriverRequest request) {
        return driverService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        driverService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
