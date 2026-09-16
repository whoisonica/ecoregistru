import assert from "node:assert/strict";
import { test } from "node:test";
import { formatTonnes, formatTonnesValue, kgToTonnes } from "@/lib/units";

test("kilogramele devin tone fără rotunjire ascunsă", () => {
  assert.equal(kgToTonnes(450), 0.45);
  assert.equal(kgToTonnes(1), 0.001);
});

test("tonele se arată cu trei zecimale, în formatul românesc", () => {
  assert.equal(formatTonnes(450), "0,450");
  assert.equal(formatTonnesValue(0.84), "0,840");
  // A treia zecimală e kilogramul: 1 kg nu dispare la formatare.
  assert.equal(formatTonnes(1), "0,001");
});
