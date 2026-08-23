package ro.ecoregistru.controller.response;

import java.util.UUID;

public record InternalGeneratorResponse(
        UUID id,
        UUID workPointId,
        String workPointName,
        String name,
        String description,
        boolean active
) {}
