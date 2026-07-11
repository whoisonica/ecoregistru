package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotBlank;

public record WorkPointRequest(
        @NotBlank String name,
        String address
) {}
