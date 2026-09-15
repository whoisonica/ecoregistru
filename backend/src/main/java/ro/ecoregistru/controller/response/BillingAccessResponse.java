package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.SubscriptionStatus;

import java.time.LocalDate;

/**
 * F4 — for the banner and for hiding the write buttons, on every screen and for every role: the status of
 * whoever pays for the account. Null status: nobody pays, nothing is restricted.
 *
 * @param readOnly   writes are refused right now ({@code SubscriptionAccessFilter})
 * @param readOnlyOn the day writes stop if the oldest unpaid invoice stays unpaid; null when nothing is late
 */
public record BillingAccessResponse(SubscriptionStatus status, boolean readOnly, LocalDate readOnlyOn) {}
