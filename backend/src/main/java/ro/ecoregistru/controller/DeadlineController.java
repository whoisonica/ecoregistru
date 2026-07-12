package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.CompleteDeadlineRequest;
import ro.ecoregistru.controller.response.DeadlineGenerationResponse;
import ro.ecoregistru.controller.response.DeadlineResponse;
import ro.ecoregistru.service.DeadlineService;

import java.util.List;
import java.util.UUID;

/**
 * Reporting deadlines (FAZA TERMENE). Reads: any authenticated tenant member.
 * Generate / complete / reopen (state changes): not CLIENT_VIEWER.
 */
@RestController
@RequestMapping("/api/v1/deadlines")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeadlineController {

    static final String CAN_WRITE = "hasAnyAuthority('PLATFORM_ADMIN','ADMIN','OPERATOR')";

    DeadlineService deadlineService;

    @GetMapping
    public List<DeadlineResponse> list(@RequestParam int year) {
        return deadlineService.list(year);
    }

    @PostMapping("/regenerate")
    @PreAuthorize(CAN_WRITE)
    public DeadlineGenerationResponse regenerate(@RequestParam int year) {
        return deadlineService.regenerateYear(year);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize(CAN_WRITE)
    public DeadlineResponse complete(@PathVariable UUID id,
                                     @RequestBody(required = false) @Valid CompleteDeadlineRequest request) {
        return deadlineService.complete(id, request);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize(CAN_WRITE)
    public DeadlineResponse reopen(@PathVariable UUID id) {
        return deadlineService.reopen(id);
    }
}
