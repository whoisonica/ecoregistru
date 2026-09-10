package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.Role;

/**
 * P1.12 — change a tenant member's role. Must be a tenant role (ADMIN/OPERATOR/CLIENT_VIEWER);
 * PLATFORM_ADMIN is refused as INVALID_INVITE_ROLE, the same rule the invite has.
 */
public record ChangeUserRoleRequest(@NotNull Role role) {}
