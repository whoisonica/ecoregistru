package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

/** Anularea unei operațiuni de depozit: motivul e obligatoriu și rămâne pe operațiune. */
public record WeighingCancelRequest(@Size(max = 1000) String reason) {
}
