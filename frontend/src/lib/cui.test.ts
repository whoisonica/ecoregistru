import { test } from "node:test";
import assert from "node:assert/strict";
import { isValidCui } from "@/lib/cui";

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
