package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.ChangeUserRoleRequest;
import ro.ecoregistru.controller.request.InviteUserRequest;
import ro.ecoregistru.controller.response.CompanyUserResponse;
import ro.ecoregistru.service.CompanyUserService;

import java.util.List;
import java.util.UUID;

/**
 * P1.12 — the users of the current tenant.
 *
 * <p><b>Tenant-scoped, unlike {@link CompanyController}.</b> There is no company id in any path
 * here: the firm is the one the session is on ({@code TenantContext}), so an {@code ADMIN} can
 * only ever reach their own colleagues. The platform admin gets the same screen for whichever
 * tenant the switcher is pointing at — one implementation, not a client copy of a staff one.
 *
 * <p>Gated to {@code ADMIN} and {@code PLATFORM_ADMIN} for every method, including the list: who
 * else works here, on what role, is not something an operator or a viewer needs.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    static final String CAN_MANAGE = "hasAnyAuthority('PLATFORM_ADMIN','ADMIN')";

    CompanyUserService companyUserService;

    @GetMapping
    @PreAuthorize(CAN_MANAGE)
    public List<CompanyUserResponse> list() {
        return companyUserService.list();
    }

    @PostMapping
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<CompanyUserResponse> invite(@RequestBody @Valid InviteUserRequest request) {
        return ResponseEntity.ok(companyUserService.invite(request));
    }

    /** A deed, not a resource — POST, like {@code /work-points/{id}/reactivate}. */
    @PostMapping("/{id}/resend-invite")
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<Void> resendInvite(@PathVariable UUID id) {
        companyUserService.resendInvite(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/role")
    @PreAuthorize(CAN_MANAGE)
    public CompanyUserResponse changeRole(
            @PathVariable UUID id, @RequestBody @Valid ChangeUserRoleRequest request) {
        return companyUserService.changeRole(id, request.role());
    }

    /**
     * DELETE deactivates, it does not delete — the same contract as work points and partners.
     * A user row is referenced by every movement they recorded ({@code createdBy}), so removing it
     * would take the evidence trail with it, on a product whose promise is that the trail holds.
     *
     * <p>Refused on an invitation that was never used: there is no access to take away, and the
     * way back from it would be the one broken state ({@code reactivate} over an unusable
     * password). That one is cancelled below.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        companyUserService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cancel an unused invitation — the mistyped address. The only endpoint in the application
     * that removes a row rather than switching it off, and it is safe precisely because the
     * account has never been used. See {@code CompanyUserService.cancelInvite}.
     */
    @DeleteMapping("/{id}/invitation")
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<Void> cancelInvite(@PathVariable UUID id) {
        companyUserService.cancelInvite(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize(CAN_MANAGE)
    public ResponseEntity<Void> reactivate(@PathVariable UUID id) {
        companyUserService.reactivate(id);
        return ResponseEntity.noContent().build();
    }
}
