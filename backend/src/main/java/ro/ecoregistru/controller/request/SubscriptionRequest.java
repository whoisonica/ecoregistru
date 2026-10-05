package ro.ecoregistru.controller.request;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ro.ecoregistru.enums.SubscriptionPlan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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
 *
 * <p>05.10.2026 — {@code billingMonths}: 1 (monthly) or 12 (annual); absent means monthly, so a
 * client that does not send it keeps working. Annual always takes {@code monthlyPrice} from the
 * request, as the price per year.
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
        @Positive @Digits(integer = 8, fraction = 2) BigDecimal monthlyPrice,
        @BillingMonths Integer billingMonths
) {

    /** The length of the billing period, null meaning monthly. */
    public int months() {
        return billingMonths == null ? 1 : billingMonths;
    }

    /** Only 1 or 12; null is allowed and means 1. */
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = BillingMonths.Validator.class)
    public @interface BillingMonths {

        String message() default "Facturarea e lunară (1) sau anuală (12).";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        class Validator implements ConstraintValidator<BillingMonths, Integer> {
            @Override
            public boolean isValid(Integer value, ConstraintValidatorContext context) {
                return value == null || value == 1 || value == 12;
            }
        }
    }
}
