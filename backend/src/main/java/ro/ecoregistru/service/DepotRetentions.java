package ro.ecoregistru.service;

import ro.ecoregistru.entity.WasteMovement;
import ro.ecoregistru.entity.WeighingOperation;
import ro.ecoregistru.enums.WeighingOperationType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * D1.9 și D1.10 — reținerile la sursă de pe o intrare de depozit. Singurul loc unde stau cele două
 * cote; se calculează la finalizare și se păstrează pe operațiune (V46 + V51), ca o cotă schimbată
 * mâine să nu rescrie ce s-a reținut ieri.
 *
 * <p><b>Cotele sunt fixe prin lege, nu setări pe firmă</b> ({@code surse-oficiale.md} §18.1, C1):
 * <ul>
 *   <li><b>2% AFM</b> — OUG 196/2005 art. 9 alin. (1) lit. a): „2% din veniturile realizate din
 *       vânzarea deşeurilor, obţinute de către deţinătorul deşeurilor, persoană fizică sau juridică”,
 *       reținuți la sursă de colector. Fără nicio scutire: și de la persoane fizice, și la hârtie sau
 *       plastic, nu doar la metale. Baza e „valoarea de vânzare, exclusiv taxa pe valoarea adăugată”
 *       (art. 10 alin. (5)); prețurile depozitului se țin oricum fără TVA (taxare inversă,
 *       Codul fiscal art. 331). Se declară și se plătește lunar, până pe 25 (art. 11 alin. (1)).</li>
 *   <li><b>10% impozit pe venit</b> — Codul fiscal art. 115 alin. (1) lit. a) pe veniturile de la
 *       art. 114 alin. (2) lit. m^2), adică deșeurile metalice din patrimoniul personal, cumpărate de
 *       la o persoană fizică. Impozit final (alin. (2)), plătit până pe 25 a lunii următoare
 *       (alin. (3)), declarat pe fiecare beneficiar până în ultima zi a lui februarie (art. 132
 *       alin. (2)). Hârtia, plasticul și acumulatorii de la o persoană fizică rămân neimpozabili
 *       (art. 62 lit. f), §12).</li>
 * </ul>
 *
 * <p><b>Cele două nu se scad una din alta:</b> amândouă se calculează pe valoarea brută (§18.1).
 * Rotunjirea se face o singură dată, pe totalul operațiunii, nu pe fiecare linie.
 *
 * <p>Ieșirile nu rețin nimic: acolo depozitul e vânzătorul, iar cei 2% îi reține cumpărătorul.
 */
public final class DepotRetentions {

    /** OUG 196/2005 art. 9 alin. (1) lit. a). */
    public static final BigDecimal AFM_RATE = new BigDecimal("0.02");

    /** Codul fiscal art. 115 alin. (1) lit. a), în vigoare din 01.01.2026 (OUG 89/2025). */
    public static final BigDecimal INCOME_TAX_RATE = new BigDecimal("0.10");

    private DepotRetentions() {
    }

    /**
     * @param afmBase       valoarea întregii intrări, fără TVA
     * @param afm           2% din ea
     * @param incomeTaxBase valoarea liniilor de metal, când vânzătorul e o persoană fizică
     * @param incomeTax     10% din ea
     */
    public record Amounts(BigDecimal afmBase, BigDecimal afm, BigDecimal incomeTaxBase, BigDecimal incomeTax) {
    }

    /** Ce se reține dintr-o operațiune finalizată. O ieșire iese cu zerouri. */
    public static Amounts of(WeighingOperation operation, List<WasteMovement> lines) {
        if (operation.getType() != WeighingOperationType.IN) {
            return new Amounts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        boolean fromIndividual = operation.getNaturalPerson() != null;
        BigDecimal afmBase = BigDecimal.ZERO;
        BigDecimal metalBase = BigDecimal.ZERO;
        for (WasteMovement line : lines) {
            BigDecimal value = line.getTotalValue();
            if (value == null) {
                continue;
            }
            afmBase = afmBase.add(value);
            if (fromIndividual && line.getArticle() != null && line.getArticle().isMetal()) {
                metalBase = metalBase.add(value);
            }
        }
        return new Amounts(money(afmBase), share(afmBase, AFM_RATE),
                money(metalBase), share(metalBase, INCOME_TAX_RATE));
    }

    private static BigDecimal share(BigDecimal base, BigDecimal rate) {
        return money(base.multiply(rate));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
