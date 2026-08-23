package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.CompanyRequest;
import ro.ecoregistru.controller.request.InviteUserRequest;
import ro.ecoregistru.controller.response.CompanyResponse;
import ro.ecoregistru.controller.response.CompanyUserResponse;
import ro.ecoregistru.service.CompanyService;

import java.util.List;
import java.util.UUID;

/**
 * Platform-level company (tenant) directory + management. The only global (NOT tenant-scoped)
 * domain endpoints: PLATFORM_ADMIN lists every tenant to drive the tenant switcher (X-Tenant-Id),
 * creates/edits companies, and invites users onto them. A deliberate, documented exception to the
 * "everything is tenant-scoped" rule — hence the strict role gate on every method.
 */
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompanyController {

    static final String PLATFORM_ONLY = "hasAuthority('PLATFORM_ADMIN')";

    CompanyService companyService;

    @GetMapping
    @PreAuthorize(PLATFORM_ONLY)
    public List<CompanyResponse> list() {
        return companyService.listAll();
    }

    /**
     * The current tenant's own profile - the one endpoint here that is not platform-only, because
     * every screen needs it: the movement form offers the operations this kind of company may
     * record (CompanyType.allowedOperations()), and only the company itself knows its type.
     */
    @GetMapping("/current")
    public CompanyResponse current() {
        return companyService.current();
    }

    @PostMapping
    @PreAuthorize(PLATFORM_ONLY)
    public ResponseEntity<CompanyResponse> create(@RequestBody @Valid CompanyRequest request) {
        return ResponseEntity.ok(companyService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(PLATFORM_ONLY)
    public CompanyResponse update(@PathVariable UUID id, @RequestBody @Valid CompanyRequest request) {
        return companyService.update(id, request);
    }

    @PostMapping("/{id}/users")
    @PreAuthorize(PLATFORM_ONLY)
    public ResponseEntity<CompanyUserResponse> inviteUser(
            @PathVariable UUID id, @RequestBody @Valid InviteUserRequest request) {
        return ResponseEntity.ok(companyService.inviteUser(id, request));
    }
}
