package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * F5 — fișa de balotare: pe ce depozit, în ce zi, ce sortiment balotat și câți baloți. Kilogramele nu se scriu: le dă
 * greutatea standard a balotului, de pe sortiment (proprietarul, 27.09.2026).
 */
public record BalingRequest(
        UUID workPointId,
        LocalDate date,
        UUID articleId,
        Integer baleCount,
        @Size(max = 1000) String notes) {
}
