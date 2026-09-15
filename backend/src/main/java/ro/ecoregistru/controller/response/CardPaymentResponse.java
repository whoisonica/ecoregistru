package ro.ecoregistru.controller.response;

import ro.ecoregistru.enums.CardPaymentStatus;
import ro.ecoregistru.enums.InvoiceStatus;

import java.util.UUID;

/** F3 — what {@code /abonament?plata=…} shows once Netopia sends the client back. */
public record CardPaymentResponse(UUID id, CardPaymentStatus status, String error, UUID invoiceId,
                                  InvoiceStatus invoiceStatus) {}
