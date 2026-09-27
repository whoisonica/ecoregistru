// Cifrele pe ecran: `npm test` (Node rulează TypeScript direct, fără Jest).
import assert from "node:assert/strict";
import { test } from "node:test";

import { formatDate, formatQuantity } from "./format.ts";

// Telefonul omului e în România; CI-ul rulează în UTC.
process.env.TZ = "Europe/Bucharest";

test("tonele își păstrează kilogramele — până la trei zecimale", () => {
  assert.equal(formatQuantity(0.045, "TONS"), "0,045");
  assert.equal(formatQuantity(1.25, "TONS"), "1,25");
  assert.equal(formatQuantity(2, "TONS"), "2");
  assert.equal(formatQuantity(1250.5, "TONS"), "1.250,5");
});

test("kilogramele rămân cu o zecimală, ca până acum", () => {
  assert.equal(formatQuantity(1240, "KG"), "1.240");
  assert.equal(formatQuantity(12.5, "KG"), "12,5");
});

test("un moment (bifa unui termen) se citește în ora telefonului, nu în UTC", () => {
  // 00:30 pe 13 martie, ora României
  assert.equal(formatDate("2027-03-12T22:30:00Z"), "13.03.2027");
});

test("o zi rămâne ziua ei", () => {
  assert.equal(formatDate("2027-03-15"), "15.03.2027");
});
