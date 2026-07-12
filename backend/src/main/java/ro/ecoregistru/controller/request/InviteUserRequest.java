package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.Role;

/**
 * Invite a user onto a tenant company. Platform-admin only. The role must be a tenant
 * role (ADMIN/OPERATOR/CLIENT_VIEWER) — never PLATFORM_ADMIN (rejected as INVALID_INVITE_ROLE).
 */
public record InviteUserRequest(
        @NotBlank @Email String email,
        @NotNull Role role,
        String firstName,
        String lastName
) {}
