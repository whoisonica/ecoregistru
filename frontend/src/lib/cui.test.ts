import { test } from "node:test";
import assert from "node:assert/strict";
import { isValidCui, partnerWithSameCui } from "@/lib/cui";

test("coduri reale trec, cu sau fără RO și spații", () => {
  assert.equal(isValidCui("14399840"), true);
  assert.equal(isValidCui("RO 1590082"), true);
  assert.equal(isValidCui("ro361757"), true);
});

test("o cifră greșită sau forma greșită nu trec", () => {
  assert.equal(isValidCui("14399841"), false);
  assert.equal(isValidCui("RO12345678"), false);
  assert.equal(isValidCui("1"), false);
  assert.equal(isValidCui("not-a-cui"), false);
});

/** 29.09.2026: al doilea partener cu același CUI trecea tăcut; formularul îl numește pe cel existent. */
test("CUI-ul tastat se potrivește cu al unui partener existent, cu sau fără RO, spații sau litere mici", () => {
  const partners = [
    { id: "a", cui: "RO 14399840", name: "Retim" },
    { id: "b", cui: null, name: "Fără CUI" },
  ];
  assert.equal(partnerWithSameCui("14399840", partners, null)?.name, "Retim");
  assert.equal(partnerWithSameCui("ro14399840 ", partners, null)?.name, "Retim");
  // Partenerul editat nu e propriul duplicat; un CUI gol nu se potrivește cu nimic.
  assert.equal(partnerWithSameCui("14399840", partners, "a"), undefined);
  assert.equal(partnerWithSameCui("  ", partners, null), undefined);
  assert.equal(partnerWithSameCui("1590082", partners, null), undefined);
});
