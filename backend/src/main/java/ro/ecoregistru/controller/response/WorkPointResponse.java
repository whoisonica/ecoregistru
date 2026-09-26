package ro.ecoregistru.controller.response;

import java.util.UUID;

public record WorkPointResponse(
        UUID id,
        String name,
        String address,
        boolean active,
        String environmentalAuthNumber,
        java.time.LocalDate environmentalAuthExpiry,
        String receivedFormsSeries
) {}
