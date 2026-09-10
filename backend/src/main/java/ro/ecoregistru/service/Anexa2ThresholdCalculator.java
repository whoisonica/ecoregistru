package ro.ecoregistru.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import ro.ecoregistru.controller.response.Anexa2ThresholdResponse;
import ro.ecoregistru.controller.response.MonthlyEvidenceResponse;
import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.util.WasteCodeLabel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Predicate;

/**
 * The 1 t/an threshold of HG 1061/2008 — the one figure on Anexa 2 that we compute rather than
 * copy, and the one place on the form where we could be wrong.
 *
 * <p><b>What the act says, and where it stops.</b> Art. 6 alin. (1) removes the agency's approval
 * for "deşeurile periculoase generate în cantitate mai mică de 1 t/an"; art. 7 requires it above,
 * "din aceeaşi categorie de deşeuri periculoase"; art. 15 alin. (1) repeats the phrase and asks
 * that the form say so clearly — which the model's own tick does. But <b>"categorie" is never
 * defined</b>: art. 2 sends every definition to "anexa nr. I A la OUG nr. 78/2000", repealed by
 * Legea 211/2011 and then by OUG 92/2021, so the chain is broken and no public source rebuilds it.
 *
 * <p><b>Why that matters in one direction only.</b> Cumulating per six-digit code is the narrowest
 * reading. If "categorie" is wider — the four-digit group, or the chapter — the per-code total
 * <em>underestimates</em>, the tick goes to "&lt; 1t/an", and the transport travels without the
 * approval art. 7 requires: 10.000–20.000 lei under art. 25 alin. (2) lit. b). The opposite error
 * costs an approval nobody needed. So the calculator returns <b>both</b> totals, proposes from the
 * narrow one, and raises {@code groupWarning} exactly in the case where the wider reading would
 * change the answer. It never decides: the tick stays editable and what the client answers is what
 * is printed.
 *
 * <p><b>"1 t/an" is read as the calendar year</b>, which is also our choice and not the act's. It
 * is the one the rest of the evidence already makes — Anexa 1 and the annual declaration are both
 * per calendar year — and a threshold measured on a different clock than the evidence it is read
 * from would be impossible to check.
 *
 * <p><b>The figure comes from the evidence engine, not from a query of its own.</b> That is
 * deliberate: {@link EvidenceCalculator} is where "generated" is defined, and its definition
 * includes the generation nobody wrote down (V24) — the quantity implied by what left the site.
 * A client who records only handovers has a real generated total and a naive
 * {@code sum(GENERATED)} would report zero for them, i.e. it would underestimate, in the one
 * direction that costs. Reading the engine also means the tick and the fişa can never disagree.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Anexa2ThresholdCalculator {

    /** The threshold itself, in tonnes. */
    private static final BigDecimal ONE_TON = BigDecimal.ONE;
    /** Evidence lines are kept in kilograms; the form prints tonnes. */
    private static final int KG_TO_TONS = 3;
    /** Three decimals on a tonne is a kilogram — the precision the evidence itself carries. */
    private static final int TON_SCALE = 3;

    EvidenceCalculator evidenceCalculator;

    public Anexa2ThresholdResponse forMovement(WasteMovement movement) {
        String code = movement.getWasteCode().getCode();
        String group = groupOf(code);
        int year = movement.getDate().getYear();

        List<MonthlyEvidenceResponse> lines = evidenceCalculator.list(year, null, null);
        BigDecimal codeTons = tons(lines, l -> l.wasteCode().equals(code));
        BigDecimal groupTons = tons(lines, l -> groupOf(l.wasteCode()).equals(group));

        boolean proposed = codeTons.compareTo(ONE_TON) < 0;
        Boolean chosen = movement.getAnexa2BelowOneTon();
        boolean effective = chosen != null ? chosen : proposed;

        return new Anexa2ThresholdResponse(
                year,
                WasteCodeLabel.official(code, movement.getWasteCode().isHazardous()),
                codeTons,
                group,
                groupTons,
                proposed,
                chosen,
                effective,
                proposed && groupTons.compareTo(ONE_TON) >= 0);
    }

    /** What the form prints, once the client's answer has had the last word over the proposal. */
    public boolean belowOneTon(WasteMovement movement) {
        return forMovement(movement).effectiveBelowOneTon();
    }

    private BigDecimal tons(List<MonthlyEvidenceResponse> lines,
                            Predicate<MonthlyEvidenceResponse> keep) {
        BigDecimal kg = lines.stream()
                .filter(keep)
                .map(MonthlyEvidenceResponse::totalGenerated)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return kg.movePointLeft(KG_TO_TONS).setScale(TON_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * The four-digit group a six-digit code sits in — {@code "08 01 17"} is in {@code "08 01"}.
     * Not a claim that the group <em>is</em> the "categorie" of art. 7; it is the next wider
     * reading of a word the act leaves open, computed so the screen can warn when the two readings
     * disagree.
     */
    static String groupOf(String code) {
        return code == null || code.length() < 5 ? code : code.substring(0, 5);
    }
}
