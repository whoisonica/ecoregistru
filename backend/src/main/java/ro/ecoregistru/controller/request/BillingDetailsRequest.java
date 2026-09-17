package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * F-E — the billing data the client keeps up to date itself (contract art. 7.5). All four are required here:
 * FGO refuses an invoice without the county, the city and the address, and the invoices go to the email.
 * The name and the CUI are not in it: they are the company's, from ANAF, and they go on e-Factura.
 */
public record BillingDetailsRequest(
        @NotBlank @Email @Size(max = 100) String billingEmail,
        @NotBlank @Size(max = 100) String billingCounty,
        @NotBlank @Size(max = 100) String billingCity,
        @NotBlank @Size(max = 500) String billingAddress
) {}
