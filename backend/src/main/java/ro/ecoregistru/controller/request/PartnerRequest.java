package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.PartnerType;

import java.time.LocalDate;

public record PartnerRequest(
        @NotBlank String name,
        String cui,
        String authorizationNumber,
        LocalDate authorizationExpiry,
        @NotNull PartnerType type
) {}
