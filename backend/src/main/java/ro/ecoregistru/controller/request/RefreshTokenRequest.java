package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotBlank;

/** G1 — tokenul de reîmprospătare, la `/auth/refresh` și la `/auth/logout`. */
public record RefreshTokenRequest(@NotBlank String refreshToken) {}
