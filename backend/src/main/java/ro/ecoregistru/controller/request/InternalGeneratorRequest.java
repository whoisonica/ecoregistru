package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Create/update payload for an internal generator. The work point is required and cannot be
 * changed by an update: moving a section between work points would silently rewrite the "Secţia"
 * column of Anexa 1 sheets already printed for the old one.
 */
public record InternalGeneratorRequest(
        @NotNull UUID workPointId,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description
) {}
