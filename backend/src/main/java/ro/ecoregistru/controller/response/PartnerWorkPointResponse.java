package ro.ecoregistru.controller.response;

import java.util.UUID;

/** One work point of a partner: what Anexa 3 can print as the recipient's unloading place. */
public record PartnerWorkPointResponse(UUID id, String name, String address) {}
