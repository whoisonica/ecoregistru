package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotNull;
import ro.ecoregistru.enums.SubscriptionPaymentMethod;

/** F3 — the client's choice on {@code /abonament}. */
public record PaymentMethodRequest(@NotNull SubscriptionPaymentMethod paymentMethod) {}
