import assert from "node:assert/strict";
import { test } from "node:test";
import { formatKg, formatTonnesValue } from "@/lib/units";

test("pragul în tone se arată cu trei zecimale, în formatul românesc", () => {
  assert.equal(formatTonnesValue(0.84), "0,840");
  // A treia zecimală e kilogramul: 1 kg nu dispare la formatare.
  assert.equal(formatTonnesValue(0.001), "0,001");
});

test("kilogramele se scriu cu punct la mii și fără zecimale forțate", () => {
  assert.equal(formatKg(1060), "1.060");
  assert.equal(formatKg(450.5), "450,5");
  assert.equal(formatKg(0), "0");
});
