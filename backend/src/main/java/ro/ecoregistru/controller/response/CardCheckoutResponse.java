package ro.ecoregistru.controller.response;

/** F3 — where to send the client: Netopia's payment page. */
public record CardCheckoutResponse(String paymentUrl) {}
