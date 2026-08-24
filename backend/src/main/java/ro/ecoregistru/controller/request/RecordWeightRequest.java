package ro.ecoregistru.controller.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ro.ecoregistru.enums.Unit;

import java.math.BigDecimal;

/**
 * The weight coming back from the recipient, for a load that left without one.
 *
 * <p>Asked for on 24.08.2026: "dacă bifezi «se cântărește la descărcare», ce faci după ce afli
 * cantitatea de la colector [...] să poți apăsa din mișcări să adaugi cantitatea ulterior, ne
 * încurcă la rapoarte și la anexe lipsa cantității". Until then the only way in was to edit the
 * movement and untick the box — which threw away the fact that the recipient did the weighing.
 *
 * <p>So this is a small, separate act with its own endpoint: it fills the quantity and nothing
 * else. {@code weighedAtUnloading} stays true, because it stays true — that <em>is</em> how this
 * load was weighed.
 *
 * @param unit the unit the figure came back in; null keeps the unit the movement was recorded with
 */
public record RecordWeightRequest(
        @NotNull @Positive BigDecimal quantity,
        Unit unit
) {}
