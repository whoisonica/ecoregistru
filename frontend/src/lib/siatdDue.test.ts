import { test } from "node:test";
import assert from "node:assert/strict";
import { formatDue } from "./siatdDue";

/** Termenul SIATD pe rândul din tab (F6a): cuvinte pentru azi și mâine, ziua săptămânii în rest, câte zile au trecut. */
test("azi și mâine", () => {
  assert.equal(formatDue("2026-10-08", "2026-10-08"), "azi");
  assert.equal(formatDue("2026-10-09", "2026-10-08"), "mâine");
});

test("mai departe: ziua cu literă mică și data scurtă", () => {
  assert.equal(formatDue("2026-10-08", "2026-10-02"), "joi 08.10");
  assert.equal(formatDue("2026-10-12", "2026-10-02"), "luni 12.10");
});

test("trecute", () => {
  assert.equal(formatDue("2026-10-06", "2026-10-08"), "trecut de 2 zile");
  assert.equal(formatDue("2026-10-07", "2026-10-08"), "trecut de o zi");
});

test("peste schimbarea orei nu pierde o zi", () => {
  // 25.10.2026 e duminica în care ora de vară se termină
  assert.equal(formatDue("2026-10-27", "2026-10-24"), "marți 27.10");
  assert.equal(formatDue("2026-10-24", "2026-10-26"), "trecut de 2 zile");
});
