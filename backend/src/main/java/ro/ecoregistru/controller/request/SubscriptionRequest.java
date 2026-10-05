package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ro.ecoregistru.enums.SubscriptionPlan;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * F1 — what the platform sets on a client. The prices come from the grid: the generator plans from
 * the employee-count tier ({@code sizeTier}, 1–5), the consultant from its constants. The request
 * carries a price only when the platform writes it by hand: {@code customPrice} on any plan, and
 * always on {@link SubscriptionPlan#FULL_SERVICE}, which is priced on request.
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
        @Size(max = 500) String billingAddress,
        @Min(1) @Max(5) Integer sizeTier,
        boolean customPrice,
        @Positive @Digits(integer = 8, fraction = 2) BigDecimal monthlyPrice
) {}
