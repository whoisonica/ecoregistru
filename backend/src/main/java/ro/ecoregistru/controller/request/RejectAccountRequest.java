package ro.ecoregistru.controller.request;

/** Why a request was declined. Kept on the request, which is never deleted. */
public record RejectAccountRequest(String reason) {}
