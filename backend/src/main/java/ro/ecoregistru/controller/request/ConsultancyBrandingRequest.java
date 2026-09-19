package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

/** P2.14 — the header line; empty or null removes it. Its length is checked in the service, with a message. */
public record ConsultancyBrandingRequest(@Size(max = 200) String headerLine) {}
