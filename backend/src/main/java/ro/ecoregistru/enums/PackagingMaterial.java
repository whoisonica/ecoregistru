package ro.ecoregistru.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The material rows of Anexa 1 Ambalaje (Ordinul 794/2012), in the order the form prints them.
 *
 * <p>Verbatim from the two files received from the specialist —
 * {@code RAPORTARE AMBALAJE _anexa 1.xlsx} (blank) and {@code RAPORTARE AMBALAJE 2021_anexa 1_ HRR.xlsx}
 * (filled): Sticlă · PET · Alte plastice · <i>Total plastic</i> · Hârtie carton · Aluminiu · Oţel ·
 * <i>Total metal</i> · Lemn · Altele · <i>TOTAL</i>. The three italic rows are sums, computed when
 * the form is drawn rather than stored — {@link #plasticParts()} and {@link #metalParts()} say
 * which rows feed which sum, and Sticlă, Hârtie carton, Lemn and Altele stand on their own.
 *
 * <p><b>How a quantity finds its row.</b> Not from the waste code alone — the European List does
 * not carry the distinction the form asks for. {@code 15 01 04} is "ambalaje metalice" and covers
 * both an aluminium can and a steel drum; {@code 15 01 02} is "ambalaje de materiale plastice" and
 * covers both a PET bottle and a plastic crate. So the material is <b>chosen on the movement</b>,
 * and the code only <i>proposes</i> it — see {@link #suggestedFor(String)}.
 *
 * <p><b>Why "Altele" is not the fallback.</b> It was, until 25.08.2026: everything the code could
 * not place landed there and the form printed a line naming the codes that did. The specialist's
 * hint, relayed by the user the same day, was that "Altele" should in practice stay empty — and
 * reading the order itself turned that hint into a rule with a citation. <b>Art. 8 alin. (1)
 * lit. d)</b>, verbatim: <i>"În coloana «material», rubrica «altele» va cuprinde numai alte
 * materiale decât cele nominalizate în coloana 0."</i> Metal packaging ({@code 15 01 04}) and
 * plastic packaging ({@code 15 01 02}) are nominated materials, so sweeping them into "Altele" was
 * against the act, not merely against practice. An unanswered movement now stays <b>out</b> of the
 * table and is listed as such in the packaging tab. Same house rule as everywhere else: what is
 * missing is shown to be missing.
 *
 * <p><b>Composites too.</b> Art. 8 alin. (1) lit. b): <i>"Ambalajele din materiale compozite se
 * raportează în funcţie de materialul preponderent."</i> So {@code 15 01 05} does not belong in
 * "Altele" either — it belongs on whichever material predominates, which only the client can say.
 * That is why the code proposes nothing for it and the movement asks.
 *
 * <p>Lit. e) adds that the wood row covers cork as well, and alin. (2) that exported packaging and
 * packaging in transit are left out of the report entirely — neither is decided here, but both are
 * written down in {@code docs/surse-oficiale.md} §5.2 so nobody has to rediscover them.
 */
@Getter
public enum PackagingMaterial {

    STICLA("Sticlă", Set.of("15 01 07")),
    PET("PET", Set.of()),
    ALTE_PLASTICE("Alte plastice", Set.of()),
    HARTIE_CARTON("Hârtie carton", Set.of("15 01 01")),
    ALUMINIU("Aluminiu", Set.of()),
    OTEL("Oţel", Set.of()),
    LEMN("Lemn", Set.of("15 01 03")),
    /** Kept because the form has the row. Only ever used when the client picks it deliberately. */
    ALTELE("Altele", Set.of());

    private final String officialLabel;

    /**
     * European List codes that settle this row on their own, with no question asked. Deliberately
     * short: only where the code names exactly one of the form's materials.
     */
    private final Set<String> wasteCodes;

    PackagingMaterial(String officialLabel, Set<String> wasteCodes) {
        this.officialLabel = officialLabel;
        this.wasteCodes = wasteCodes;
    }

    /**
     * Whether this code belongs to the packaging declaration at all.
     *
     * <p>Only chapter 15 01 counts. A shop's cardboard recorded under 20 01 01 does <b>not</b> feed
     * this declaration — which is exactly the distinction the specialist drew on 24.08.2026:
     * "cartonul din magazine este 15 01 01", and it is the code chosen at recording that decides.
     */
    public static boolean isPackagingCode(String code) {
        return code != null && code.startsWith("15 01");
    }

    /**
     * The material a code proposes, for the movement form to pre-select — empty when the code does
     * not settle it and the client has to say.
     *
     * <p>{@code 15 01 02} proposes <i>Alte plastice</i> rather than PET: a PET bottle and a plastic
     * crate share the code, and PET is the narrower claim, so it is the client's to make.
     */
    public static Optional<PackagingMaterial> suggestedFor(String code) {
        if (!isPackagingCode(code)) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(m -> m.wasteCodes.contains(code))
                .findFirst()
                .or(() -> "15 01 02".equals(code) ? Optional.of(ALTE_PLASTICE) : Optional.empty());
    }

    /**
     * The row a movement's quantity counts in: what the client chose, and failing that what the
     * code proposes. Empty means nobody has answered — the quantity stays off the printed table.
     */
    public static Optional<PackagingMaterial> resolve(PackagingMaterial chosen, String code) {
        return chosen != null ? Optional.of(chosen) : suggestedFor(code);
    }

    /** The rows that are sums of the ones above them, in the order the form draws them. */
    public static List<PackagingMaterial> plasticParts() {
        return List.of(PET, ALTE_PLASTICE);
    }

    public static List<PackagingMaterial> metalParts() {
        return List.of(ALUMINIU, OTEL);
    }
}
