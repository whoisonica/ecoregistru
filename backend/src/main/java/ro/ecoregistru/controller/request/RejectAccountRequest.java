package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

/** Why a request was declined. Kept on the request, which is never deleted. */
public record RejectAccountRequest(@Size(max = 1000) String reason) {}
