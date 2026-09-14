package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.ConsultancyRequest;
import ro.ecoregistru.controller.request.InviteConsultantRequest;
import ro.ecoregistru.controller.response.CompanyUserResponse;
import ro.ecoregistru.controller.response.ConsultancyResponse;
import ro.ecoregistru.service.ConsultancyService;

import java.util.List;
import java.util.UUID;

/**
 * P2.13 — the consultancies, as the platform admin runs them: list, create, invite the first
 * consultant. Handing a company to a consultancy is on {@link CompanyController}.
 */
@RestController
@RequestMapping("/api/v1/consultancies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsultancyController {

    static final String PLATFORM_ONLY = "hasAuthority('PLATFORM_ADMIN')";

    ConsultancyService consultancyService;

    @GetMapping
    @PreAuthorize(PLATFORM_ONLY)
    public List<ConsultancyResponse> list() {
        return consultancyService.list();
    }

    @PostMapping
    @PreAuthorize(PLATFORM_ONLY)
    public ResponseEntity<ConsultancyResponse> create(@RequestBody @Valid ConsultancyRequest request) {
        return ResponseEntity.ok(consultancyService.create(request));
    }

    @PostMapping("/{id}/users")
    @PreAuthorize(PLATFORM_ONLY)
    public ResponseEntity<CompanyUserResponse> inviteConsultant(
            @PathVariable UUID id, @RequestBody @Valid InviteConsultantRequest request) {
        return ResponseEntity.ok(consultancyService.inviteConsultant(id, request));
    }
}
