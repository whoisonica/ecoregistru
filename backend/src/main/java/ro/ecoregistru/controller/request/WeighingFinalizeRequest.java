package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

/**
 * Finalizarea unei operațiuni. Corpul e opțional: îl trimite ecranul doar când cântarul nu era legal
 * la data cântăririi (D2.3).
 *
 * @param scaleReason de ce se finalizează totuși; rămâne pe operațiune și în jurnal
 */
public record WeighingFinalizeRequest(@Size(max = 1000) String scaleReason) {
}
