package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.SubscriptionPlan;
import ro.ecoregistru.service.BillingCalculator.Invoice;

/** F-C — what the final summary says: the company, the subscription (null without one) and who was invited. */
public record OnboardClientResponse(
        CompanyResponse company,
        SubscriptionPlan plan,
        Invoice firstInvoice,
        String invitedEmail,
        /** BUG-038 — null without an invite, false when its mail did not go out. */
        Boolean inviteEmailSent
) {}
