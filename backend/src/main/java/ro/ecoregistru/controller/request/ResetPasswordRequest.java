package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String code,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank String confirmPassword
) {}
