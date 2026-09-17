package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.service.BillingCalculator.Invoice;

/** F-C — what the final summary says: the company, the subscription (null without one) and who was invited. */
public record OnboardClientResponse(
        CompanyResponse company,
        SubscriptionPlan plan,
        Invoice firstInvoice,
        String invitedEmail
) {}
