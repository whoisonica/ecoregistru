package ro.ecoregistru.enums;

import lombok.Getter;

import java.util.List;
import java.util.Set;

/**
 * The material rows of Anexa 1 Ambalaje (Ordinul 794/2012), in the order the form prints them.
 *
 * <p>Verbatim from the two files received from the specialist —
 * {@code RAPORTARE AMBALAJE _anexa 1.xlsx} (blank) and {@code RAPORTARE AMBALAJE 2021_anexa 1_ HRR.xlsx}
 * (filled): Sticlă · PET · Alte plastice · <i>Total plastic</i> · Hârtie carton · Aluminiu · Oţel ·
 * <i>Total metal</i> · Lemn · Altele · <i>TOTAL</i>. The three italic rows are sums, computed when
 * the form is drawn rather than stored.
 *
 * <p><b>Which waste codes feed which row.</b> Table 2 of that annex — the waste actually handed
 * over — can be filled from the movements already recorded, but only where the European List code
 * says the material without ambiguity. Two cases where it does not:
 *
 * <ul>
 *   <li><b>15 01 02</b> is "ambalaje de materiale plastice", full stop. A PET bottle and a plastic
 *       crate share it, so the quantity lands in <i>Alte plastice</i> and the client moves what is
 *       PET. Putting it in PET would be a guess on a filed form.</li>
 *   <li><b>15 01 04</b> is "ambalaje metalice" — aluminium cans and steel drums, same code. It
 *       lands in <i>Altele</i>, and the form prints a line naming the codes that ended up there,
 *       so the gap is visible instead of silent.</li>
 * </ul>
 */
@Getter
public enum PackagingMaterial {

    STICLA("Sticlă", Set.of("15 01 07")),
    PET("PET", Set.of()),
    ALTE_PLASTICE("Alte plastice", Set.of("15 01 02")),
    HARTIE_CARTON("Hârtie carton", Set.of("15 01 01")),
    ALUMINIU("Aluminiu", Set.of()),
    OTEL("Oţel", Set.of()),
    LEMN("Lemn", Set.of("15 01 03")),
    /** Everything the code cannot place: metals (15 01 04), composites, mixed, textile, ceramic. */
    ALTELE("Altele", Set.of("15 01 04", "15 01 05", "15 01 06", "15 01 09", "15 01 07*"));

    private final String officialLabel;
    /** European List codes that land in this row on their own. */
    private final Set<String> wasteCodes;

    PackagingMaterial(String officialLabel, Set<String> wasteCodes) {
        this.officialLabel = officialLabel;
        this.wasteCodes = wasteCodes;
    }

    /**
     * The row a packaging waste code belongs to, or empty when the code is not packaging at all.
     *
     * <p>Only chapter 15 01 counts. A shop's cardboard recorded under 20 01 01 does <b>not</b> feed
     * this declaration — which is exactly the distinction the specialist drew on 24.08.2026:
     * "cartonul din magazine este 15 01 01", and it is the code chosen at recording that decides.
     */
    public static java.util.Optional<PackagingMaterial> forWasteCode(String code) {
        if (code == null || !code.startsWith("15 01")) {
            return java.util.Optional.empty();
        }
        return java.util.Arrays.stream(values())
                .filter(m -> m.wasteCodes.contains(code))
                .findFirst()
                .or(() -> java.util.Optional.of(ALTELE));
    }

    /** True for the codes we cannot place on a material row without the client telling us. */
    public static boolean isAmbiguous(String code) {
        return ALTELE.wasteCodes.contains(code);
    }

    /** The rows that are sums of the ones above them, in the order the form draws them. */
    public static List<PackagingMaterial> plasticParts() {
        return List.of(PET, ALTE_PLASTICE);
    }

    public static List<PackagingMaterial> metalParts() {
        return List.of(ALUMINIU, OTEL);
    }
}
