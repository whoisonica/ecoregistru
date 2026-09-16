package ro.ecoregistru.controller.response;

/**
 * Result of completing the calendar with the next deadline of each kind. Generation is additive
 * and idempotent, so {@code generated} counts only the newly created deadlines.
 */
public record DeadlineGenerationResponse(
        int generated
) {}
