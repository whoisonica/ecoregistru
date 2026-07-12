package ro.ecoregistru.controller.response;

import lombok.Builder;
import ro.ecoregistru.enums.Role;

import java.util.UUID;

@Builder
public record AuthenticationResponse(
        String token,
        Role role,
        UUID tenantId,
        String tenantName,
        String email
) {}
