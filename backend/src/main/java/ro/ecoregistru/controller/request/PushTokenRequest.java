package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

/** G2 — tokenul Expo Push al telefonului; {@code null} îl șterge. Forma o verifică serviciul. */
public record PushTokenRequest(@Size(max = 255) String token) {}
