package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * P2.13 — invite a consultant onto a consultancy. No role: inside a consultancy everybody is a
 * {@code CONSULTANT}, with the same rights (decided 14.09.2026 — a role can be added later).
 */
public record InviteConsultantRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 128) String firstName,
        @Size(max = 128) String lastName
) {}
