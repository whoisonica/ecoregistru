package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;
import ro.ecoregistru.enums.ScaleEventKind;

import java.time.LocalDate;

/**
 * Un rând în istoricul cântarului.
 *
 * @param admitted       doar la verificare: ADMIS (true) sau RESPINS (false)
 * @param bulletinNumber doar la verificare, obligatoriu
 * @param validUntil     doar la ADMIS; lipsă = data + 12 luni; mai mult de atât se refuză
 */
public record ScaleEventRequest(
        ScaleEventKind kind,
        LocalDate date,
        Boolean admitted,
        @Size(max = 60) String bulletinNumber,
        LocalDate validUntil,
        @Size(max = 255) String laboratory,
        @Size(max = 255) String verifier,
        @Size(max = 1000) String notes) {
}
