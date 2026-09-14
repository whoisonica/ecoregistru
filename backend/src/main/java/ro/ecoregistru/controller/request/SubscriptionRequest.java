package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.SubscriptionPlan;

import java.time.LocalDate;

/** F1 — what the platform sets on a client. The prices come from the grid, never from the request. */
public record SubscriptionRequest(
        @NotNull SubscriptionPlan plan,
        boolean founder,
        @NotNull LocalDate startedAt
) {}
