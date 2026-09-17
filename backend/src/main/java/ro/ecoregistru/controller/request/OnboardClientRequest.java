package ro.ecoregistru.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * F-C — „Client nou” in one go: the company, and optionally the request it comes from, its subscription and the
 * person who administers the account. Saved together or not at all, so no client is left half made.
 *
 * @param accountRequestId the request being approved; null for a client added by hand
 * @param subscription     null for „Salvează fără abonament”; the platform only
 * @param admin            null when nobody is invited yet
 */
public record OnboardClientRequest(
        @NotNull @Valid CompanyRequest company,
        UUID accountRequestId,
        @Valid SubscriptionRequest subscription,
        @Valid Admin admin
) {
    public record Admin(@NotBlank @Email String email, String firstName, String lastName) {}
}
