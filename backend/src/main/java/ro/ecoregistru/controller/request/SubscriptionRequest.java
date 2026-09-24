package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.ecoregistru.enums.SubscriptionPlan;

import java.time.LocalDate;

/**
 * F1 — what the platform sets on a client. The prices come from the grid, never from the request.
 *
 * <p>F2 — the billing data FGO needs for a Romanian buyer. Optional: without the county, city and
 * address, the invoice stays DRAFT and says what is missing.
 */
public record SubscriptionRequest(
        @NotNull SubscriptionPlan plan,
        boolean founder,
        boolean twelveMonthCommitment,
        @NotNull LocalDate startedAt,
        @Email @Size(max = 100) String billingEmail,
        @Size(max = 100) String billingCounty,
        @Size(max = 100) String billingCity,
        @Size(max = 500) String billingAddress
) {}
