package ro.ecoregistru.controller.response;

import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        String url,
        String fileName,
        String contentType
) {}
