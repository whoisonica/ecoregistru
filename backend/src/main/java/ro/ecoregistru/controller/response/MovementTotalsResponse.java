package ro.ecoregistru.controller.response;

import java.math.BigDecimal;

/**
 * Cifrele de deasupra unei liste de mișcări — pe aceleași filtre ca lista, dar peste <b>toate</b>
 * rândurile ei, nu peste pagina adusă.
 *
 * <p>Există din același motiv ca {@link MovementSummaryResponse}: lista vine pe pagini (P3.1), deci
 * „Primit 12.640 kg" adunat din 25 de rânduri ar fi un total mai mic decât adevărul, care nu spune
 * că e mai mic. Toate cantitățile sunt în kilograme, normalizate în interogare (o mișcare în tone
 * se înmulțește cu 1000), ca la {@code EvidenceCalculator.toKg}.
 *
 * <p>Rândurile fără cantitate — plecate fără cântar, destinatarul cântărește la descărcare — nu
 * intră în nicio sumă; se numără la {@code awaitingWeighing}. Ecranul le scrie „de cântărit", nu zero.
 *
 * @param rows                 câte mișcări lasă filtrul să treacă
 * @param quantityKg           suma cantităților lor, în kg
 * @param awaitingWeighing     câte n-au încă o cantitate
 * @param recoveredKg          kg pe operațiunea de valorificare (cod R)
 * @param disposedKg           kg pe operațiunea de eliminare (cod D)
 * @param missingOperationCode câte ieșiri n-au cod R/D — ce blochează depunerea
 * @param fromNaturalPersonsKg kg intrate de la persoane fizice, prin operațiunea de cântar (D1.7);
 *                             restul intrărilor sunt de la firme
 * @param incomplete           câte predări n-au tot ce tipăresc rapoartele (decizia 19.09.2026)
 */
public record MovementTotalsResponse(long rows,
                                     BigDecimal quantityKg,
                                     long awaitingWeighing,
                                     BigDecimal recoveredKg,
                                     BigDecimal disposedKg,
                                     long missingOperationCode,
                                     BigDecimal fromNaturalPersonsKg,
                                     long incomplete) {
}
