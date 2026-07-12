package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.CompanyType;

import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String cui,
        CompanyType type,
        boolean active
) {}
