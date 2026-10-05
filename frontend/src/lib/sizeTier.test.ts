import assert from "node:assert/strict";
import { test } from "node:test";
import { parseMonthlyPrice, planLabel, SIZE_TIERS, subscriptionSummary } from "@/lib/sizeTier";

test("cele cinci trepte au intervalele din grilă", () => {
  assert.deepEqual(
    SIZE_TIERS.map((s) => [s.tier, s.range]),
    [[1, "0–2"], [2, "3–9"], [3, "10–19"], [4, "20–39"], [5, "40+"]]
  );
});

test("planLabel: cu treaptă, fără treaptă, la preț personalizat", () => {
  assert.equal(planLabel("GENERATOR", 2, false), "Generator, treapta 2");
  assert.equal(planLabel("GENERATOR", 2, true), "Generator");
  assert.equal(planLabel("GENERATOR", null, false), "Generator");
  assert.equal(planLabel("FULL_SERVICE", null, false), "Serviciu complet");
  assert.equal(planLabel("CONSULTANCY", null, false), "Abonament de consultant");
});

test("subscriptionSummary: pe treaptă, personalizat, vechi, fără preț", () => {
  assert.equal(subscriptionSummary("GENERATOR", 2, false, "50 lei"), "Generator · treapta 2 · 50 lei");
  assert.equal(subscriptionSummary("GENERATOR", null, true, "42 lei"), "Generator · 42 lei · preț personalizat");
  assert.equal(subscriptionSummary("GENERATOR", null, false, "99 lei"), "Generator · 99 lei");
  assert.equal(subscriptionSummary("GENERATOR", 2, false, null), "Generator · treapta 2");
});

test("parseMonthlyPrice: cel mult 8 cifre și 2 zecimale, peste zero", () => {
  assert.equal(parseMonthlyPrice("42"), 42);
  assert.equal(parseMonthlyPrice(" 300.5 "), 300.5);
  assert.equal(parseMonthlyPrice("99999999.99"), 99999999.99);
  for (const bad of ["", "  ", "0", "0.00", "-5", "10.123", "123456789", "1e3", "abc", "12,5", ".5"]) {
    assert.equal(parseMonthlyPrice(bad), null, bad);
  }
});
