package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank String code
) {}
