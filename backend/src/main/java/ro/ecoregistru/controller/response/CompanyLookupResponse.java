package ro.ecoregistru.controller.response;

import ro.ecoregistru.service.AnafClient;

/**
 * What ANAF knows about a CUI, for filling in a form. Nothing here is saved: the form shows it, the
 * user checks it, and only what they then save reaches a partner.
 */
public record CompanyLookupResponse(
        String cui,
        String name,
        String address,
        String tradeRegisterNumber,
        String caenCode,
        String county,
        String city,
        String registrationStatus,
        boolean inactive) {

    public static CompanyLookupResponse from(AnafClient.Company c) {
        return new CompanyLookupResponse(c.cui(), c.name(), c.address(), c.tradeRegisterNumber(),
                c.caenCode(), c.county(), c.city(), c.registrationStatus(), c.inactive());
    }
}
