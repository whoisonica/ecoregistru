package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * One work point of a partner, as the form sends it.
 *
 * @param id null for a new one; an existing id keeps the row, so a movement that already points at
 *           it — and the Anexa 3 already printed from that movement — still names the same place
 */
public record PartnerWorkPointRequest(
        UUID id,
        @Size(max = 255) String name,
        @Size(max = 500) String address
) {}
