package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * P2.13 — invite a consultant onto a consultancy. No role: inside a consultancy everybody is a
 * {@code CONSULTANT}, with the same rights (decided 14.09.2026 — a role can be added later).
 */
public record InviteConsultantRequest(
        @NotBlank @Email String email,
        String firstName,
        String lastName
) {}
