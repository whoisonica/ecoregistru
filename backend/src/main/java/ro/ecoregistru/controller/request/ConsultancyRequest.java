package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** P2.13 — a new consultancy. CUI format is checked in the service, where it is also normalised. */
public record ConsultancyRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 32) String cui
) {}
