package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * One driver, as a form sends it. Used both nested in {@link PartnerRequest} — a carrier's own
 * drivers — and on its own for the drivers of our firm.
 *
 * @param id null for a new one; an existing id keeps the row, so the movements that already
 *           prefilled from him are unaffected by an edit
 */
public record DriverRequest(
        UUID id,
        @Size(max = 255) String name,
        @Size(max = 100) String identification,
        @Size(max = 50) String vehicleRegistration
) {}
