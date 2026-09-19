package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkPointRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 512) String address
) {}
