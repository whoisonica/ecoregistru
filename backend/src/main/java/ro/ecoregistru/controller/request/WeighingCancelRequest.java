package ro.ecoregistru.controller.request;

/** Anularea unei operațiuni de depozit: motivul e obligatoriu și rămâne pe operațiune. */
public record WeighingCancelRequest(String reason) {
}
