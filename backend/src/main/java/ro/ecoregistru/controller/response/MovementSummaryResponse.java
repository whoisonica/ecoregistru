package ro.ecoregistru.controller.response;

import java.math.BigDecimal;

/**
 * Cât s-a înregistrat într-o lună, în două cifre — pentru Panou.
 *
 * <p>Există fiindcă paginarea (P3.1) a luat de pe masă singurul fel în care panoul afla lucrurile
 * astea: până acum cerea <b>toate</b> mișcările lunii și le aduna în browser. Cu o pagină de 25 de
 * rânduri, aceeași adunare ar fi dat un număr mai mic decât adevărul și n-ar fi spus-o — un total
 * greșit pe un ecran care se cheamă „Panou" e mai rău decât un total lipsă.
 *
 * <p>Suma se face în baza de date și <b>în kilograme</b>: fiecare mișcare își poartă unitatea, iar
 * adunarea cantităților brute punea 1000 kg și 1 tonă cap la cap ca 1001. E aceeași normalizare pe
 * care o face {@code EvidenceCalculator.toKg} de mult.
 *
 * @param movements  câte mișcări s-au înregistrat în lună — cifra care spune dacă evidența se ține
 *                   la zi, și nu se poate citi din lungimea unei pagini
 * @param quantityKg suma cantităților lor, în kg. Mișcările plecate fără cântar nu au cantitate și
 *                   nu intră în sumă — ele se văd ca „de cântărit" pe rândul lor, nu ca un zero aici
 */
public record MovementSummaryResponse(long movements, BigDecimal quantityKg) {
}
