import { test } from "node:test";
import assert from "node:assert/strict";
import { wasteCodeValidOn } from "./types";

/**
 * Lista deșeurilor are ediții (V80): Decizia (UE) 2025/934 scoate 20 01 33* după 8.11.2026 și aduce
 * 20 01 42* de la 9.11.2026. Selectorul din formularul de mișcare propune din profil numai codurile
 * valabile la data mișcării — cele două zile de graniță sunt cele care contează.
 */
test("un cod scos se propune până în ultima lui zi, inclusiv", () => {
  const retras = { validFrom: null, validTo: "2026-11-08" };
  assert.equal(wasteCodeValidOn(retras, "2026-11-08"), true);
  assert.equal(wasteCodeValidOn(retras, "2026-11-09"), false);
});

test("un cod nou se propune din prima lui zi, nu înainte", () => {
  const nou = { validFrom: "2026-11-09", validTo: null };
  assert.equal(wasteCodeValidOn(nou, "2026-11-08"), false);
  assert.equal(wasteCodeValidOn(nou, "2026-11-09"), true);
});

test("un cod din lista de bază, fără date, e valabil oricând", () => {
  assert.equal(wasteCodeValidOn({}, "2015-06-01"), true);
  assert.equal(wasteCodeValidOn({ validFrom: null, validTo: null }, "2030-01-01"), true);
});
