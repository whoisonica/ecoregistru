package ro.ecoregistru.controller.request;

/** G2 — tokenul Expo Push al telefonului; {@code null} îl șterge. Forma o verifică serviciul. */
public record PushTokenRequest(String token) {}
