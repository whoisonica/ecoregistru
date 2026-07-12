package ro.ecoregistru.controller.response;

/**
 * Result of (re)generating a year's reporting deadlines. Generation is additive and
 * idempotent, so {@code generated} counts only the newly created deadlines.
 */
public record DeadlineGenerationResponse(
        int year,
        int generated
) {}
