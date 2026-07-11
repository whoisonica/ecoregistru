package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.WorkPointRequest;
import ro.ecoregistru.controller.response.WorkPointResponse;
import ro.ecoregistru.service.WorkPointService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-points")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkPointController {

    static final String CAN_MANAGE = "hasAnyAuthority('PLATFORM_ADMIN','ADMIN')";

    WorkPointService workPointService;

    @GetMapping
    public List<WorkPointResponse> list() {
        return workPointService.list();
    }

    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<WorkPointResponse> create(@RequestBody @Valid WorkPointRequest request) {
        return ResponseEntity.ok(workPointService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public WorkPointResponse update(@PathVariable UUID id, @RequestBody @Valid WorkPointRequest request) {
        return workPointService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        workPointService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
