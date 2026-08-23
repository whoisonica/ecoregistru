package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.AccountRequestSubmission;
import ro.ecoregistru.controller.request.RejectAccountRequest;
import ro.ecoregistru.controller.response.AccountRequestResponse;
import ro.ecoregistru.controller.response.CompanyResponse;
import ro.ecoregistru.enums.AccountRequestStatus;
import ro.ecoregistru.service.AccountRequestService;

import java.util.List;
import java.util.UUID;

/**
 * The way into a closed register: a client fills in the intake form, support turns it into an
 * account.
 *
 * <p>{@code POST} is public, and is the only public write in the application. It creates a request
 * — never a user, never a session — and returns nothing about what it wrote, so it cannot be used
 * to probe which companies exist. Everything else here is PLATFORM_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/account-requests")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountRequestController {

    static final String PLATFORM_ONLY = "hasAuthority('PLATFORM_ADMIN')";

    AccountRequestService accountRequestService;

    /** Public. 202: received, someone will look at it — not "your account is ready". */
    @PostMapping
    public ResponseEntity<Void> submit(@RequestBody @Valid AccountRequestSubmission submission) {
        accountRequestService.submit(submission);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    @PreAuthorize(PLATFORM_ONLY)
    public List<AccountRequestResponse> list(
            @RequestParam(required = false) AccountRequestStatus status) {
        return accountRequestService.list(status);
    }

    /** Creates the company from the answers, profile included, and returns it. */
    @PostMapping("/{id}/approve")
    @PreAuthorize(PLATFORM_ONLY)
    public CompanyResponse approve(@PathVariable UUID id) {
        return accountRequestService.approve(id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(PLATFORM_ONLY)
    public ResponseEntity<Void> reject(@PathVariable UUID id,
                                       @RequestBody(required = false) RejectAccountRequest body) {
        accountRequestService.reject(id, body);
        return ResponseEntity.noContent().build();
    }
}
