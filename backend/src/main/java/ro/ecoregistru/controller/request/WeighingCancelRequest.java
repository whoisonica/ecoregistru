package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.Size;

/**
 * Anularea unei operațiuni de depozit: motivul e obligatoriu și rămâne pe operațiune.
 *
 * @param confirmPastPeriod omul a confirmat că schimbă o lună încheiată (D2 din 28.09); lipsă = nu
 */
public record WeighingCancelRequest(@Size(max = 1000) String reason, Boolean confirmPastPeriod) {
}
