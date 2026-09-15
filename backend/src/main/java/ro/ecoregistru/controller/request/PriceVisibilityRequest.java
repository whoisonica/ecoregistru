package ro.ecoregistru.controller.request;

import ro.ecoregistru.enums.PriceVisibility;

/** D1.8 — cine vede prețurile depozitului. O schimbă doar adminul firmei. */
public record PriceVisibilityRequest(PriceVisibility priceVisibility) {
}
