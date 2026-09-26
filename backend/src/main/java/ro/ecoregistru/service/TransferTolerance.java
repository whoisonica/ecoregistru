package ro.ecoregistru.service;

import java.math.BigDecimal;

/**
 * D2.5 — cât pot diferi, legal, cântărirea de la plecare și cea de la recepție a unui transfer.
 *
 * <p>Eroarea maximă tolerată a unui cântar neautomat e în HG 710/2015 anexa 1 tabelul 3: 0,5 e, 1 e sau 1,5 e,
 * după câte diviziuni „e” are încărcătura și după clasa de precizie; pct. 4.2 o <b>dublează în exploatare</b>. Un
 * net din brut și tara vine din două cântăriri, deci poartă eroarea de două ori. Cele două cântare pot greși în
 * sensuri opuse: toleranța transferului e suma lor (`surse-oficiale.md` §4, runda 2).
 *
 * <p>Fără clasa sau diviziunea unui cântar (sau fără cântar), toleranța e necunoscută: {@code null}, iar orice
 * diferență se explică.
 */
public final class TransferTolerance {

    private TransferTolerance() {
    }

    /**
     * O parte a transferului: cântarul (clasa, „e”), încărcătura cea mai mare cântărită și dacă netul a ieșit din două
     * cântăriri (brut și tara).
     */
    public record Side(String accuracyClass, BigDecimal divisionKg, BigDecimal loadKg, boolean twoWeighings) {
    }

    /** Suma toleranțelor celor două părți, sau {@code null} când una nu se poate calcula. */
    public static BigDecimal of(Side departure, Side reception) {
        BigDecimal a = side(departure);
        BigDecimal b = side(reception);
        return a == null || b == null ? null : a.add(b);
    }

    private static BigDecimal side(Side s) {
        if (s == null || s.loadKg() == null) {
            return null;
        }
        BigDecimal one = inService(s.accuracyClass(), s.divisionKg(), s.loadKg());
        return one == null ? null : s.twoWeighings() ? one.multiply(BigDecimal.TWO) : one;
    }

    /** Eroarea maximă tolerată în exploatare pentru o cântărire: 2 × (0,5 | 1 | 1,5) × e. */
    public static BigDecimal inService(String accuracyClass, BigDecimal divisionKg, BigDecimal loadKg) {
        if (accuracyClass == null || divisionKg == null || divisionKg.signum() <= 0 || loadKg == null) {
            return null;
        }
        long[] steps = switch (accuracyClass) {
            case "I" -> new long[]{50_000, 200_000};
            case "II" -> new long[]{5_000, 20_000};
            case "III" -> new long[]{500, 2_000};
            case "IIII" -> new long[]{50, 200};
            default -> null;
        };
        if (steps == null) {
            return null;
        }
        BigDecimal divisions = loadKg.abs().divide(divisionKg, 6, java.math.RoundingMode.HALF_UP);
        BigDecimal factor = divisions.compareTo(BigDecimal.valueOf(steps[0])) <= 0 ? new BigDecimal("0.5")
                : divisions.compareTo(BigDecimal.valueOf(steps[1])) <= 0 ? BigDecimal.ONE
                : new BigDecimal("1.5");
        return factor.multiply(divisionKg).multiply(BigDecimal.TWO);
    }
}
