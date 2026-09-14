package ro.ecoregistru.controller.request;

import java.util.UUID;

/** P2.13 — the consultancy to hand a company to; {@code null} takes it back to a direct client. */
public record AssignConsultancyRequest(UUID consultancyId) {}
