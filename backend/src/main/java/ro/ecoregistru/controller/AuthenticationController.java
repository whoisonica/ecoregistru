package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.*;
import ro.ecoregistru.controller.response.AuthenticationResponse;
import ro.ecoregistru.service.AuthenticationService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    // No /register endpoint, on purpose. EcoRegistru is a closed register: an account exists
    // because support created the company and invited the user onto it, from the intake form the
    // client filled in (POST /api/v1/companies, POST /api/v1/companies/{id}/users). A disabled
    // self-registration endpoint would still have been one configuration flag away from open.

    AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    // No /verify-email either, and for the same reason. It came from the template this project
    // started from, where a user registered themselves and then confirmed an address. Here an
    // account is created disabled by an invite, and picking a password through /reset-password is
    // what enables it — so the confirmation step had nothing left to confirm: no screen triggered
    // it, and the link it mailed pointed at /verifica-email, a route the frontend never had.
    // Removed on 24.08.2026 together with resend-verification-email; /parola-uitata covers a
    // disabled user too, so nothing lost a way in.

    @PostMapping("/request-reset-password")
    public ResponseEntity<Void> requestResetPassword(@RequestBody @Valid RequestResetPasswordRequest request) {
        authenticationService.requestPasswordReset(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
