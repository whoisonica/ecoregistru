package ro.ecoregistru.util;

import ro.ecoregistru.enums.SiatdModule;

import java.util.Optional;
import java.util.Set;

/**
 * Modulul SIATD al unui cod LER — de el depinde termenul de confirmare (spec F6, tabelul „Ce modul are o linie”).
 * Listele DEEE și baterii sunt explicite, ca {@link MetalWasteCodes}: restul capitolului 20 e „municipal” abia după ce
 * ele au fost scoase. {@code 16 06 01*} (acumulatorii auto) rămâne afară dinadins: SIATD numește doar bateriile și
 * acumulatorii <b>portabili</b>.
 */
public final class SiatdFlow {

    private static final Set<String> WEEE_MUNICIPAL = Set.of("20 01 21", "20 01 23", "20 01 35", "20 01 36");
    private static final Set<String> BATTERIES = Set.of("16 06 02", "16 06 03", "16 06 04", "16 06 05",
            "20 01 33", "20 01 34");

    private SiatdFlow() {
    }

    public static Optional<SiatdModule> of(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String plain = code.replace("*", "").trim();
        if (plain.startsWith("15 01 ")) {
            return Optional.of(SiatdModule.PACKAGING);
        }
        if (plain.equals("16 01 03")) {
            return Optional.of(SiatdModule.TYRE);
        }
        if (plain.startsWith("16 02 ") || WEEE_MUNICIPAL.contains(plain)) {
            return Optional.of(SiatdModule.WEEE);
        }
        if (BATTERIES.contains(plain)) {
            return Optional.of(SiatdModule.BATTERY);
        }
        if (plain.startsWith("20 ")) {
            return Optional.of(SiatdModule.MUNICIPAL);
        }
        return Optional.empty();
    }
}
