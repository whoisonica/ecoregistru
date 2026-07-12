package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.Role;

import java.util.UUID;

/**
 * A user belonging to a tenant company. Returned when a platform admin invites a user.
 * `enabled` stays false until the invitee sets their password via the reset-password link.
 */
public record CompanyUserResponse(
        UUID id,
        String email,
        Role role,
        String firstName,
        String lastName,
        boolean enabled
) {}
