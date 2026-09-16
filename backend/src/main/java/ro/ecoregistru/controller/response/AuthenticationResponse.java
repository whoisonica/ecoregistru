package ro.ecoregistru.controller.response;

import lombok.Builder;
import ro.ecoregistru.enums.Role;

import java.util.UUID;

@Builder(toBuilder = true)
public record AuthenticationResponse(
        String token,
        Role role,
        UUID tenantId,
        String tenantName,
        /** P2.13 — set for a CONSULTANT, who has no tenant of their own until they pick one. */
        String consultancyName,
        String email,
        /**
         * G1 — numai când cererea a numit un dispozitiv (aplicația mobilă). Null pentru web, unde
         * nimic nu s-ar putea face cu el: browserul n-are Keychain, iar `localStorage` e exact locul
         * din care P0.4 a scos sesiunile de o lună.
         */
        String refreshToken,
        /** G1 — care rând din „Dispozitive conectate” e telefonul ăsta. Null pentru web. */
        UUID deviceSessionId
) {}
