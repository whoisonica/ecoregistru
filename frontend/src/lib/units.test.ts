import assert from "node:assert/strict";
import { test } from "node:test";
import { formatKg, formatQuantity, formatQuantityInput, formatTonnesValue } from "@/lib/units";

test("pragul în tone se arată cu trei zecimale, în formatul românesc", () => {
  assert.equal(formatTonnesValue(0.84), "0,840");
  // A treia zecimală e kilogramul: 1 kg nu dispare la formatare.
  assert.equal(formatTonnesValue(0.001), "0,001");
});

/** 05.10.2026: „in generare a ramas 150,000 kg” — kg se tastează întregi din 30.09, deci fără „,000”. */
test("kilogramele întregi se scriu fără virgulă, cu punct la mii", () => {
  assert.equal(formatKg(150), "150");
  assert.equal(formatKg(1060), "1.060");
  assert.equal(formatKg(0), "0");
  // O sumă de zecimale care iese întreagă doar pe hârtie (0,1 + 0,2 + 0,7) tot întreagă se scrie.
  assert.equal(formatKg(0.1 + 0.2 + 0.7), "1");
});

test("un rând vechi cu zecimale în kg le păstrează pe toate trei", () => {
  assert.equal(formatKg(450.5), "450,500");
  assert.equal(formatKg(0.001), "0,001");
});

/**
 * G06: miezul regulii. „35.125" și „35,125" sunt două numere care diferă de o mie de ori, iar
 * până pe 20.09.2026 aceeași cantitate se scria în amândouă felurile, pe ecrane diferite. Cu trei
 * zecimale după orice virgulă și punctul mereu la mii, nu mai e nimic de ghicit — nici după
 * 05.10.2026, când kilogramele întregi și-au pierdut „,000”.
 */
test("aceeași cantitate nu se mai poate citi de o mie de ori mai mare sau mai mică", () => {
  assert.equal(formatKg(35125), "35.125");
  assert.equal(formatKg(35.125), "35,125");
  assert.notEqual(formatKg(35125), formatKg(35.125));
});

/**
 * Continuarea lui G06, pe ecran (20.09.2026). Lista de mişcări tipărea cifra brută din JSON —
 * „35.125", cum o scrie JavaScript — iar deasupra ei banda de totaluri scria „12.640", adică
 * douăsprezece mii, rotunjite dinadins fiindcă e un rezumat. Cele două stăteau una sub alta şi
 * nu se puteau deosebi. Orice cifră de transcris trece acum prin `formatQuantity`, în formatul
 * românesc: punctul e la mii, zecimalele (când sunt) vin după virgulă, câte trei.
 *
 * Controlul negativ: cu `String(value)` în loc de formatare, prima verificare de jos cade cu
 * „35.125".
 */
test("o cantitate de pe un rând se scrie în unitatea ei, în formatul românesc", () => {
  assert.equal(formatQuantity(35.125, "KG"), "35,125");
  assert.equal(formatQuantity(12640, "KG"), "12.640");
  // Tonele rămân tone: rândul spune el în ce unitate e, nu se converteşte pe ascuns.
  assert.equal(formatQuantity(1.06, "TONS"), "1,060");
  assert.notEqual(formatQuantity(35.125, "KG"), String(35.125));
});

/** 29.09.2026: bonul formularului de mișcare scria „35.125 kg”, textul brut din câmp. */
test("cantitatea tastată se scrie pe bon ca pe listă, fără ambiguitate", () => {
  assert.equal(formatQuantityInput("35.125", "KG"), "35,125");
  assert.equal(formatQuantityInput("1060", "KG"), "1.060");
  assert.equal(formatQuantityInput("0.84", "TONS"), "0,840");
  assert.equal(formatQuantityInput("", "KG"), null);
  assert.equal(formatQuantityInput("abc", "KG"), null);
});
