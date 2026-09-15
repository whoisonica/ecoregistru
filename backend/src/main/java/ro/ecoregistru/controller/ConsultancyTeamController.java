package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.InviteConsultantRequest;
import ro.ecoregistru.controller.response.CompanyUserResponse;
import ro.ecoregistru.service.ConsultancyService;

import java.util.List;
import java.util.UUID;

/**
 * P2.13 — a consultant's own colleagues. No consultancy id anywhere in the path: it is the caller's,
 * the way {@link UserController} works on the caller's tenant.
 */
@RestController
@RequestMapping("/api/v1/consultancy/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsultancyTeamController {

    static final String CONSULTANT_ONLY = "hasAuthority('CONSULTANT')";

    ConsultancyService consultancyService;

    @GetMapping
    @PreAuthorize(CONSULTANT_ONLY)
    public List<CompanyUserResponse> list() {
        return consultancyService.listTeam();
    }

    @PostMapping
    @PreAuthorize(CONSULTANT_ONLY)
    public ResponseEntity<CompanyUserResponse> invite(@RequestBody @Valid InviteConsultantRequest request) {
        return ResponseEntity.ok(consultancyService.inviteColleague(request));
    }

    /** Deactivates, never deletes — the row is the author of everything they recorded. */
    @DeleteMapping("/{id}")
    @PreAuthorize(CONSULTANT_ONLY)
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        consultancyService.deactivateColleague(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize(CONSULTANT_ONLY)
    public ResponseEntity<Void> reactivate(@PathVariable UUID id) {
        consultancyService.reactivateColleague(id);
        return ResponseEntity.noContent().build();
    }

    /** Aceleași căi ca la utilizatorii firmei ({@link UserController}). */
    @PostMapping("/{id}/resend-invite")
    @PreAuthorize(CONSULTANT_ONLY)
    public ResponseEntity<Void> resendInvite(@PathVariable UUID id) {
        consultancyService.resendColleagueInvite(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/invitation")
    @PreAuthorize(CONSULTANT_ONLY)
    public ResponseEntity<Void> cancelInvite(@PathVariable UUID id) {
        consultancyService.cancelColleagueInvite(id);
        return ResponseEntity.noContent().build();
    }
}
