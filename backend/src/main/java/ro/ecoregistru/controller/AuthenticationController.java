package ro.ecoregistru.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.ecoregistru.controller.request.*;
import ro.ecoregistru.controller.response.AuthenticationResponse;
import ro.ecoregistru.controller.response.DeviceSessionResponse;
import ro.ecoregistru.security.SecurityUtils;
import ro.ecoregistru.service.AuthenticationService;
import ro.ecoregistru.service.DeviceSessionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {

    // No /register endpoint, on purpose. WasteHouse is a closed register: an account exists
    // because support created the company and invited the user onto it, from the intake form the
    // client filled in (POST /api/v1/companies, POST /api/v1/companies/{id}/users). A disabled
    // self-registration endpoint would still have been one configuration flag away from open.

    AuthenticationService authenticationService;
    DeviceSessionService deviceSessionService;

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

    /**
     * G1 — telefonul își schimbă sesiunea lungă pe un token de acces nou și pe o sesiune lungă nouă.
     *
     * <p>Public, ca loginul: cererea nu poartă un token de acces (tocmai a expirat cel vechi), iar ce
     * o autorizează e chiar tokenul din corp. Orice refuz e același mesaj — cine întreabă nu află
     * dacă tokenul e necunoscut, revocat, expirat sau contul e oprit.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refresh(request.refreshToken()));
    }

    /** Ieșirea din cont de pe telefon. 200 și când tokenul nu mai există: n-a rămas nimic de stins. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshTokenRequest request) {
        deviceSessionService.revokeByToken(request.refreshToken());
        return ResponseEntity.ok().build();
    }

    /**
     * „Deconectare” din aplicația web. Cere un token tocmai fiindcă pe el îl stinge: până acum
     * ieșirea din cont se petrecea numai în browser, iar tokenul copiat înainte mergea opt ore.
     *
     * <p>{@code isAuthenticated()} și nu un prag pe rol, din același motiv ca la „scoate
     * dispozitivul ăsta”: e o scriere despre sesiunea celui care o cere, nu despre datele unei
     * firme, deci și un cont de vizualizare trebuie să poată ieși din el.
     */
    @PostMapping("/sign-out")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> signOut() {
        authenticationService.signOut();
        return ResponseEntity.ok().build();
    }

    /**
     * „Dispozitive conectate”, din Setări. Ca {@code /sign-out}, o rută de sub {@code /auth} care
     * cere un token — vezi lista din {@code SecurityConfiguration}, unde sunt scoase din whitelist
     * pe nume.
     */
    @GetMapping("/devices")
    public ResponseEntity<List<DeviceSessionResponse>> devices() {
        return ResponseEntity.ok(deviceSessionService.listLive(SecurityUtils.currentUser())
                .stream().map(DeviceSessionResponse::of).toList());
    }

    /**
     * „Scoate telefonul ăsta”. Doar de pe contul propriu; un id străin e refuzat, nu ignorat.
     *
     * <p>Singura scriere din aplicație care nu e despre datele unei firme, ci despre sesiunea celui
     * care o cere — deci pragul e „are cont”, nu un rol. Inclusiv {@code CLIENT_VIEWER}: patronul
     * care doar se uită își ține totuși aplicația pe telefon și trebuie să și-o poată scoate.
     */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/devices/{id}")
    public ResponseEntity<Void> revokeDevice(@PathVariable UUID id) {
        deviceSessionService.revokeById(SecurityUtils.currentUser(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * G2 — tokenul de push al telefonului, pe sesiunea lui. Același prag ca „Scoate telefonul”: e despre
     * sesiunea celui care cere, deci și {@code CLIENT_VIEWER} primește notificările firmei lui.
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/devices/{id}/push-token")
    public ResponseEntity<Void> setPushToken(@PathVariable UUID id, @RequestBody PushTokenRequest request) {
        deviceSessionService.setPushToken(SecurityUtils.currentUser(), id, request.token());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
