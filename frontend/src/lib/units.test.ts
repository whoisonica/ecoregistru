import assert from "node:assert/strict";
import { test } from "node:test";
import { formatKg, formatQuantity, formatTonnesValue } from "@/lib/units";

test("pragul în tone se arată cu trei zecimale, în formatul românesc", () => {
  assert.equal(formatTonnesValue(0.84), "0,840");
  // A treia zecimală e kilogramul: 1 kg nu dispare la formatare.
  assert.equal(formatTonnesValue(0.001), "0,001");
});

test("kilogramele se scriu cu punct la mii și întotdeauna trei zecimale", () => {
  assert.equal(formatKg(1060), "1.060,000");
  assert.equal(formatKg(450.5), "450,500");
  assert.equal(formatKg(0), "0,000");
});

/**
 * G06: miezul regulii. „35.125" și „35,125" sunt două numere care diferă de o mie de ori, iar
 * până pe 20.09.2026 aceeași cantitate se scria în amândouă felurile, pe ecrane diferite. Cu trei
 * zecimale mereu, virgula e ultima și punctul e la mii — nu mai e nimic de ghicit.
 */
test("aceeași cantitate nu se mai poate citi de o mie de ori mai mare sau mai mică", () => {
  assert.equal(formatKg(35125), "35.125,000");
  assert.equal(formatKg(35.125), "35,125");
  assert.notEqual(formatKg(35125), formatKg(35.125));
});

/**
 * Continuarea lui G06, pe ecran (20.09.2026). Lista de mişcări tipărea cifra brută din JSON —
 * „35.125", cum o scrie JavaScript — iar deasupra ei banda de totaluri scria „12.640", adică
 * douăsprezece mii, rotunjite dinadins fiindcă e un rezumat. Cele două stăteau una sub alta şi
 * nu se puteau deosebi. Orice cifră de transcris trece acum prin `formatQuantity`, deci are mereu
 * virgulă şi trei zecimale — iar lipsa virgulei e chiar semnul că sus e un rezumat.
 *
 * Controlul negativ: cu `String(value)` în loc de formatare, prima verificare de jos cade cu
 * „35.125".
 */
test("o cantitate de pe un rând se scrie în unitatea ei, cu trei zecimale", () => {
  assert.equal(formatQuantity(35.125, "KG"), "35,125");
  assert.equal(formatQuantity(12640, "KG"), "12.640,000");
  // Tonele rămân tone: rândul spune el în ce unitate e, nu se converteşte pe ascuns.
  assert.equal(formatQuantity(1.06, "TONS"), "1,060");
  assert.notEqual(formatQuantity(35.125, "KG"), String(35.125));
});
