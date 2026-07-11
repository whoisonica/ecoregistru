package ro.ecoregistru.controller.response;

import java.util.UUID;

public record WasteCodeResponse(
        UUID id,
        String code,
        String name,
        boolean hazardous
) {}
