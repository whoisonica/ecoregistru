import assert from "node:assert/strict";
import { test } from "node:test";
import { formatKg, formatTonnesValue } from "@/lib/units";

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
