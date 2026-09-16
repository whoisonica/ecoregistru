import assert from "node:assert/strict";
import { test } from "node:test";
import { BIN_CODE_COUNT, binFor } from "@/lib/binColor";

test("culoarea vine din lista explicită, nu din prefix", () => {
  assert.equal(binFor("15 01 01"), "paper");
  assert.equal(binFor("15 01 02"), "plastic");
  assert.equal(binFor("15 01 07"), "glass");
  assert.equal(binFor(" 20 02 01 "), "bio");
  // Același prefix, cod care nu e în listă: fără culoare, nu ghicit.
  assert.equal(binFor("15 01 10"), null);
  assert.ok(BIN_CODE_COUNT > 0);
});

test("periculosul e roșu înaintea listei, după steluță sau după steagul din catalog", () => {
  assert.equal(binFor("15 01 10*"), "hazard");
  assert.equal(binFor("15 01 01", true), "hazard");
});
